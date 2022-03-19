package org.sheedon.mqtt.internal

import org.sheedon.mqtt.Response
import org.sheedon.mqtt.ResponseBody

/**
 * 响应结果转换器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/16 3:34 下午
 */
interface IResponseConverter {

    fun buildFailure(
        keyword: String,
        message: String
    ): Response

    fun buildResponse(
        keyword: String,
        data: ResponseBody
    ): Response
}