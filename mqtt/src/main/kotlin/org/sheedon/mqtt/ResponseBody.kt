/*
 * Copyright (C) 2022 Sheedon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sheedon.mqtt

import org.eclipse.paho.client.mqttv3.MqttMessage

/**
 * This message is wrapped with the current class when a message arrives from the server.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 11:13 上午
 */
class ResponseBody private constructor() : MqttMessage() {

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

    /**
     * Constructor to create Response Body through「MQTT Topic」and「MqttMessage」
     */
    constructor(topic: String, mqttMessage: MqttMessage) : this() {
        this.topic = topic
        super.setPayload(mqttMessage.payload)
        super.setQos(mqttMessage.qos)
    }


}