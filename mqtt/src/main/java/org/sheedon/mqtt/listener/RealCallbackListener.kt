package org.sheedon.mqtt.listener

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken

/**
 * 反馈动作监听者实现类
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/29 8:39 下午
 */
class RealCallbackListener(
    val callbackListener: IActionListener?,
    val actionListener: IResultActionListener?,
    val action: IMqttListener.ACTION
) : IMqttActionListener {

    override fun onSuccess(asyncActionToken: IMqttToken?) {
        callbackListener?.onSuccess(action)
        actionListener?.onSuccess()
    }

    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        callbackListener?.onFailure(action, exception)
        actionListener?.onFailure(exception)
    }
}