package org.sheedon.mqtt

import kotlin.jvm.Throws

/**
 * The purpose is to associate a request object with key data that acts on the response.
 *
 * The associated information is [topics] and [keyword], If both are configured,
 * limited use of [keyword].
 *
 * One is to configure the [topic],
 * which extends the logic of MQTT to subscribe messages through topic,
 * The subscription topic is consistent with the target topic, or the subscription topic uses wildcards,
 * and the target topic is within the coverage of the wildcard, that is, the association is completed.
 *
 * Other is the configuration parameter [keyword]. Developers can add a keyword conversion
 * strategy in [OkMqttClient.Builder.keywordConverter] as the keyword matching logic.
 *
 * And if [Relation.timeout] is not set, the global timeout is used by default.
 * Otherwise, the timeout duration is set according to the custom timeout.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/7 10:52 下午
 */
class Relation private constructor(
    @get:JvmName("topics") val topics: Topics? = null,
    @get:JvmName("keyword") val keyword: String? = null,
    @get:JvmName("timeout") val timeout: Long? = null,
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
            keyword?.also {
                append("{keyword:${keyword}}")
            }
            append(",{timeout:$timeout}")

        }
    }

    open class Builder {
        internal var topics: Topics? = null
        internal var keyword: String? = null
        internal var timeout: Long? = null

        /**
         * Sets mqtt topic used to match the response topic.
         *
         * @param topics mqtt topic configuration information
         * @return Builder
         */
        @Throws(IllegalStateException::class)
        open fun topics(topics: Topics) = apply {
            this.topics = topics
        }

        /**
         * Sets the keyword of the response message
         *
         * If the field is not "", it means that the correlation field is used as the matching field
         * of the response message for correlation; otherwise, the subscription topic in the relation is taken.
         *
         * Note: It is hoped that in the case of subscribing to the same topic, the short-term
         * request will use the topic uniformly, or use the keyword instead of mixing
         * (sometimes there is no keyword). In this case, there is a response error.
         *
         * @param keyword the keyword of the response message
         */
        open fun keyword(keyword: String) = apply {
            this.keyword = keyword
        }

        /**
         * Sets the request timeout value.
         * This value, measured in millisecond,defines the maximum time interval
         * the request will wait for the network callback to the MQTT Message response to be established.
         *
         * @param delayMilliSecond request timeout value (millisecond)
         */
        open fun delayMilliSecond(delayMilliSecond: Long) = apply {
            this.timeout = delayMilliSecond
        }

        /**
         * Sets the request timeout value.
         * This value, measured in seconds,defines the maximum time interval
         * the request will wait for the network callback to the MQTT Message response to be established.
         *
         * @param delayMilliSecond request timeout value (seconds)
         */
        open fun delaySecond(delaySecond: Int) = apply {
            this.timeout = delaySecond * 1000L
        }

        open fun build(): Relation = Relation(topics, keyword, timeout)
    }
}