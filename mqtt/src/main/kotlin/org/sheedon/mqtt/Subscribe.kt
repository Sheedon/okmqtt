package org.sheedon.mqtt

import java.util.ArrayList

/**
 * Subscribe object, including content: "relations" is Relation of Array.
 *
 * If more than one topic needs to be subscribed, and it cannot be subscribed through a topic with
 * a wildcard, and the callback format is consistent, you can use [Subscribe] as the subscription object
 * to subscribe to the mqtt topic.
 *
 * For example:
 * Subscription object 1: AA/BB/CC .
 * Subscription object 2: AA/DD/# .
 * Subscription object 3: AA/EE/+ .
 * But do not want to subscribe to AA directly, so the current subscription object can be used
 * for implementation.
 *
 * #[Note] that the feedback format here needs to be consistent, and only one callback format can be
 * specified for the same request subscription behavior.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/3 6:57 下午
 */
class Subscribe internal constructor(
    @get:JvmName("relations") val relations: Array<Relation>
) {

    fun size(): Int = relations.size

    fun newBuilder(): Builder {
        val builder = Builder()
        builder.addAll(relations.toList())
        return builder
    }

    fun getTopicArray(): Pair<Array<String>, IntArray> {
        val topicArray = arrayListOf<String>()
        val qosArray = arrayListOf<Int>()
        relations.forEach {
            topicArray.add(it.topics?.topic ?: "")
            qosArray.add(it.topics?.qos ?: 0)
        }
        return Pair(topicArray.toTypedArray(), qosArray.toIntArray())
    }

    class Builder {
        val relations: MutableList<Relation> = ArrayList()

        /**
         * Add a subscription association object
         *
         * @param topic if [topic] is not a valid MQTT topic. Avoid this
         *     exception by calling [MQTT topic.parse]; it returns null for invalid Topic.
         * @param keyword keyword under subscription. It is hoped that in the case of subscribing to
         * the same topic, the short-term request will use the topic uniformly, or use the keyword
         * instead of mixing (sometimes there is no keyword). In this case, there is a response error.
         * @param qos Sets the qos target of this topics.
         * @param attachRecord Whether to append to the cache record, if false,
         * it means a single subscription, after clearing the behavior, it will not be restored
         * @param subscriptionType Set the subscription type. If [SubscriptionType.REMOTE] is used, it means mqtt+local is required.
         * If the type is [SubscriptionType.LOCAL], it means a single local request.
         * The default is [SubscriptionType.REMOTE]
         */
        @JvmOverloads
        fun add(
            topic: String,
            keyword: String? = null,
            qos: Int = 0,
            attachRecord: Boolean = true,
            subscriptionType: SubscriptionType = SubscriptionType.REMOTE,
        ) = apply {
            val builder = Relation.Builder()
            builder.topics(Topics.build(topic, qos, attachRecord, subscriptionType))
            keyword?.let {
                builder.keyword(it)
            }
            this.relations.add(builder.build())
        }


        /**
         * Add a subscription association object
         *
         * @param relation Associating a subscription object
         */
        fun add(relation: Relation) = apply {
            this.relations.add(relation)
        }

        /**
         * Add a set of subscription associations
         *
         * @param relations Associating a subscription group
         */
        fun addAll(relations: List<Relation>) = apply {
            this.relations.addAll(relations)
        }


        fun build() = Subscribe(relations.toTypedArray())
    }
}