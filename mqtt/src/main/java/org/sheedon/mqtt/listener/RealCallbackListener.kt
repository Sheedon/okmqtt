package org.sheedon.mqtt.listener

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken

/**
 * The listener implementation class when the asynchronous feedback is completed
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/29 8:39 下午
 */
class RealCallbackListener(
    val callbackListener: IActionListener?,
    val actionListener: IResultActionListener?,
    val action: IActionListener.ACTION
) : IMqttActionListener {

    /**
     * This method is invoked when an action has completed successfully.
     * Dispatch callbackListener and actionListener to execute onSuccess()
     *
     * @param asyncActionToken associated with the action that has completed
     */
    override fun onSuccess(asyncActionToken: IMqttToken?) {
        callbackListener?.onSuccess(action)
        actionListener?.onSuccess()
    }

    /**
     * This method is invoked when an action fails.
     * Dispatch callbackListener and actionListener to execute onFailure()
     *
     * @param asyncActionToken associated with the action that has completed
     * @param exception thrown by the action that has failed
     */
    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        callbackListener?.onFailure(action, exception)
        actionListener?.onFailure(exception)
    }
}