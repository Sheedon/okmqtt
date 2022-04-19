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

import androidx.annotation.IntRange
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.Charset

/**
 * As request content for constructing MQTT-Topic and MqttMessage.
 * An MQTT message holds the application payload and options
 * specifying how the message is to be delivered
 * The message includes a "payload" (the body of the message)
 * represented as a byte[].
 *
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 10:58 上午
 */
open class RequestBody @JvmOverloads constructor(
    @get:JvmName("topic") var topic: String,
    @get:JvmName("data") var data: String? = null,
    @get:JvmName("qos") @IntRange(from = 0, to = 2) val qos: Int = 0,
    @get:JvmName("retained") val retained: Boolean = false,
    @get:JvmName("charset") val charset: String? = null,
    @get:JvmName("autoEncode") var autoEncode: Boolean = true
) : MqttMessage() {

    /**
     * When the current class is initialized, if the charset is empty,
     * the byte array of data is set to the payload, and if autoEncode == true,
     * the globally set charset is set to the payload before the request is sent.
     * Otherwise, set the current charset directly, and set auto Encode to false.
     * Before submitting, No longer reset the charset of the payload by global charset.
     */
    init {
        if (charset.isNullOrEmpty()) {
            super.setPayload(data?.toByteArray() ?: byteArrayOf())
        } else {
            autoEncode = false
            super.setPayload(data?.toByteArray(Charset.forName(charset)) ?: byteArrayOf())
        }
        super.setQos(qos)
        super.setRetained(retained)
    }

    override fun toString(): String = buildString {
        append("RequestBody{topic=")
        append(topic)
        append(",data=")
        append(data)
        append(",qos=")
        append(qos)
        append(",retained=")
        append(isRetained)
        append(",charset=")
        append(charset)
        append(",autoEncode=")
        append(autoEncode)
        append('}')
    }

}