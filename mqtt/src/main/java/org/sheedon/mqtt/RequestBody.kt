package org.sheedon.mqtt

import androidx.annotation.IntRange
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.UnsupportedEncodingException

/**
 * 请求对象内容体，包含：「请求主题」+「消息内容」+「消息质量」+「是否保留」
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 10:58 上午
 */
class RequestBody @JvmOverloads constructor(
    val topic: String,
    var data: String,
    @IntRange(from = 0, to = 2) qos: Int = 0,
    retained: Boolean = false,
) : MqttMessage() {

    init {
        super.setPayload(data.toByteArray())
        super.setQos(qos)
        super.setRetained(retained)
    }

    /**
     * 更换字符类型
     *
     * @param data mqtt 请求数据
     * @param charset 数据类型
     * */
    @JvmOverloads
    fun updateData(data: String, charset: String? = null): RequestBody {
        if (charset.isNullOrEmpty()) {
            this.data = data
            payload = data.toByteArray()
            return this
        }

        try {
            this.data = String(data.toByteArray(), charset(charset))
            payload = data.toByteArray()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        return updateData(this.data)
    }

}