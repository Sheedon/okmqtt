package org.sheedon.mqtt

import org.sheedon.rr.core.IResponse
import org.sheedon.rr.core.ResponseAdapter
import java.nio.charset.Charset

/**
 * mqtt 响应适配器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 2:09 下午
 */
internal class MqttResponseAdapter(val charsetName: String?) :
    ResponseAdapter<String, ResponseBody> {


    @Suppress("UNCHECKED_CAST")
    override fun <Response : IResponse<String, ResponseBody>> buildFailure(
        topic: String,
        message: String
    ): Response {
        return Response(topic, message) as Response
    }

    @Suppress("UNCHECKED_CAST")
    override fun <Response : IResponse<String, ResponseBody>> buildResponse(
        topic: String,
        body: ResponseBody
    ): Response {
        if (charsetName.isNullOrEmpty()) {
            body.data = String(body.payload)
        } else {
            val charset = Charset.forName(charsetName)
            body.data = String(body.payload, charset)
        }
        return Response(topic, body = body) as Response
    }
}