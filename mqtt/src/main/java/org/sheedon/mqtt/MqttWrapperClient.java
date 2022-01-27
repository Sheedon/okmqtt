package org.sheedon.mqtt;

import android.content.Context;

import java.util.Queue;

/**
 * Mqtt包装客户端类
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:38 下午
 */
public class MqttWrapperClient {

    Context context;
    String serverURI;
    String clientId;

    String baseTopic;

    DefaultMqttConnectOptions connectOptions;
    DefaultDisconnectedBufferOptions disconnectedOptions;

    int messageTimeout;

    Queue<SubscribeBody> subscribeBodies;
    boolean isAutoSubscribeToTopic;

//    MqttCallbackExtendedListener listener;

    String charsetName;

//    final List<DataConverter.Factory> converterFactories = new ArrayList<>();

}
