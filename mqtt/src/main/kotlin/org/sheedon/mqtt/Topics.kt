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
 * As part of an MQTT request or subscription data source,
 * it is intended to configure the subscription topic associated as a feedback message.
 *
 * If the behavior of requesting or subscribing needs to be associated with the binding of
 * a specific callback topic, and the topic has not yet been subscribed, the subscription will be
 * implemented according to the [topic], [qos], [userContext] of the current class configuration.
 * And the scope and storage of the current [Topics] are determined according to the configuration
 * of [headers], see [Headers] for details.
 *
 * Use `new Topics.Builder()` to create a shared instance with custom settings:
 *
 * ```java
 * // The Topics object.
 * Topics topics = new Topics.Builder()
 *     .addInterceptor(new HttpLoggingInterceptor())
 *     .topic("AA/BB/CC",0)
 *     .headers(false,SubscriptionType.REMOTE)
 *     .build();
 * ```
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:47 下午
 */
open class Topics @JvmOverloads constructor(
    @get:JvmName("topic") val topic: String,
    @get:JvmName("qos") val qos: Int = 0,
    @get:JvmName("userContext") val userContext: Any? = null,
    @get:JvmName("headers") val headers: Headers = Headers()
) {


    fun newBuilder(): Builder = Builder(this)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Topics

        if (topic != other.topic) return false
        if (qos != other.qos) return false
        if (userContext != other.userContext) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topic.hashCode()
        result = 31 * result + qos
        result = 31 * result + (userContext?.hashCode() ?: 0)
        result = 31 * result + headers.hashCode()
        return result
    }


    open class Builder {
        internal var topic: String? = null
        internal var qos: Int
        internal var userContext: Any? = null
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
         * Sets the topic target of this topics, including topic, qos, userContext.
         *
         * @param topic
         *            the topic to subscribe to, which can include wildcards.
         * @param qos
         *            the maximum quality of service at which to subscribe.
         *            Default value is 0.
         * @param userContext
         *            optional object used to pass context to the callback. Use null
         *            if not required. Default value is null.
         */
        @JvmOverloads
        open fun topic(
            topic: String,
            @IntRange(from = 0, to = 2) qos: Int = 0,
            userContext: Any? = null
        ) = apply {
            this.topic = topic
            this.qos(qos)
        }

        /**
         * Sets the qos target of this topics.
         *
         * @param qos
         *            the maximum quality of service at which to subscribe.
         */
        open fun qos(@IntRange(from = 0, to = 2) qos: Int) = apply {
            this.qos = qos
        }

        /**
         * Sets the userContext target of this topics.
         *
         * @param userContext
         *            optional object used to pass context to the callback. Use null
         *            if not required. Default value is null.
         */
        open fun userContext(userContext: Any?) = apply {
            this.userContext = userContext
        }

        /**
         * Sets the scope and storage of the current [Topics] are determined according to the configuration
         * of [headers].
         *
         * Whether to append to the cache record, if false,it means a single subscription,
         * after clearing the behavior, it will not be restored.
         * Set the subscription type. If [SubscriptionType.REMOTE] is used, it means mqtt+local is required.
         * If the type is [SubscriptionType.LOCAL], it means a single local request.
         * The default is [SubscriptionType.REMOTE]
         *
         * @param attachRecord
         *            Whether to append to the cache record.
         * @param subscriptionType
         *            Sets the scope of the current [Topics].
         */
        @JvmOverloads
        open fun headers(
            attachRecord: Boolean = false,
            subscriptionType: SubscriptionType = SubscriptionType.REMOTE
        ) = apply {
            this.attachRecord(attachRecord)
            this.subscriptionType(subscriptionType)
        }

        /**
         * Sets the attachRecord target of this topics.
         *
         * Whether to append to the cache record, if false,
         * it means a single subscription, after clearing the behavior, it will not be restored.
         *
         * @param attachRecord
         *            Whether to append to the cache record.
         */
        open fun attachRecord(attachRecord: Boolean) = apply {
            this.headers.attachRecord(attachRecord)
        }

        /**
         * Sets the subscriptionType target of this topics.
         *
         * Set the subscription type. If [SubscriptionType.REMOTE] is used, it means mqtt+local is required.
         * If the type is [SubscriptionType.LOCAL], it means a single local request.
         * The default is [SubscriptionType.REMOTE].
         *
         * @param subscriptionType
         *             Sets the scope of the current [Topics].
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

        /**
         * A static method to easily build Topics.
         */
        @JvmStatic
        @JvmOverloads
        fun build(
            topic: String,
            qos: Int = 0,
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