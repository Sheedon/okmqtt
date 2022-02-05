package org.sheedon.mqtt.listener

import org.eclipse.paho.client.mqttv3.MqttMessage
import java.lang.Exception

/**
 * Message listener, used to globally monitor messages subscribed to by mqtt
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/29 3:27 下午
 */
interface IMessageListener {

    @Throws(Exception::class)
    fun messageArrived(topic: String?, message: MqttMessage?)

}