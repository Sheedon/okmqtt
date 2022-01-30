package org.sheedon.mqtt.listener

/**
 * connect/disconnect/subscribe/unsubscribe 's action listener
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/28 8:14 下午
 */
interface IResultActionListener {
    /**
     * This method is invoked when an action has completed successfully.
     */
    fun onSuccess()

    /**
     * This method is invoked when an action fails.
     *
     * @param exception thrown by the action that has failed
     */
    fun onFailure(exception: Throwable?)
}