package org.sheedon.mqtt.internal.binder

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.sheedon.mqtt.RequestBody
import org.sheedon.mqtt.Topics

/**
 * 请求发送执行者
 * 包括请求行为，订阅行为，取消订阅行为
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/17 11:51 下午
 */
interface IRequestHandler {


    /**
     * 核实请求数据，并且将处理后的请求数据返回
     *
     * @param data 请求数据
     * @return 核实组合后的请求数据
     */
    fun checkRequestData(data: RequestBody): RequestBody

    
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
        vararg body: Topics,
        listener: IMqttActionListener? = null
    )

    /**
     * 取消订阅mqtt主题
     * @param body mqtt消息体
     * @param listener 操作监听器
     */
    fun unsubscribe(
        vararg body: Topics,
        listener: IMqttActionListener? = null
    )
}