package org.sheedon.mqtt.listener

/**
 * 全局动作监听器
 * 用于订阅和取消订阅的监听器
 * 用于连接和取消连接的监听器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/29 3:42 下午
 */
interface IActionListener {

    /**
     * This method is invoked when an action has completed successfully.
     */
    fun onSuccess(action: IMqttListener.ACTION)

    /**
     * This method is invoked when an action fails.
     */
    fun onFailure(action: IMqttListener.ACTION, exception: Throwable?)
}