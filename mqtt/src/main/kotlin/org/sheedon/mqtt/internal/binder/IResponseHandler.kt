package org.sheedon.mqtt.internal.binder

import org.eclipse.paho.client.mqttv3.MqttMessage

/**
 * 响应处理执行者
 * 主要包含响应结果的反馈
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/17 11:51 下午
 */
interface IResponseHandler {

    fun callResponse(topic: String, message: MqttMessage)
}