package org.sheedon.mqtt.internal

import org.sheedon.mqtt.Response
import org.sheedon.mqtt.internal.binder.IBindHandler
import org.sheedon.mqtt.internal.binder.IRequestHandler
import org.sheedon.mqtt.internal.binder.IResponseConverter

/**
 * 作为「请求响应模式」下，请求和反馈信息的输入。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/20 3:03 下午
 */
interface IDispatchManager {

    /**
     * 请求处理者
     */
    fun requestHandler(): IRequestHandler

    /**
     * 响应结果转换器
     */
    fun responseConverter(): IResponseConverter

    /**
     * 将请求行为入队，按预定策略去执行请求动作
     *
     * @param runnable 处理事件
     */
    fun enqueueRequest(runnable: Runnable)

    /**
     * 绑定处理者，处理响应关键字和Callback的关联
     */
    fun bindHandler(): IBindHandler


    /**
     * 反馈结果监听
     *
     * @param response 反馈结果
     */
    fun onResponse(response: Response)

}