package org.sheedon.mqtt;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Date;
import java.util.UUID;

/**
 * 真实反馈类
 * * 异步执行数据请求
 * * 1. 获取是否需要反馈，有反馈监听才需要添加反馈
 * * 2. 无需反馈，直接发送完成
 * * 3. 需要反馈，添加任务队列Map<name,listener>
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/11 20:16
 */
public class RealCall implements Call {
    protected final RealClient client;

    protected final Request originalRequest;
    // Guarded by this.
    private boolean executed;

    protected RealCall(RealClient client, Request originalRequest) {
        this.client = client;
        this.originalRequest = originalRequest;
    }

    /**
     * 新增反馈类
     */
    static RealCall newRealCall(OkMqttClient client, Request originalRequest) {
        // Safely publish the Call instance to the EventListener.
        //        call.eventListener = client.eventListenerFactory().create(call);
        return new RealCall(client, originalRequest);
    }

    @Override
    public Request request() {
        return originalRequest;
    }

    /**
     * 新增无反馈请求
     */
    @Override
    public void publishNotCallback() {
        enqueue(null);
    }

    /**
     * 新增有消息反馈的数据
     *
     * @param responseCallback 请求反馈
     */
    @Override
    public void enqueue(Callback responseCallback) {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
        client.dispatcher().enqueue(new AsyncCall(client, originalRequest, responseCallback));
    }

    /**
     * 取消
     */
    @Override
    public void cancel() {

    }

    @Override
    public boolean isExecuted() {
        return false;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public Call clone() {
        return null;
    }

    final class AsyncCall extends NamedRunnable implements AsyncCallImpl {

        private final Callback responseCallback;
        private final RealClient client;
        private final Request originalRequest;
        private String id;
        private Date delayDate;

        AsyncCall(RealClient client, Request originalRequest, Callback responseCallback) {
            super("AsyncCall %s", new Object());
            this.client = client;
            this.originalRequest = originalRequest;
            this.responseCallback = responseCallback;
        }

        public RealCall get() {
            return RealCall.this;
        }

        public String backName() {
            return originalRequest.backName();
        }

        public Callback callback() {
            return responseCallback;
        }

        public String id() {
            return id;
        }

        public Date delayDate() {
            return delayDate;
        }

        public Request request() {
            return originalRequest;
        }

        long delayMillis() {
            long delayMillis = originalRequest.delayMilliSecond();
            return delayMillis <= 0 ? client.timeOutMilliSecond() : delayMillis;
        }

        @Override
        protected void execute() {

            // 1. 获取是否需要反馈，有反馈监听才需要添加反馈
            // 2. 无需反馈，直接发送完成
            // 3. 需要反馈，添加任务队列Map<name,listener>
            boolean isNeedCallback = responseCallback != null;
            DelayEvent delayEvent = null;

            if (isNeedCallback) {
                id = UUID.randomUUID().toString();
                delayDate = new Date(System.currentTimeMillis() + delayMillis());
                delayEvent = client.dispatcher().addTaskAndNetCall(this);
            }

            Request request = originalRequest;
            OkMqttClient mqttClient = (OkMqttClient) client;

            try {

                String topic = request.topic();
                if (topic == null || topic.isEmpty())
                    topic = mqttClient.baseTopic();

                if (mqttClient == null || mqttClient.mqttClient() == null) {
                    assert client != null;
                    client.dispatcher().finishedByLocal(id(), new Throwable("mqtt client is null"));
                    return;
                }
                mqttClient.mqttClient().publish(topic,
                        request.getBody());

                if (isNeedCallback) {
                    client.dispatcher().addLocalTimeOutCall(delayEvent);
                }

            } catch (MqttException | NullPointerException e) {
                e.printStackTrace();
                client.dispatcher().finishedByLocal(id(), e);

            } finally {

            }
        }


    }
}
