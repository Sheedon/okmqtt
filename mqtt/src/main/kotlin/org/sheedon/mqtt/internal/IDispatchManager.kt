package org.sheedon.mqtt.internal

import org.sheedon.mqtt.Response
import org.sheedon.mqtt.internal.binder.IBindHandler
import org.sheedon.mqtt.internal.binder.IRequestHandler
import org.sheedon.mqtt.internal.concurrent.EventBehavior

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
     * 事件执行者，提供请求和响应的调度方法，将事件放入异步执行
     */
    fun eventBehavior(): EventBehavior

    /**
     * 绑定处理者，处理响应关键字和Callback的关联
     */
    fun bindHandler(): IBindHandler

}