package org.sheedon.mqtt

import org.eclipse.paho.client.mqttv3.MqttMessage

/**
 * Response message body Contains content,
 * "Subscription Topic" + "Message Content" + "Message Quality" (useless for the client) + "reserve" (useless for the client)
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

            field = String(super.getPayload())
            return field
        }

    constructor(topic: String, mqttMessage: MqttMessage) : this() {
        this.topic = topic
        super.setPayload(mqttMessage.payload)
        super.setQos(mqttMessage.qos)
    }


}