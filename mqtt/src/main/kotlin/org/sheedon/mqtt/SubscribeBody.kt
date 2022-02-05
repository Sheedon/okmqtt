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

/**
 * mqtt subscribes topic content
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:47 下午
 */
class SubscribeBody {
    var topic: String? = null
    var qos = 0

    fun convertKey(): String {
        return topic + qos
    }

    companion object {
        fun build(topic: String, qos: Int): SubscribeBody {
            val body = SubscribeBody()
            body.topic = topic
            body.qos = qos
            return body
        }
    }
}