package org.sheedon.mqtt

import org.sheedon.mqtt.utils.Logger
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
    RequestAdapter.AbstractRequestImpl<RequestBody>() {

    override fun checkRequestData(data: RequestBody): RequestBody {
        if (data.autoEncode && charsetName.isNotEmpty()) {
            data.payload = data.data.toByteArray(Charset.forName(charsetName))
        }

        if (baseTopic.isNotEmpty()) {
            data.topic = baseTopic + data.topic
        }
        Logger.info("checkRequestData (data is $data)")
        return data
    }

    override fun publish(data: RequestBody): Boolean {
        if (sender == null) return false

        try {
            val token = (sender!! as MqttWrapperClient).publish(data.topic, data)
            token.waitForCompletion(3000)
        } catch (e: Exception) {
            Logger.error("publish mqtt message fail", e)
            return false
        }
        return true
    }
}