package org.sheedon.mqtt;

import android.content.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 真实的mqtt调度客户端
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/27 10:19 上午
 */
public class MqttWrapperClient {

    // 锁
    private final Object lock = new Object();

    // mqttAndroid客户端
    private final MqttAndroidClient mqttClient;

    // 连接配置选项
    private MqttConnectOptions connectOptions;
    // 主题订阅数据
    private final List<SubscribeBody> subscribeBodies = new ArrayList<>();
    // 订阅情况监听器
    private IMqttActionListener subscribeListener;

    // mqtt 反馈监听器
    // TODO 修改
    private MqttCallback callback;

    // 是否开始连接
    private boolean isStartConnect;
    // 上一次重连时间
    private long lastTime = 0;

    private MqttWrapperClient() {
        this(new Builder());
    }

    private MqttWrapperClient(Builder builder) {
        this.mqttClient = builder.androidClient;
        this.connectOptions = builder.connectOptions;
        this.subscribeBodies.addAll(Arrays.asList(builder.subscribeBodies));
        this.subscribeListener = builder.subscribeListener;
        this.callback = builder.callback;

        // TODO
//        mqttClient.setCallback(mCreateCallback);
        connect();
    }

    /**
     * mqtt 创建连接
     */
    private void connect() {
        if (mqttClient == null) throw new NullPointerException("please create mqtt client");
        synchronized (lock) {
            if (mqttClient.isConnected() || isStartConnect) {
                return;
            }
        }
        try {
            isStartConnect = true;
            mqttClient.connect(connectOptions, null, connectListener);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重连操作
     */
    public void reConnect() {
        long nowTime = System.currentTimeMillis();
        if (nowTime - lastTime < 5000) {
            isStartConnect = false;
//            if (listener != null)
//                listener.onFailure(null, new Throwable("Only reconnect once within 5 seconds"));
            return;
        }

        lastTime = nowTime;
        try {
            connect();
        } catch (Exception e) {
//            if (listener != null)
//                listener.onFailure(null, e);
        }
    }

    private final MqttConnectActionListener connectListener = new MqttConnectActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {

        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {

        }

        @Override
        public void connectionLost(Throwable cause) {

        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    public static final class Builder {
        // mqtt的Android客户端
        private MqttAndroidClient androidClient;
        // 上下文，用于创建MqttAndroidClient
        private Context context;
        // 服务地址
        private String serverURI;
        // 设备ID
        private String clientId;
        // Mqtt 客户端持久化
        private MqttClientPersistence persistence;
        // Ack 反馈处理类型
        private MqttAndroidClient.Ack ackType;

        // 连接配置选项
        private MqttConnectOptions connectOptions;
        // 断开连接缓冲选项
        private DisconnectedBufferOptions bufferOpts;

        // 订阅信息
        private SubscribeBody[] subscribeBodies = new SubscribeBody[0];
        // 订阅情况监听器
        private IMqttActionListener subscribeListener;

        // mqtt 反馈监听器
        // TODO 修改
        private MqttCallback callback;

//        final List<DataConverter.Factory> converterFactories = new ArrayList<>();

        public Builder() {
            connectOptions = DefaultMqttConnectOptions.getDefault();
            bufferOpts = DefaultDisconnectedBufferOptions.getDefault();
        }

        /**
         * Enables an android application to communicate with an MQTT server using non-blocking methods.
         *
         * @param androidClient MqttAndroidClient
         * @return Builder
         */
        public Builder androidClient(MqttAndroidClient androidClient) {
            this.androidClient = Objects.requireNonNull(androidClient, "androidClient == null");
            return this;
        }

        /**
         * Constructor - create an MqttAndroidClient that can be used to communicate with an MQTT server on android
         *
         * @param context   object used to pass context to the callback.
         * @param serverURI specifies the protocol, host name and port to be used to
         *                  connect to an MQTT server
         * @param clientId  specifies the name by which this connection should be
         *                  identified to the server
         * @return Builder
         */
        public Builder clientInfo(Context context, String serverURI, String clientId) {
            this.context = Objects.requireNonNull(context, "context == null");
            this.serverURI = Objects.requireNonNull(serverURI, "serverURI == null");
            this.clientId = clientId == null ? UUID.randomUUID().toString() : clientId;
            return this;
        }

        /**
         * Constructor- create an MqttAndroidClient that can be used to communicate
         * with an MQTT server on android
         *
         * @param context     used to pass context to the callback.
         * @param serverURI   specifies the protocol, host name and port to be used to
         *                    connect to an MQTT server
         * @param clientId    specifies the name by which this connection should be
         *                    identified to the server
         * @param persistence the persistence class to use to store in-flight message. If
         *                    null then the default persistence mechanism is used
         * @param ackType     how the application wishes to acknowledge a message has been
         *                    processed.
         * @return Builder
         */
        public Builder clientInfo(Context context, String serverURI, String clientId,
                                  MqttClientPersistence persistence,
                                  MqttAndroidClient.Ack ackType) {
            clientInfo(context, serverURI, clientId);
            this.persistence = persistence;
            this.ackType = ackType == null ? MqttAndroidClient.Ack.AUTO_ACK : ackType;
            return this;
        }

        /**
         * set of connection parameters that override the defaults
         *
         * @param options  connection parameters
         * @param <Option> MqttConnectOptions
         * @return Builder
         */
        public <Option extends MqttConnectOptions> Builder connectOptions(Option options) {
            this.connectOptions = Objects.requireNonNull(options, "options == null");
            return this;
        }

        /**
         * Sets the DisconnectedBufferOptions for this client
         *
         * @param bufferOpts the DisconnectedBufferOptions
         * @param <Option>   DisconnectedBufferOptions
         * @return Builder
         */
        public <Option extends DisconnectedBufferOptions> Builder disconnectedOptions(Option bufferOpts) {
            this.bufferOpts = Objects.requireNonNull(bufferOpts, "options == null");
            return this;
        }

        /**
         * Sets the default subscribe info for this client
         *
         * @param subscribeBodies Subscribe topic and qos
         * @return Builder
         */
        public Builder subscribeBodies(SubscribeBody... subscribeBodies) {
            this.subscribeBodies = Objects.requireNonNull(subscribeBodies,
                    "subscribeBodies == null");
            return this;
        }

        /**
         * Sets the default subscribe info for this client
         *
         * @param subscribeListener IMqttActionListener
         * @param subscribeBodies   Subscribe topic and qos
         * @return Builder
         */
        public Builder subscribeBodies(IMqttActionListener subscribeListener,
                                       SubscribeBody... subscribeBodies) {
            this.subscribeListener = subscribeListener;
            this.subscribeBodies = Objects.requireNonNull(subscribeBodies,
                    "subscribeBodies == null");
            return this;
        }

        /**
         * Build MqttWrapperClient by MqttWrapperClient.Builder
         *
         * @return MqttWrapperClient
         */
        public MqttWrapperClient build() {

            // If androidClient is not null,
            // it means that the developer wants to use the MqttAndroidClient created by himself
            if (androidClient != null) {
                return new MqttWrapperClient(this);
            }

            // Otherwise, create it according to the configuration content
            Objects.requireNonNull(context, "The current context is empty," +
                    " please set the context");
            Objects.requireNonNull(serverURI, "The current serverURI is empty," +
                    " please set the serverURI");
            clientId = clientId == null || clientId.isEmpty() ? UUID.randomUUID().toString() : clientId;
            ackType = ackType == null ? MqttAndroidClient.Ack.AUTO_ACK : ackType;
            androidClient = new MqttAndroidClient(context, serverURI, clientId, persistence, ackType);


            return new MqttWrapperClient(this);
        }
    }

}
