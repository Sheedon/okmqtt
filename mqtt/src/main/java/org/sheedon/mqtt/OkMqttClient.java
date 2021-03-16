package org.sheedon.mqtt;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

import static org.sheedon.mqtt.Util.checkNotNull;

/**
 * OkMqtt客户端，创建绑定反馈处理
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/4/5 16:55
 */
public class OkMqttClient implements RealClient, MQTTFactory {
    private static final String TAG = "OK_MQTT_CLIENT";

    // 调度器
    private final Dispatcher dispatcher;
    // 超时毫秒
    private final long timeOutMilliSecond;
    // mqttAndroid客户端
    private final MqttAndroidClient mqttClient;

    // 反馈监听器
    private final MqttCallbackExtendedListener listener;

    // 连接选项配置类
    private final MqttConnectOptions connectOptions;
    // 断开连接缓存选项配置类
    private final DisconnectedBufferOptions disconnectedOptions;

    // 主题订阅数据
    private final Queue<SubscribeBody> subscribeBodies;

    // 基础主题
    private final String baseTopic;
    // 编码名称
    private final String charsetName;

    // 是否自动订阅主题
    private final boolean isAutoSubscribeToTopic;

    // 是否开始连接
    private boolean isStartConnect;

    public OkMqttClient() {
        this(new Builder());
    }

    private OkMqttClient(Builder builder) {
        this.dispatcher = builder.dispatcher;
        this.timeOutMilliSecond = builder.messageTimeout * 1000;
        this.charsetName = builder.charsetName;
        this.subscribeBodies = builder.subscribeBodies;
        this.baseTopic = builder.baseTopic;
        this.isAutoSubscribeToTopic = builder.isAutoSubscribeToTopic;
        this.dispatcher.setConverterFactories(Collections.unmodifiableList(builder.converterFactories));

        checkNotNull(builder.context, "context is null");
        checkNotNull(builder.serverURI, "server uri is null");

        String clientId = builder.clientId == null || builder.clientId.equals("") ? UUID.randomUUID().toString() : builder.clientId;
        mqttClient = new MqttAndroidClient(builder.context, builder.serverURI, clientId);
        this.listener = builder.listener;
        mqttClient.setCallback(mCreateCallback);


        this.connectOptions = builder.connectOptions;
        this.disconnectedOptions = builder.disconnectedOptions;

        connect();
    }

    public Builder newBuilder() {
        return new Builder();
    }

