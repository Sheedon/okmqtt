package org.sheedon.mqtt

import org.sheedon.rr.core.DispatchAdapter
import org.sheedon.rr.core.RequestAdapter

/**
 * 数据交换中介者
 * 用于将请求响应模型下请求消息 发送到 mqttClient
 * mqttClient 订阅的消息 反馈到 请求响应的模块下
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 10:27 上午
 */
internal class SwitchMediator internal constructor(
    _baseTopic: String = "",
    _charsetName: String = "UTF-8",
    _requestAdapter: RequestAdapter<RequestBody>? = null
) : DispatchAdapter.AbstractDispatchImpl<RequestBody, ResponseBody>() {

    private val charsetName: String = if (_charsetName.isEmpty()) {
        "UTF-8"
    } else {
        _charsetName
    }
    private val requestAdapter: RequestAdapter<RequestBody> =
        _requestAdapter ?: MqttRequestAdapter(_baseTopic, charsetName)

    override fun loadRequestAdapter(): RequestAdapter<RequestBody> {
        return requestAdapter
    }
}