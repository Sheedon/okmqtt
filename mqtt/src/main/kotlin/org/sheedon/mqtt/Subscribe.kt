package org.sheedon.mqtt

import java.util.ArrayList

/**
 * Subscribe object, including content: "relations" is Relation of Array
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

    class Builder {
        val relations: MutableList<Relation> = ArrayList()

        /**
         * Add a subscription association object
         *
         * @param topic if [topic] is not a valid MQTT topic. Avoid this
         *     exception by calling [MQTT topic.parse]; it returns null for invalid Topic.
         * @param keyword keyword under subscription
         * @param qos Sets the qos target of this topics.
         * @param attachRecord Whether to append to the cache record, if false,
         * it means a single subscription, after clearing the behavior, it will not be restored
         * @param subscriptionType Set the subscription type. If [SubscriptionType.REMOTE] is used, it means mqtt+local is required.
         * If the type is [SubscriptionType.LOCAL], it means a single local request.
         * The default is [SubscriptionType.REMOTE]
         */
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