    /**
     * mqtt 创建连接
     */
    private void connect() {
        checkNotNull(mqttClient, "please create mqtt client");
        if (!mqttClient.isConnected() && !isStartConnect) {
            try {
                isStartConnect = true;
                mqttClient.connect(connectOptions, null, mCreateCallback);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private long lastTime = 0;

    public void reConnect() {
        long nowTime = System.currentTimeMillis();
        if (nowTime - lastTime < 5000) {
            isStartConnect = false;
            if (listener != null)
                listener.onFailure(null, new Throwable("Only reconnect once within 5 seconds"));
            return;
        }

        lastTime = nowTime;
        try {
            connect();
        } catch (Exception e) {
            if (listener != null)
                listener.onFailure(null, e);
        }
    }

    /**
     * 获取调度器
     */
    public Dispatcher dispatcher() {
        return dispatcher;
    }

    /**
     * 获取超时毫秒数
     */
    public long timeOutMilliSecond() {
        return timeOutMilliSecond;
    }

    /**
     * mqttAndroid客户端
     */
    public MqttAndroidClient mqttClient() {
        return mqttClient;
    }

    /**
     * 基础主题
     */
    String baseTopic() {
        return baseTopic;
    }

    /**
     * mqtt连接监听
     */
    private final MqttCallbackExtendedListener mCreateCallback = new MqttCallbackExtendedListener() {

        /**
         * 连接成功
         * */
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {

            isStartConnect = false;
            mqttClient.setBufferOpts(disconnectedOptions);
            if (listener != null)
                listener.onSuccess(asyncActionToken);
        }

        /**
         * 连接失败
         * */
        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            isStartConnect = false;

            if (listener != null)
                listener.onFailure(asyncActionToken, exception);
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {

            if (isAutoSubscribeToTopic)
                subscribeToTopic(reconnect);


            if (listener != null)
                listener.connectComplete(reconnect, serverURI);
        }

        @Override
        public void connectionLost(Throwable cause) {

            if (listener != null)
                listener.connectionLost(cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            Log.v(TAG, "messageArrived: " + topic + " : " + new String(message.getPayload(), charsetName));
            // 数据转义
            String data = new String(message.getPayload(), charsetName);

            dispatcher.enqueueNetCallback(topic, message, charsetName);
            messageArrived(topic, data);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

            if (listener != null)
                listener.deliveryComplete(token);
        }

        @Override
        public void messageArrived(String topic, String data) {
            if (listener != null)
                listener.messageArrived(topic, data);
        }
    };


    /**
     * 订阅主题
     *
     * @param reconnect 是否重连
     */
    private void subscribeToTopic(boolean reconnect) {
        if (mqttClient == null
                || subscribeBodies == null
                || subscribeBodies.size() == 0)
            return;

        // 配置为自动清除 或者 非清除+第一次连接
        if ((!connectOptions.isCleanSession() && !reconnect)
                || connectOptions.isCleanSession()) {
            String[] topicFilters = new String[subscribeBodies.size()];
            int[] qos = new int[subscribeBodies.size()];
            int index = 0;
            for (SubscribeBody body : subscribeBodies) {
                topicFilters[index] = body.getTopic();
                qos[index] = body.getQos();
                index++;
            }
            try {
                mqttClient.subscribe(topicFilters, qos, null, listener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

    }

    public Queue<SubscribeBody> getSubscribeBodies() {
        return new LinkedBlockingDeque<>(subscribeBodies);
    }

    public void updateSubscribeBodies(Queue<SubscribeBody> queue) {
        if (subscribeBodies != null) {
            subscribeBodies.clear();
            subscribeBodies.addAll(queue);

            subscribeToTopic(true);
        }
    }

    /**
     * 创建Call
     *
     * @param request 请求数据
     * @return Call
     */
    @Override
    public Call newCall(Request request) {
        return RealCall.newRealCall(this, request);
    }

    /**
     * 创建观察者
     *
     * @param request 请求数据
     * @return Observable
     */
    @Override
    public Observable newObservable(Request request) {
        return RealObservable.newRealObservable(this, request);
    }


    public static final class Builder {
        Dispatcher dispatcher;

        Context context;
        String serverURI;
        String clientId;

        String baseTopic;

        MqttConnectOptions connectOptions;
        DisconnectedBufferOptions disconnectedOptions;

        int messageTimeout;

        Queue<SubscribeBody> subscribeBodies;
        boolean isAutoSubscribeToTopic;

        MqttCallbackExtendedListener listener;

        String charsetName;

        final List<DataConverter.Factory> converterFactories = new ArrayList<>();


        public Builder() {
            dispatcher = new Dispatcher();
            messageTimeout = 5;
            isAutoSubscribeToTopic = true;
            connectOptions = MqttConnectOptions.getDefault();
            disconnectedOptions = DisconnectedBufferOptions.getDefault();
            charsetName = "GBK";
            baseTopic = "";
        }

        /**
         * 设置用于设置策略和执行异步请求的调度程序。不能为null。
         */
        public Builder dispatcher(Dispatcher dispatcher) {
            if (dispatcher == null) throw new IllegalArgumentException("dispatcher == null");
            this.dispatcher = dispatcher;
            return this;
        }

        /**
         * 设置mqtt客户端基本信息。Context/serverURI不能为null。
         */
        public Builder clientInfo(Context context, String serverURI, String clientId) {
            if (context == null) throw new IllegalArgumentException("context == null");
            if (serverURI == null || serverURI.equals(""))
                throw new IllegalArgumentException("serverURI == null");
            if (clientId == null) throw new IllegalArgumentException("clientId == null");
            this.context = context;
            this.serverURI = serverURI;
            this.clientId = clientId;
            return this;
        }

        /**
         * 设置mqtt连接选项内容配置
         */
        public Builder mqttOptions(org.eclipse.paho.client.mqttv3.MqttConnectOptions options) {
            return mqttOptions((MqttConnectOptions) options);
        }

        /**
         * 设置mqtt连接选项内容配置
         */
        public Builder mqttOptions(MqttConnectOptions options) {
            if (options == null) throw new IllegalArgumentException("options == null");
            MqttConnectOptions.setDefault(options);
            return this;
        }

        /**
         * 设置mqtt连接选项内容配置
         */
        public Builder disconnectedOptions(org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions options) {
            return disconnectedOptions((DisconnectedBufferOptions) options);
        }

        /**
         * 设置mqtt连接选项内容配置
         */
        public Builder disconnectedOptions(DisconnectedBufferOptions options) {
            if (options == null) throw new IllegalArgumentException("options == null");
            DisconnectedBufferOptions.setDefault(options);
            return this;
        }

        /**
         * 设置用户名和密码
         */
        public Builder userNameAndPassword(String userName, String password) {
            connectOptions.setUserName(userName);
            connectOptions.setPassword(password.toCharArray());
            return this;
        }

        /**
         * 基础主题- 用于请求数据时没有数据时添加
         */
        public Builder baseTopic(String baseTopic) {
            this.baseTopic = baseTopic;
            return this;
        }

        /**
         * 设置will主题
         */
        public Builder will(String topic, String payload) {
            return will(topic, payload, 1, true);
        }

        /**
         * 设置will主题
         */
        public Builder will(String topic, String payload, int qos, boolean retained) {
            connectOptions.setWill(topic, payload.getBytes(), qos, retained);
            return this;
        }

        /**
         * 设置保持在线的判断时间间隔
         */
        public Builder keepAliveInterval(int keepAliveInterval) {
            if (keepAliveInterval < 0)
                return this;

            connectOptions.setKeepAliveInterval(keepAliveInterval);
            return this;
        }

        /**
         * 设置连接超时时间
         */
        public Builder connectionTimeout(int connectionTimeout) {
            if (connectionTimeout < 0)
                return this;

            connectOptions.setConnectionTimeout(connectionTimeout);
            return this;
        }

        /**
         * 设置信息请求超时时间
         */
        public Builder messageTimeout(int messageTimeout) {
            if (messageTimeout < 0)
                return this;

            this.messageTimeout = messageTimeout;
            return this;
        }

        /**
         * 设置最大并发值
         */
        public Builder maxInflight(int maxInflight) {
            if (maxInflight < 0)
                return this;

            connectOptions.setMaxInflight(maxInflight);
            return this;
        }

        /**
         * 设置客户端和服务器是否应该记住重新启动和重新连接之间的状态。
         */
        public Builder cleanSession(boolean cleanSession) {
            connectOptions.setCleanSession(cleanSession);
            return this;
        }

        /**
         * 是否自动订阅主题
         */
        public Builder isAutoSubscribeToTopic(boolean isAutoSubscribeToTopic) {
            this.isAutoSubscribeToTopic = isAutoSubscribeToTopic;
            return this;
        }

        /**
         * 设置如果连接断开，客户端是否将自动尝试重新连接到服务器
         */
        public Builder automaticReconnect(boolean automaticReconnect) {
            connectOptions.setAutomaticReconnect(automaticReconnect);
            return this;
        }

        /**
         * 设置订阅主题内容
         */
        public Builder subscribeBodies(Queue<SubscribeBody> subscribeBodies) {
            this.subscribeBodies = subscribeBodies;
            return this;
        }

        /**
         * 添加订阅主题内容
         */
        public Builder addSubscribeBodies(Queue<SubscribeBody> subscribeBodies) {
            if (this.subscribeBodies == null)
                this.subscribeBodies = new ArrayDeque<>();

            this.subscribeBodies.addAll(subscribeBodies);
            return this;
        }

        /**
         * 添加订阅主题内容
         */
        public Builder addSubscribeBodies(SubscribeBody subscribeBody) {
            if (this.subscribeBodies == null)
                this.subscribeBodies = new ArrayDeque<>();

            this.subscribeBodies.add(subscribeBody);
            return this;
        }

        /**
         * 设置反馈监听器
         */
        public Builder callback(MqttCallbackExtendedListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * 设置字符转义
         */
        public Builder charsetName(String charsetName) {
            this.charsetName = charsetName;
            return this;
        }


        /**
         * 消息处理工具
         */
        public Builder addConverterFactory(DataConverter.Factory factory) {
            converterFactories.add(checkNotNull(factory, "DataConverter.MqttFactory == null"));
            return this;
        }


        public OkMqttClient build() {

            if (converterFactories.size() == 0) {
                throw new IllegalStateException("converterFactories is null.");
            }

            return new OkMqttClient(this);
        }
    }
}
