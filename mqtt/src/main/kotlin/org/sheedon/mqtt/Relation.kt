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
    @get:JvmName("subscribe") val subscribe: Subscribe? = null,// 需要订阅的集合
    @get:JvmName("keyword") val keyword: String? = null,// 订阅到关键字
    @get:JvmName("timeOut") val timeOut: Long? = null,// 超时时长
) {

    fun newBuilder(): Builder {
        val result = Builder()
        result.subscribe = subscribe
        result.keyword = keyword
        result.timeOut = timeOut
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is Relation && subscribe == other
    }

    override fun hashCode(): Int {
        return subscribe?.hashCode() ?: 0
    }

    override fun toString(): String {
        return buildString {
            subscribe?.also {
                append("{topic:${subscribe.topic}, qos:${subscribe.qos}}")
            }
        }
    }

    open class Builder {
        // 订阅主题
        internal var subscribe: Subscribe? = null

        // 订阅到关键字
        internal var keyword: String? = null

        // 超时时长
        internal var timeOut: Long? = null

        // 是否添加绑定行为，订阅主题/关键字，当前版本只能二选一
        internal var getSubscribe = false
        internal var getKeyWork = false

        /**
         * 需要订阅mqtt主题
         *
         * @param subscribe mqtt主题
         * @return Builder
         */
        @Throws(IllegalStateException::class)
        open fun subscribe(subscribe: Subscribe) = apply {
            this.getSubscribe = true
            if (getKeyWork) {
                throw IllegalStateException("Only one of them can be selected keyword and subscribe")
            }
            this.subscribe = subscribe
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
                throw IllegalStateException("Only one of them can be selected keyword and subscribe")
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
            this.timeOut = delayMilliSecond
        }

        /**
         * 单次请求超时额外设置
         *
         * @param delaySecond 延迟时间（秒）
         * @return Builder
         */
        open fun delaySecond(delaySecond: Int) = apply {
            this.timeOut = delaySecond * 1000L
        }

        open fun build(): Relation = Relation(subscribe, keyword, timeOut)
    }
}