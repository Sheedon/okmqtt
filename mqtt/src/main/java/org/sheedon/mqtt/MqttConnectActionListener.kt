package org.sheedon.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;

/**
 * mqtt连接动作的监听器
 * 包括连接成功与否 和 重连
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/27 11:38 上午
 */
public interface MqttConnectActionListener extends MqttCallbackExtended, IMqttActionListener {
}
