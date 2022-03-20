package org.sheedon.mqtt.internal.concurrent

import org.sheedon.mqtt.Callback
import org.sheedon.mqtt.Request
import org.sheedon.mqtt.internal.binder.InternalCallback
import org.sheedon.rr.timeout.DelayEvent

/**
 * 事件管理者
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/8 5:03 下午
 */
interface EventManager {
    /**
     * 将反馈主题和反馈监听器添加到事件中，并且返回延迟事件，用于处理超时任务
     *
     * @param request  请求对象
     * @param callback 反馈监听器
     * @return DelayEvent<T>
    </T> */
    fun push(
        request: Request,
        defaultDelayMilliSecond: Long,
        callback: InternalCallback
    ): DelayEvent<String>

    /**
     * 根据反馈关键字获取反馈监听者
     * 关键字，可以为订阅的主题，也可以为响应绑定的关联字段
     *
     * @param keyword 反馈关键字
     * @return Callback
     */
    fun popByKeyword(keyword: String): ReadyTask?

    /**
     * 根据请求ID获取反馈监听者
     *
     * @param id 请求ID
     * @return Callback
     */
    fun popById(id: String): ReadyTask?

    /**
     * 通过主题和反馈监听者，实现监听内容的绑定
     *
     * @param request  请求对象
     * @param callback 反馈监听者
     */
    fun subscribe(
        request: Request,
        callback: InternalCallback
    ): Boolean

    /**
     * 根据反馈关键字取消订阅的绑定
     * 关键字，可以为订阅的主题，也可以为响应绑定的关联字段
     *
     * @param keyword  反馈主题
     */
    fun unsubscribe(keyword: String): Boolean

    /**
     * 通过反馈关键字 加载 反馈监听者
     *
     * @param keyword 反馈关键字
     * @return 反馈监听者
     */
    fun loadObservable(keyword: String): ReadyTask?
}