/*
 * Copyright (C) 2020 Sheedon.
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

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.sheedon.mqtt.listener.Callback

/**
 * Real Observer wrapper class for dispatching locally constructed subscribes
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 8:31 下午
 */
class RealObserver(
    private val client: MqttWrapperClient,
    private val originalSubscribe: List<Subscribe>
) : Observable {

    // 执行是否为订阅
    private var isSubscribe = false

    // 是否执行取消操作
    @Volatile
    private var canceled = false

    override fun isCanceled() = canceled

    companion object {

        @JvmStatic
        fun newObservable(client: MqttWrapperClient, subscribe: Subscribe): Observable {
            return RealObserver(client, listOf(subscribe))
        }

        @JvmStatic
        fun newObservable(client: MqttWrapperClient, subscribe: List<Subscribe>): Observable {
            return RealObserver(client, subscribe)
        }
    }

    override fun subscribe(callback: Callback?) {
        isSubscribe = true
        client.subscribe(originalSubscribe, loadListener(callback))
    }

    override fun unsubscribe(callback: Callback?) {
        isSubscribe = false
        client.subscribe(originalSubscribe, loadListener(callback))
    }

    /**
     * 加载监听器
     * @param callback 反馈监听器
     * @return IMqttActionListener mqtt动作监听器
     */
    private fun loadListener(callback: Callback?): IMqttActionListener? {
        if (callback == null) {
            return null
        }
        return object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                callback.onResponse(originalSubscribe, asyncActionToken?.response)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                callback.onFailure(exception)
            }
        }

    }

    override fun subscribes(): List<Subscribe> {
        return originalSubscribe
    }

    /**
     * 取消动作
     * 如果是订阅操作，则取消订阅
     * 但是取消订阅，不操作
     */
    override fun cancel() {
        originalSubscribe.takeIf {
            isSubscribe
        }?.also {
            client.subscribe(originalSubscribe)
        }
    }
}