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

import org.sheedon.rr.core.Call

/**
 * A call is a request that is ready to execute.
 * Since the object represents a single request-response pair (stream), it cannot be executed twice.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 7:43 下午
 */
interface Call : Call<String, RequestBody, ResponseBody> {

    /**
     * Use the Callback in the protocol, you can also use the default enqueue (RRCallback callback),
     * but too many generics are displayed
     *
     * @param callback Callback with request
     */
    fun enqueue(callback: Callback?)

    fun interface Factory {
        fun newCall(request: Request): org.sheedon.mqtt.Call
    }
}