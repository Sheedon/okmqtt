package org.sheedon.mqtt

/**
 * Topic header information, which records the configuration information of the currently subscribed topic
 * AttachRecord:need logged to the cache to facilitate subscription in case of reconnection,default false
 * SubscriptionType:「REMOTE - remote subscription」/「LOCAL - single local subscription」,default REMOTE
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


    class Builder {
        private var attachRecord: Boolean = false
        private var subscriptionType: SubscriptionType = SubscriptionType.REMOTE

        /**
         * need logged to the cache to facilitate subscription in case of reconnection,default false
         */
        fun attachRecord(attachRecord: Boolean) = apply {
            this.attachRecord = attachRecord
        }

        /**
         * 「REMOTE - remote subscription」/「LOCAL - single local subscription」,default REMOTE
         */
        fun subscriptionType(subscriptionType: SubscriptionType) = apply {
            this.subscriptionType = subscriptionType
        }

        fun build(): Headers {
            return Headers(attachRecord, subscriptionType)
        }

    }

}