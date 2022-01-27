package org.sheedon.mqtt;

import android.content.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
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

    // 连接选项配置类
    private final MqttConnectOptions connectOptions;
    // 断开连接缓存选项配置类
    private final DisconnectedBufferOptions disconnectedOptions;

    // 主题订阅数据
    private final Queue<SubscribeBody> subscribeBodies;

    // 是否自动订阅主题
    private final boolean isAutoSubscribeToTopic;

    // 是否开始连接
    private boolean isStartConnect;
    // 上一次重连时间
    private long lastTime = 0;

    private MqttWrapperClient() {
        this(new Builder());
    }

    private MqttWrapperClient(Builder builder) {
        this.connectOptions = builder.connectOptions;
        this.disconnectedOptions = builder.disconnectedOptions;
        this.subscribeBodies = builder.subscribeBodies;
        this.isAutoSubscribeToTopic = builder.isAutoSubscribeToTopic;

        Context context = Objects.requireNonNull(builder.context, "The current context is empty," +
                " please set the context");
        String serverUri = Objects.requireNonNull(builder.serverURI, "The current serverURI is empty," +
                " please set the serverURI");
        String clientId = builder.clientId == null || builder.clientId.isEmpty() ? UUID.randomUUID().toString() : builder.clientId;
        mqttClient = new MqttAndroidClient(context, serverUri, clientId);

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
        Context context;
        String serverURI;
        String clientId;

        MqttConnectOptions connectOptions;
        DisconnectedBufferOptions disconnectedOptions;

        Queue<SubscribeBody> subscribeBodies;
        boolean isAutoSubscribeToTopic;

//        final List<DataConverter.Factory> converterFactories = new ArrayList<>();

        public Builder() {
            isAutoSubscribeToTopic = true;
            connectOptions = DefaultMqttConnectOptions.getDefault();
            disconnectedOptions = DefaultDisconnectedBufferOptions.getDefault();
        }

        /**
         * 设置mqtt客户端基本信息。Context/serverURI/clientId不能为null。
         */
        public Builder clientInfo(Context context, String serverURI, String clientId) {
            this.context = Objects.requireNonNull(context, "context == null");
            this.serverURI = Objects.requireNonNull(serverURI, "serverURI == null");
            this.clientId = clientId == null ? UUID.randomUUID().toString() : clientId;
            return this;
        }

        /**
         * 设置mqtt连接选项内容配置
         */
        public <Option extends MqttConnectOptions> Builder mqttOptions(Option options) {
            this.connectOptions = Objects.requireNonNull(options, "options == null");
            return this;
        }

        /**
         * 设置mqtt连接选项内容配置
         */
        public <Option extends DisconnectedBufferOptions> Builder disconnectedOptions(Option options) {
            this.disconnectedOptions = Objects.requireNonNull(options, "options == null");
            return this;
        }

        /**
         * 设置用户名和密码
         */
        public Builder userNameAndPassword(String userName, String password) {
            return userNameAndPassword(userName, password.toCharArray());
        }

        /**
         * 设置用户名和密码
         */
        public Builder userNameAndPassword(String userName, char[] password) {
            connectOptions.setUserName(userName);
            connectOptions.setPassword(password);
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
         * 消息处理工具
         */
//        public Builder addConverterFactory(DataConverter.Factory factory) {
//            converterFactories.add(checkNotNull(factory, "DataConverter.MqttFactory == null"));
//            return this;
//        }
        public MqttWrapperClient build() {

//            if (converterFactories.size() == 0) {
//                throw new IllegalStateException("converterFactories is null.");
//            }

            return new MqttWrapperClient(this);
        }
    }

}
