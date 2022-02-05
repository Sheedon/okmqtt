package org.sheedon.mqtt

import org.sheedon.rr.core.IResponse
import org.sheedon.rr.core.ResponseAdapter
import java.nio.charset.Charset

/**
 * mqtt response adapter
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 2:09 下午
 */
internal class MqttResponseAdapter(val charsetName: String?) :
    ResponseAdapter<String, ResponseBody> {


    /**
     * Build failed response objects from topic and message
     *
     * @param topic Feedback Topics
     * @param message error message
     * @return Response Response data
     */
    @Suppress("UNCHECKED_CAST")
    override fun <Response : IResponse<String, ResponseBody>> buildFailure(
        topic: String,
        message: String
    ): Response {
        return Response(topic, message) as Response
    }

    /**
     * Build success response objects from topic and message
     *
     * @param topic Feedback Topics
     * @param body Response data
     * @return Response Response data
     */
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