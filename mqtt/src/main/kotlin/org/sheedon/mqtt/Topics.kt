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

import androidx.annotation.IntRange

/**
 * mqtt subscribes topic content
 * topic：mqtt topic，
 * qos：mqtt qos
 * userContext：MqttClient userContext
 * headers:Topic binding header information
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:47 下午
 */
class Topics(
    @get:JvmName("topic") val topic: String,
    @get:JvmName("qos") val qos: Int = 0,
    @get:JvmName("userContext") val userContext: Any? = null,
    @get:JvmName("headers") val headers: Headers = Headers()
) {

    fun convertKey(): String {
        return "$topic$$qos"
    }

    fun newBuilder(): Builder = Builder(this)

    open class Builder {
        // 主题
        internal var topic: String? = null

        // 质量
        internal var qos: Int

        // 用户上下文
        internal var userContext: Any? = null

        // 主题所绑定的头部信息，
        // 1. attachRecord:是否附加到缓存记录中，若false，则代表单次订阅，清空行为后，不恢复
        // 2. subscriptionType:REMOTE - mqtt订阅和本地订阅，LOCAL - 单一本地订阅
        internal var headers = Headers.Builder()

        constructor() {
            qos = 0
        }

        internal constructor(topics: Topics) {
            this.topic = topics.topic
            this.qos = topics.qos
            this.userContext = topics.userContext
            this.headers = topics.headers.toBuilder()
        }

        /**
         * Sets the topic target of this topics.
         *
         * @throws IllegalArgumentException if [topic] is not a valid MQTT topic. Avoid this
         *     exception by calling [MQTT topic.parse]; it returns null for invalid Topic.
         */
        open fun topic(topic: String) = apply {
            this.topic = topic
        }

        /**
         * Sets the qos target of this topics.
         */
        open fun qos(@IntRange(from = 0, to = 2) qos: Int) = apply {
            this.qos = qos
        }

        /**
         * Sets the userContext target of this topics.
         */
        open fun userContext(userContext: Any?) = apply {
            this.userContext = userContext
        }

        /**
         * Sets the attachRecord target of this topics.
         *
         * Whether to append to the cache record, if false,
         * it means a single subscription, after clearing the behavior, it will not be restored
         */
        open fun attachRecord(attachRecord: Boolean) = apply {
            this.headers.attachRecord(attachRecord)
        }

        /**
         * Sets the subscriptionType target of this topics.
         *
         * Set the subscription type. If [SubscriptionType.REMOTE] is used, it means mqtt+local is required.
         * If the type is [SubscriptionType.LOCAL], it means a single local request.
         * The default is [SubscriptionType.REMOTE]
         */
        open fun subscriptionType(subscriptionType: SubscriptionType) = apply {
            this.headers.subscriptionType(subscriptionType)
        }

        open fun build(): Topics {
            return Topics(
                checkNotNull(topic) { "topic == null" },
                qos,
                userContext,
                headers.build()
            )
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun build(
            topic: String,
            qos: Int,
            attachRecord: Boolean = false,
            subscriptionType: SubscriptionType = SubscriptionType.REMOTE,
        ): Topics {
            return Builder().topic(topic)
                .qos(qos)
                .attachRecord(attachRecord)
                .subscriptionType(subscriptionType)
                .build()
        }
    }
}