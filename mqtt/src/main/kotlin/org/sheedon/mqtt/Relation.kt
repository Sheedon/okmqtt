package org.sheedon.mqtt

import kotlin.collections.ArrayList

/**
 * 请求对象的关联者
 * 版本1，新增订阅主题集合
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/7 10:52 下午
 */
class Relation private constructor(
    @get:JvmName("subscribeArray") val subscribeArray: Array<Subscribe>? = null,// 需要订阅的集合
) {

    fun newBuilder(): Builder {
        val result = Builder()
        result.subscribeArray.addAll(subscribeArray ?: arrayOf())
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is Relation && subscribeArray.contentEquals(other.subscribeArray)
    }

    override fun hashCode(): Int {
        return subscribeArray?.contentHashCode() ?: 0
    }

    override fun toString(): String {
        return buildString {
            if (subscribeArray == null) {
                append("subscribeArray == null")
                return@buildString
            }
            subscribeArray.forEachIndexed { index, subscribe ->
                if (index > 0) {
                    append(", ")
                }
                append("{topic:${subscribe.topic}, qos:${subscribe.qos}}")
            }
        }
    }

    class Builder {
        internal val subscribeArray: MutableList<Subscribe> = ArrayList(2)

        fun addAll(subscribeArray: MutableList<Subscribe>) = apply {
            this.subscribeArray.addAll(subscribeArray)
        }

        fun add(subscribe: Subscribe) = apply {
            this.subscribeArray.add(subscribe)
        }

        fun build(): Relation = Relation(subscribeArray.toTypedArray())
    }
}