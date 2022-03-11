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

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.sheedon.rr.core.IRequest
import org.sheedon.rr.core.IResponse
import org.sheedon.rr.dispatcher.RealObservable
import org.sheedon.rr.core.Callback

/**
 * Real Observer wrapper class for dispatching locally constructed subscribes
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 8:31 下午
 */
class RealListener(
    val realObserver: RealObservable<String, String, RequestBody, ResponseBody>,
    val okMqttClient: OkMqttClient? = null
) : Listener {

    companion object {

        @JvmStatic
        fun newObservable(client: MqttRRBinderClient, request: Request): Listener {
            val realObservable = RealObservable(client, request)
            return RealListener(realObservable)
        }

        @JvmStatic
        fun newObservable(
            client: OkMqttClient,
            binderClient: MqttRRBinderClient,
            request: Request
        ): Listener {
            val realObservable = RealObservable(binderClient, request)
            return RealListener(realObservable, client)
        }
    }

    override fun <RRCallback : Callback<IRequest<String, RequestBody>,
            IResponse<String, ResponseBody>>> subscribe(
        callback: RRCallback
    ) {

        val request = realObserver.request<Request>()
        val subscribeArray = request.relation.subscribeArray
        // 优先订阅主题，订阅成功后再执行监听操作
        if (subscribeArray.isNotEmpty()) {
            okMqttClient?.subscribe(
                subscribeArray.toList(),
                SubscribeListener(callback)
            )
        } else {
            // 未配置订阅内容，则直接监听
            realObserver.subscribe(callback)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun listen(callback: org.sheedon.mqtt.Callback) {
        this.subscribe(
            callback as Callback<IRequest<String, RequestBody>,
                    IResponse<String, ResponseBody>>
        )
    }

    override fun <Request : IRequest<String, RequestBody>> request(): Request {
        return realObserver.request()
    }

    override fun cancel() {
        val request = realObserver.request<Request>()
        val subscribeArray = request.relation.subscribeArray
        // 优先订阅主题，订阅成功后再执行监听操作
        if (subscribeArray.isNotEmpty()) {
            okMqttClient?.unsubscribe(
                subscribeArray.toList(),
                SubscribeListener(null)
            )
        }
        realObserver.cancel()
    }

    override fun isCanceled(): Boolean {
        return realObserver.isCanceled()
    }

    inner class SubscribeListener(
        val callback: Callback<IRequest<String, RequestBody>,
                IResponse<String, ResponseBody>>?
    ) : IMqttActionListener {

        override fun onSuccess(asyncActionToken: IMqttToken?) {
            callback?.let { realObserver.subscribe(it) }
        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            callback?.onFailure(exception ?: Throwable("mqtt subscribe failure"))
        }

    }
}