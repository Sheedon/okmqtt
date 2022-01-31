package org.sheedon.mqtt

import org.sheedon.rr.dispatcher.model.BaseResponse

/**
 * 基础反馈类，需要包含的内容包括「反馈主题」和「反馈消息体」
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 11:01 上午
 */
class Response(
    backTopic: String? = "",
    message: String? = "",
    body: ResponseBody? = null
) : BaseResponse<String, ResponseBody>(backTopic, message, body) {

    override fun backTopic(): String {
        return super.backTopic() ?: ""
    }

    override fun message(): String {
        return super.message() ?: ""
    }

}