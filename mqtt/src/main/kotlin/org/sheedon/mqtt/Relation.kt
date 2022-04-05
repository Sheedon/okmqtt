package org.sheedon.mqtt

import kotlin.jvm.Throws

/**
 * 请求对象的关联者
 * 版本1，新增订阅主题集合
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/7 10:52 下午
 */
class Relation private constructor(
    @get:JvmName("topics") val topics: Topics? = null,// 需要订阅的集合
    @get:JvmName("keyword") val keyword: String? = null,// 订阅到关键字
    @get:JvmName("timeout") val timeout: Long? = null,// 超时时长
) {

    fun newBuilder(): Builder {
        val result = Builder()
        result.topics = topics
        result.keyword = keyword
        result.timeout = timeout
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is Relation && topics == other
    }

    override fun hashCode(): Int {
        return topics?.hashCode() ?: 0
    }

    override fun toString(): String {
        return buildString {
            topics?.also {
                append("{topic:${topics.topic}, qos:${topics.qos}}")
            }
        }
    }

    open class Builder {
        // 订阅主题
        internal var topics: Topics? = null

        // 订阅到关键字
        internal var keyword: String? = null

        // 超时时长
        internal var timeout: Long? = null

        // 是否添加绑定行为，订阅主题/关键字，当前版本只能二选一
        internal var getSubscribe = false
        internal var getKeyWork = false

        /**
         * 需要订阅mqtt主题
         *
         * @param topics mqtt主题
         * @return Builder
         */
        @Throws(IllegalStateException::class)
        open fun topics(topics: Topics) = apply {
            this.getSubscribe = true
            if (getKeyWork) {
                throw IllegalStateException("Only one of them can be selected keyword and topics")
            }
            this.topics = topics
        }

        /**
         * 订阅下的响应关键字
         *
         * @param keyword 关键字
         * @return Builder
         */
        open fun keyword(keyword: String) = apply {
            this.getKeyWork = true
            if (getSubscribe) {
                throw IllegalStateException("Only one of them can be selected keyword and topics")
            }
            this.keyword = keyword
        }

        /**
         * 单次请求超时额外设置
         *
         * @param delayMilliSecond 延迟时间（毫秒）
         * @return Builder
         */
        open fun delayMilliSecond(delayMilliSecond: Long) = apply {
            this.timeout = delayMilliSecond
        }

        /**
         * 单次请求超时额外设置
         *
         * @param delaySecond 延迟时间（秒）
         * @return Builder
         */
        open fun delaySecond(delaySecond: Int) = apply {
            this.timeout = delaySecond * 1000L
        }

        open fun build(): Relation = Relation(topics, keyword, timeout)
    }
}