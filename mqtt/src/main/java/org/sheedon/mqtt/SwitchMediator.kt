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
internal class SwitchMediator : DispatchAdapter<RequestBody, ResponseBody> {

    private var listener: DispatchAdapter.OnCallListener<ResponseBody>? = null

    override fun bindCallListener(listener: DispatchAdapter.OnCallListener<ResponseBody>?) {
        this.listener = listener
    }

    /**
     * 将响应结果发送到请求响应模块下执行数据处理
     * @param message 消息体内容
     */
    fun callResponse(message: ResponseBody) {
        this.listener?.callResponse(message)
    }


    override fun loadRequestAdapter(): RequestAdapter<RequestBody> {
        TODO()
    }
}