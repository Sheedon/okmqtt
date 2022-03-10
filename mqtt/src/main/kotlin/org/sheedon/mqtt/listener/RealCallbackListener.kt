/*
 * Copyright (C) 2022 Sheedon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    val actionListener: IMqttActionListener?,
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
        actionListener?.onSuccess(asyncActionToken)
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
        actionListener?.onFailure(asyncActionToken, exception)
    }
}