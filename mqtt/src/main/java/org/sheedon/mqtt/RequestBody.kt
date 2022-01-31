package org.sheedon.mqtt

import androidx.annotation.IntRange
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.charset.Charset

/**
 * 请求对象内容体，包含：「请求主题」+「消息内容」+「消息质量」+「是否保留」
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 10:58 上午
 */
class RequestBody @JvmOverloads constructor(
    var topic: String,
    var data: String,
    @IntRange(from = 0, to = 2) qos: Int = 0,
    retained: Boolean = false,
    charset: String? = null,
    var autoEncode: Boolean = true
) : MqttMessage() {

    init {
        if (charset.isNullOrEmpty()) {
            super.setPayload(data.toByteArray())
        } else {
            autoEncode = false
            super.setPayload(data.toByteArray(Charset.forName(charset)))
        }
        super.setQos(qos)
        super.setRetained(retained)
    }

}