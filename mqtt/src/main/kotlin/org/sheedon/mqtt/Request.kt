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
 * Request object, including content: "back topic" + "timeout duration" + "request message body"
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 10:58 上午
 */
class Request internal constructor(
    @get:JvmName("relation") val relation: Relation,
    @get:JvmName("body") val body: RequestBody?,
) {

    override fun toString(): String = buildString {
        append("Request{body=")
        append(body)
        append(",relation=")
        append(relation)
        append('}')
    }

    open class Builder {
        internal var topic: String = ""
        internal var data: String = ""
        internal var qos: Int = 0
        internal var retained: Boolean = false
        internal var charset: String? = null
        internal var relation: Relation.Builder = Relation.Builder()
        internal var body: RequestBody? = null

        /**
         * 设置请求主题
         * @param topic 请求主题
         */
        open fun topic(topic: String) = apply {
            this.topic = topic
        }

        /**
         * 设置请求数据
         * @param data 请求数据
         */
        open fun data(data: String) = apply {
            this.data = data
        }

        /**
         * 设置请求数据
         * @param data 请求数据
         */
        open fun data(data: ByteArray) = apply {
            this.data = data.toString()
        }

        /**
         * 设置请求消息质量 0，1，2
         * @param qos 请求消息质量
         */
        open fun qos(@IntRange(from = 0, to = 2) qos: Int) = apply {
            this.qos = qos
        }

        /**
         * 设置请求消息是否保留
         * @param retained 是否保留
         */
        open fun retained(retained: Boolean) = apply {
            this.retained = retained
        }

        /**
         * 设置请求消息字符集
         * @param charset 字符集
         */
        open fun charset(charset: String) = apply {
            this.charset = charset
        }

        /**
         * 设置请求消息
         * 包含内容：发送主题、发送消息质量、是否保留、消息
         *
         * @param body 消息内容
         */
        open fun body(body: RequestBody) = apply {
            this.body = body
        }

        /**
         * 响应所订阅的主题，默认消息质量为0
         */
        open fun backTopic(backTopic: String) = apply {
            relation.topics(Topics(backTopic))
        }

        /**
         * 配置关联项，包含响应主题
         */
        open fun relation(relationBuilder: Relation.Builder) = apply {
            relation = relationBuilder
        }

        /**
         * 订阅主题信息
         */
        open fun topics(topics: Topics) = apply {
            relation.topics(topics)
        }

        /**
         * 订阅主题信息
         */
        open fun topics(
            backTopic: String,
            qos: Int = 0,
            attachRecord: Boolean = false,
            subscriptionType: SubscriptionType = SubscriptionType.REMOTE,
            userContext: Any? = null
        ) = apply {
            relation.topics(
                Topics(
                    backTopic,
                    qos,
                    userContext,
                    Headers(attachRecord, subscriptionType)
                )
            )
        }

        /**
         * 绑定的关联字段，若该字段不为""，则代表采用关联字段作为响应消息的匹配字段进行关联
         * 否则取relation内的订阅主题
         */
        open fun keyword(keyword: String) = apply {
            relation.keyword(keyword)
        }

        /**
         * 单次请求超时额外设置
         *
         * @param delayMilliSecond 延迟时间（毫秒）
         */
        open fun delayMilliSecond(delayMilliSecond: Long) = apply {
            relation.delayMilliSecond(delayMilliSecond)
        }

        /**
         * 单次请求超时额外设置
         *
         * @param delaySecond 延迟时间（秒）
         */
        open fun delaySecond(delaySecond: Int) = apply {
            relation.delaySecond(delaySecond)
        }

        open fun build(): Request {
            if (body == null) {
                body(RequestBody(topic, data, qos, retained, charset))
            }

            return Request(relation.build(), body)
        }


    }
}