package org.sheedon.mqtt.listener

import org.eclipse.paho.client.mqttv3.MqttMessage
import java.lang.Exception

/**
 * 全局消息监听器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/29 3:27 下午
 */
interface IMessageListener {

    @Throws(Exception::class)
    fun messageArrived(topic: String?, message: MqttMessage?)

}