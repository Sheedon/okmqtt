package org.sheedon.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * mqtt信息反馈接口
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/14 10:52
 */
public interface MqttCallbackExtendedListener extends MqttCallbackExtended, IMqttActionListener {

    /**
     * 连接完成
     *
     * @param reconnect 是否为重新连接
     * @param serverURI 服务连接
     */
    @Override
    void connectComplete(boolean reconnect, String serverURI);

    /**
     * 连接断开
     *
     * @param cause 错误
     */
    @Override
    void connectionLost(Throwable cause);


    // 使用下面那个subscribe的messageArrived
    @Override
    void messageArrived(String topic, MqttMessage message) throws Exception;


    @Override
    void deliveryComplete(IMqttDeliveryToken token);

    // 消息反馈
    void messageArrived(String topic, String data);
}
