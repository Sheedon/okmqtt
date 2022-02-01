package org.sheedon.mqtt

import androidx.annotation.IntRange
import org.sheedon.rr.dispatcher.model.BaseRequest
import org.sheedon.rr.dispatcher.model.BaseRequestBuilder

/**
 * 请求对象，包含内容：「反馈主题」+「超时时长」+「请求消息体」
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 10:58 上午
 */
class Request internal constructor(
    builder: Builder
) : BaseRequest<String, RequestBody>(builder) {

    open class Builder : BaseRequestBuilder<Request, String, RequestBody>() {
        internal var topic: String = ""
        internal var data: String = ""
        internal var qos: Int = 0
        internal var retained: Boolean = false



        override fun backTopic(backTopic: String) = apply {
            super.backTopic(backTopic)
        }

        override fun delayMilliSecond(delayMilliSecond: Int) = apply {
            super.delayMilliSecond(delayMilliSecond)
        }

        override fun delaySecond(delaySecond: Long) = apply {
            super.delaySecond(delaySecond)
        }

        override fun body(body: RequestBody?) = apply {
            super.body(body)
        }

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

        override fun requireBackTopicNull(backTopic: String?): Boolean {
            return backTopic.isNullOrEmpty()
        }

        override fun build(): Request {
            if (super.body() == null) {
                body(RequestBody(topic, data, qos, retained))
            }
            return Request(this)
        }


    }
}