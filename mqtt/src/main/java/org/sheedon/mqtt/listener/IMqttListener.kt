package org.sheedon.mqtt.listener

import org.eclipse.paho.client.mqttv3.IMqttToken

/**
 * mqtt动作监听器
 * 包含订阅和取消订阅的行为
 * 包含连接和取消连接的行为
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/29 4:05 下午
 */
interface IMqttListener {

    enum class ACTION {
        CONNECT,
        DISCONNECT,
        SUBSCRIBE,
        UNSUBSCRIBE
    }

    /**
     * This method is invoked when an action has completed successfully.
     *
     */
    fun onSuccess(action: ACTION, asyncActionToken: IMqttToken?)

    /**
     * This method is invoked when an action fails.
     */
    fun onFailure(action: ACTION, asyncActionToken: IMqttToken?, exception: Throwable?)
}