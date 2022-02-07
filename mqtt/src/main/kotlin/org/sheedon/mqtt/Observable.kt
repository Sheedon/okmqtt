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

import org.sheedon.rr.core.Observable

/**
 * Observations are messages for which subscriptions are specified. The message can be cancelled.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 8:12 下午
 */
interface Observable : Observable<String, RequestBody, ResponseBody> {

    /**
     * Use the Callback in the protocol, you can also use the default subscribe (RRCallback callback),
     * but the generic type is too much
     *
     * @param callback Callback
     */
    fun subscribe(callback: Callback?)

    fun interface Factory {
        fun newObservable(request: Request): org.sheedon.mqtt.Observable
    }
}