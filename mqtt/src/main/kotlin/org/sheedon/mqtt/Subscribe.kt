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

import androidx.annotation.IntRange

/**
 * mqtt subscribes topic content
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:47 下午
 */
class Subscribe(
    @get:JvmName("topic") val topic: String,
    @get:JvmName("qos") val qos: Int = 0,
    @get:JvmName("userContext") val userContext: Any? = null
) {

    fun convertKey(): String {
        return topic + qos
    }

    fun newBuilder(): Builder = Builder(this)

    open class Builder {
        internal var topic: String? = null
        internal var qos: Int
        internal var userContext: Any? = null

        constructor() {
            qos = 0
        }

        internal constructor(subscribe: Subscribe) {
            this.topic = subscribe.topic
            this.qos = subscribe.qos
        }

        /**
         * Sets the topic target of this subscribe.
         *
         * @throws IllegalArgumentException if [topic] is not a valid MQTT topic. Avoid this
         *     exception by calling [MQTT topic.parse]; it returns null for invalid Topic.
         */
        open fun topic(topic: String) = apply {
            this.topic = topic
        }

        /**
         * Sets the qos target of this subscribe.
         */
        open fun qos(@IntRange(from = 0, to = 2) qos: Int) = apply {
            this.qos = qos
        }

        /**
         * Sets the userContext target of this subscribe.
         */
        open fun userContext(userContext: Any?) = apply {
            this.userContext = userContext
        }

        open fun build(): Subscribe {
            return Subscribe(
                checkNotNull(topic) { "topic == null" },
                qos,
                userContext
            )
        }
    }

    companion object {
        fun build(topic: String, qos: Int): Subscribe {
            return Builder().topic(topic)
                .qos(qos)
                .build()
        }
    }
}