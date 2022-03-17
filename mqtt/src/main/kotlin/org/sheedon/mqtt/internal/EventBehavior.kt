package org.sheedon.mqtt.internal

/**
 * 事件处理行为
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/8 4:57 下午
 */
interface EventBehavior {
    /**
     * 将请求事件提交到队列
     *
     * @param requestRunnable 请求
     * @return 是否已经被处理
     */
    fun enqueueRequestEvent(requestRunnable: Runnable): Boolean

    /**
     * 将反馈事件提交队列
     *
     * @param callbackRunnable Runnable
     * @return 是否已经被处理
     */
    fun enqueueCallbackEvent(callbackRunnable: Runnable): Boolean
}