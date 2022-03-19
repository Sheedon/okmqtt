package org.sheedon.mqtt.internal

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.sheedon.mqtt.Subscribe

/**
 * 请求发送执行者
 * 包括请求行为，订阅行为，取消订阅行为
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/17 11:51 下午
 */
interface IRequestProxy {

    /**
     * 发送mqtt消息
     *
     * @param topic 主题
     * @param message mqtt消息内容
     */
    fun publish(
        topic: String,
        message: MqttMessage
    ): IMqttDeliveryToken

    /**
     * 订阅mqtt主题
     * @param body mqtt消息体
     * @param listener 操作监听器
     */
    fun subscribe(
        body: Subscribe,
        listener: IMqttActionListener? = null
    )

    /**
     * 取消订阅mqtt主题
     * @param body mqtt消息体
     * @param listener 操作监听器
     */
    fun unsubscribe(
        body: Subscribe,
        listener: IMqttActionListener? = null
    )
}