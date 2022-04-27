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
package org.sheedon.mqtt

import org.sheedon.mqtt.internal.connection.Listen

/**
 * Observations are messages for which subscriptions are specified. The message can be cancelled.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 8:12 下午
 */
interface Observable : Listen {

    /**
     * Use the Callback in the protocol
     *
     * @param callback Callback
     */
    fun enqueue(callback: ObservableBack)

    /**
     * Use the SubscribeCallback in the protocol
     *
     * @param callback SubscribeCallback
     */
    fun enqueue(callback: SubscribeBack?)

    /**
     * Use the FullCallback in the protocol
     *
     * @param callback SubscribeCallback
     */
    fun enqueue(callback: FullCallback)

    /**
     * Unsubscribe from a topic, not necessarily after subscribe
     *
     * @param callback SubscribeCallback
     */
    fun unsubscribe(callback: SubscribeBack?)


    /**
     * get request information
     *
     * @return Request information
     */
    fun request(): Request

    /**
     * get subscription information
     *
     * @return Subscribe information
     */
    fun subscribe(): Subscribe
}