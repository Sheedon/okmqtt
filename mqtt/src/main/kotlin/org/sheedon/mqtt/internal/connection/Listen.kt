package org.sheedon.mqtt.internal.connection

/**
 * Listening behavior, inherited only by [org.sheedon.mqtt.Call] and [org.sheedon.mqtt.Subscribe],
 * as the objects referenced internally by the two
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/3 2:44 下午
 */
interface Listen {


    /**
     * Whether the message is canceled
     */
    fun isCanceled(): Boolean

    /**
     * cancel task
     */
    fun cancel()


    /**
     * whether to be executed
     */
    fun isExecuted(): Boolean
}