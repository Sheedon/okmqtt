package org.sheedon.mqtt

import org.sheedon.rr.core.IResponse
import org.sheedon.rr.core.ResponseAdapter

/**
 * mqtt 响应适配器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 2:09 下午
 */
internal class MqttResponseAdapter(charsetName: String?) : ResponseAdapter<String, ResponseBody> {


    override fun <Response : IResponse<String, ResponseBody>> buildFailure(
        topic: String,
        message: String
    ): Response {
        TODO("Not yet implemented")
    }

    override fun <Response : IResponse<String, ResponseBody>?> buildResponse(data: ResponseBody): Response {
        TODO("Not yet implemented")
    }
}