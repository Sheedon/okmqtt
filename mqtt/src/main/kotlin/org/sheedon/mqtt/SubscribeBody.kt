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