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

import org.sheedon.rr.core.DispatchAdapter
import org.sheedon.rr.core.RequestAdapter

/**
 * The data exchange intermediary is used to send the request message under the request-response model to the mqttClient,
 * and the mqttClient "subscribed message" is fed back to the "request-response module"
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 10:27 上午
 */
internal class SwitchMediator internal constructor(
    _charsetName: String = "UTF-8",
    _requestAdapter: RequestAdapter<RequestBody>? = null
) : DispatchAdapter.AbstractDispatchImpl<RequestBody, ResponseBody>() {

    private val charsetName: String = _charsetName.ifEmpty {
        "UTF-8"
    }
    private val requestAdapter: RequestAdapter<RequestBody> =
        _requestAdapter ?: MqttRequestAdapter(charsetName)

    override fun loadRequestAdapter(): RequestAdapter<RequestBody> {
        return requestAdapter
    }
}