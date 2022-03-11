package org.sheedon.mqtt

import org.sheedon.mqtt.listener.Callback

/**
 * 订阅
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/9 10:38 下午
 */
interface Observable {

    /**
     * 订阅，指代于需要订阅的任务
     *
     * @param callback 反馈内容
     */
    fun subscribe(callback: Callback?)

    /**
     * 取消订阅，指代于需要取消订阅的任务
     *
     * @param callback 反馈内容
     */
    fun unsubscribe(callback: Callback?)


    /**
     * 获取订阅数据
     *
     * @return Subscribe 订阅信息
     */
    fun subscribes(): List<Subscribe>

    /**
     * 消息是否取消
     */
    fun isCanceled(): Boolean

    /**
     * 取消任务
     */
    fun cancel()
}