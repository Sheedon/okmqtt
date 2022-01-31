package org.sheedon.mqtt

import org.sheedon.rr.core.RequestAdapter
import java.nio.charset.Charset

/**
 * mqtt 请求适配器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 2:03 下午
 */
open class MqttRequestAdapter(val baseTopic: String, val charsetName: String) :
    RequestAdapter<RequestBody> {

    private var client: MqttWrapperClient? = null

    /**
     * 绑定mqtt客户端，用于持有该对象以发送消息
     */
    fun bindMqttClient(client: MqttWrapperClient) {
        this.client = client
    }

    override fun checkRequestData(data: RequestBody): RequestBody {
        if (data.autoEncode && charsetName.isNotEmpty()) {
            data.payload = data.data.toByteArray(Charset.forName(charsetName))
        }

        if (baseTopic.isNotEmpty()) {
            data.topic = baseTopic + data.topic
        }
        return data
    }

    override fun publish(data: RequestBody): Boolean {
        if (client == null) return false

        try {
            val token = client!!.publish(data.topic, data)
            token.waitForCompletion(3000)
        } catch (e: Exception) {
            return false
        }
        return true
    }
}