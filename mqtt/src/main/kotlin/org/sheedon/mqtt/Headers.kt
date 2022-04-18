package org.sheedon.mqtt

/**
 * Topic header information, which records the configuration information of the currently subscribed topic
 *
 * AttachRecord:need logged to the cache to facilitate subscription in case of reconnection,
 * default it is false,that don't log into the cache.
 *
 * SubscriptionType:The scope of the current [Topics], If [SubscriptionType.REMOTE] is used,
 * that means that the current [Topics] performs MQTT Topic subscription without subscribing,
 * and associates the current [Topics] with the callback implementation.
 * If the type is [SubscriptionType.LOCAL], that just associated with callback, no need to subscribe to mqtt topic.
 * The default is [SubscriptionType.REMOTE].
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/3 4:00 下午
 */
class Headers internal constructor(
    internal val attachRecord: Boolean = false,
    internal val subscriptionType: SubscriptionType = SubscriptionType.REMOTE,
) {

    fun toBuilder(): Builder {
        val builder = Builder()
        builder.attachRecord(attachRecord)
        builder.subscriptionType(subscriptionType)
        return builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Headers

        if (attachRecord != other.attachRecord) return false
        if (subscriptionType != other.subscriptionType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attachRecord.hashCode()
        result = 31 * result + subscriptionType.hashCode()
        return result
    }


    class Builder {
        private var attachRecord: Boolean = false
        private var subscriptionType: SubscriptionType = SubscriptionType.REMOTE

        /**
         * Sets the attachRecord target of this topics.
         *
         * Whether to append to the cache record, if false,
         * it means a single subscription, after clearing the behavior, it will not be restored.
         *
         * @param attachRecord
         *            Whether to append to the cache record.
         */
        fun attachRecord(attachRecord: Boolean) = apply {
            this.attachRecord = attachRecord
        }

        /**
         * Sets the subscriptionType target of this topics.
         *
         * The scope of the current [Topics], If [SubscriptionType.REMOTE] is used, that means that
         * the current [Topics] performs MQTT Topic subscription without subscribing, and associates
         * the current [Topics] with the callback implementation. If the type is [SubscriptionType.LOCAL],
         * that just associated with callback, no need to subscribe to mqtt topic.
         * The default is [SubscriptionType.REMOTE].
         *
         * @param subscriptionType
         *             Sets the scope of the current [Topics].
         */
        fun subscriptionType(subscriptionType: SubscriptionType) = apply {
            this.subscriptionType = subscriptionType
        }

        fun build(): Headers {
            return Headers(attachRecord, subscriptionType)
        }

    }

}