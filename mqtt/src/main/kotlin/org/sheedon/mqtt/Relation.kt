package org.sheedon.mqtt

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
) {

    fun newBuilder(): Builder {
        val result = Builder()
        result.subscribe = subscribe
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

    class Builder {
        internal var subscribe: Subscribe? = null

        fun subscribe(subscribe: Subscribe) = apply {
            this.subscribe = subscribe
        }

        fun build(): Relation = Relation(subscribe)
    }
}