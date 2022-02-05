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

/**
 * Implementers of this interface will be notified when the asynchronous operation is complete.
 * Mainly used for "connect", "disconnect", "subscribe", "unsubscribe".
 * After receiving the feedback result, the action field indicates the processing type of the request,
 * and onSuccess/onFailure methods indicate the content of the success or failure
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/29 3:42 下午
 */
interface IActionListener {

    /**
     * The enumeration object of mqtt action listener includes "subscribe", "unsubscribe", "connect", "unconnect".
     */
    enum class ACTION {
        CONNECT,
        DISCONNECT,
        SUBSCRIBE,
        UNSUBSCRIBE
    }

    /**
     * This method is invoked when an action has completed successfully.
     */
    fun onSuccess(action: ACTION)

    /**
     * This method is invoked when an action fails.
     */
    fun onFailure(action: ACTION, exception: Throwable?)
}