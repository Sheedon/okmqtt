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
class RealObserver(
    val realObserver: RealObservable<String, String, RequestBody, ResponseBody>
) : Observable {

    companion object {

        @JvmStatic
        fun newObservable(client: MqttRRBinderClient, request: Request): Observable {
            val realObservable = RealObservable(client, request)
            return RealObserver(realObservable)
        }
    }

    override fun <RRCallback : Callback<IRequest<String, RequestBody>,
            IResponse<String, ResponseBody>>> subscribe(
        callback: RRCallback
    ) {
        realObserver.subscribe(callback)
    }

    @Suppress("UNCHECKED_CAST")
    override fun subscribe(callback: org.sheedon.mqtt.Callback?) {
        this.subscribe(
            callback as Callback<IRequest<String, RequestBody>,
                    IResponse<String, ResponseBody>>
        )
    }

    override fun cancel() {
        realObserver.cancel()
    }

    override fun isCanceled(): Boolean {
        return realObserver.isCanceled()
    }
}