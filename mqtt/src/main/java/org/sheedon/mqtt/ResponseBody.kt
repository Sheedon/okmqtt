package org.sheedon.mqtt

import org.eclipse.paho.client.mqttv3.MqttMessage

/**
 * 反馈响应消息体
 * 包含内容，「订阅主题」+「消息内容」+「消息质量」（客户端无用）+「是否保留」（客户端无用）
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 11:13 上午
 */
class ResponseBody internal constructor() : MqttMessage() {

    var topic: String? = null
        private set
    var data: String? = null
        get() {
            if (field != null) {
                return field
            }

            field = super.getPayload().toString()
            return field
        }

    constructor(topic: String, mqttMessage: MqttMessage) : this() {
        this.topic = topic
        super.setPayload(mqttMessage.payload)
        super.setQos(mqttMessage.qos)
    }


}