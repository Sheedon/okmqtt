package org.sheedon.mqtt.internal

import org.sheedon.mqtt.Topics

/**
 * 通配符过滤类
 *
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/25 5:58 下午
 */
internal class WildcardFilter @JvmOverloads constructor(
    _topicsBodies: List<Topics> = mutableListOf()
) {

    // 主题消息池
    private val topicsPool = TopicsPool()

    // 订阅的主题集合
    var topicsBodies: MutableSet<Topics> = mutableSetOf()

    /**
     * 将构建WildcardFilter传入的订阅主题集合，通过topicsPool核实后，返回当前需要订阅的主题集合
     * */
    init {
        val (addTopics, _) = topicsPool.push(_topicsBodies)
        topicsBodies.addAll(addTopics)
    }

    /**
     * 订阅主题，借由topicsPool存储订阅的主题，并且返回两个订阅集合[addTopics],[removeTopics]
     * [addTopics]: 代表当次需要订阅的主题，一般若有值，那必然是body。
     * [removeTopics]: 代表当次需要取消订阅的主题，若订阅是带通配符的主题，那么对应的历史订阅在该主题下的「mqtt主题」
     * 就需要移除，否则将mqtt中存在重复接收同一个主题消息的问题。
     * 例如：已订阅「AA/BB/CC」，当前订阅「AA/BB/#」,那么接收到「AA/BB/CC」的主题消息时，「AA/BB/CC」和「AA/BB/#」
     * 各会收到一次，不利于开发区分。
     *
     * @param body 单项订阅内容
     */
    internal fun subscribe(
        body: Topics,
        unsubscribeRealTopic: (topics: Collection<String>) -> Unit
    ): Topics? {
        val (addTopics, removeTopics) = topicsPool.push(body)

        // 需要订阅的主题不为空，则添加订阅
        if (addTopics != null) {
            topicsBodies.add(addTopics)
        }

        // 取消订阅的主题不为空，则取消订阅
        unsubscribeTopic(removeTopics, unsubscribeRealTopic)

        return addTopics
    }

    /**
     * 订阅主题，借由topicsPool存储订阅的主题，并且返回两个订阅集合[addTopics],[removeTopics]
     * [addTopics]: 代表当次需要订阅的主题，一般若有值，那必然是在bodies中。
     * [removeTopics]: 代表当次需要取消订阅的主题，若订阅是带通配符的主题，那么对应的历史订阅在该主题下的「mqtt主题」
     * 就需要移除，否则将mqtt中存在重复接收同一个主题消息的问题。
     * 例如：已订阅「AA/BB/CC」，当前订阅「AA/BB/#」,那么接收到「AA/BB/CC」的主题消息时，「AA/BB/CC」和「AA/BB/#」
     * 各会收到一次，不利于开发区分。
     *
     * 批量订阅，需要从订阅的头部文件中取值attachRecord，来决定是否保留到当前记录中，用于后续mqtt重连后，自动订阅。
     *
     * @param bodies 订阅内容集合
     */
    internal fun subscribe(
        bodies: List<Topics>,
        unsubscribeRealTopic: (topics: Collection<String>) -> Unit
    ): Pair<Collection<String>, Collection<Int>> {

        val (addTopics, removeTopics) = topicsPool.push(bodies)

        // 订阅信息
        val topic = mutableListOf<String>()
        val qos = mutableListOf<Int>()
        addTopics.forEach {
            if (it.headers.attachRecord) {
                topicsBodies.add(it)
            }
            topic.add(it.topic)
            qos.add(it.qos)
        }

        // 取消订阅的主题不为空，则取消订阅
        unsubscribeTopic(removeTopics, unsubscribeRealTopic)

        return Pair(topic, qos)
    }

    /**
     * 取消订阅主题
     */
    private fun unsubscribeTopic(
        topic: Set<Topics>,
        unsubscribeRealTopic: (topics: Collection<String>) -> Unit
    ) {
        // 取消订阅的主题不为空，则取消订阅
        topic.map {
            if (it.headers.attachRecord) {
                topicsBodies.remove(it)
            }
            it.topic
        }.takeIf {
            it.isNotEmpty()
        }?.run {
            unsubscribeRealTopic(this)
        }
    }


    /**
     * 取消订阅主题，借由topicsPool存储订阅的主题，并且返回两个订阅集合[addTopics],[removeTopics]
     * [addTopics]: 代表当次需要订阅的主题，在取消订阅主题后，需要恢复的原本被包含的订阅主题。
     * [removeTopics]: 代表当次需要取消订阅的主题，若有值必然是body。
     * 例如：已订阅「AA/BB/CC」（已被取消订阅），「AA/BB/#」，当前取消订阅「AA/BB/#」，对应需要恢复「AA/BB/CC」的订阅。
     *
     * @param body 单项订阅内容
     */
    internal fun unsubscribe(
        body: Topics,
        subscribeRealTopic: (Collection<String>, Collection<Int>) -> Unit
    ): Topics? {

        val (addTopics, removeTopics) = topicsPool.pop(body)
        // 需要取消订阅的主题不为空，则移除订阅
        if (removeTopics != null) {
            topicsBodies.remove(removeTopics)
        }

        // 订阅的主题不为空，则添加订阅
        subscribeTopic(addTopics, subscribeRealTopic)

        return removeTopics
    }

    /**
     * 取消订阅主题，借由topicsPool存储订阅的主题，并且返回两个订阅集合[addTopics],[removeTopics]
     * [addTopics]: 代表当次需要订阅的主题，在取消订阅主题后，需要恢复的原本被包含的订阅主题。
     * [removeTopics]: 代表当次需要取消订阅的主题，一般在bodies中。
     * 例如：已订阅「AA/BB/CC」（已被取消订阅），「AA/BB/#」，当前取消订阅「AA/BB/#」，对应需要恢复「AA/BB/CC」的订阅。
     *
     * 需要从取消订阅的头部文件中取值attachRecord，来决定是否保留到当前记录中，用于后续mqtt重连后，自动订阅。
     *
     * @param bodies 订阅内容集合
     */
    internal fun unsubscribe(
        bodies: List<Topics>,
        subscribeRealTopic: (Collection<String>, Collection<Int>) -> Unit
    ): Collection<String> {

        val (addTopics, removeTopics) = topicsPool.pop(bodies)

        // 取消订阅信息
        val topic = mutableListOf<String>()
        removeTopics.forEach {
            if (it.headers.attachRecord) {
                topicsBodies.add(it)
            }
            topic.add(it.topic)
        }

        // 订阅的主题不为空，则添加订阅
        subscribeTopic(addTopics, subscribeRealTopic)

        return topic
    }

    /**
     * 添加订阅主题
     */
    private fun subscribeTopic(
        topic: Set<Topics>,
        subscribeRealTopic: (Collection<String>, Collection<Int>) -> Unit
    ) {
        if (topic.isEmpty()) return

        // 订阅的主题不为空，则添加订阅
        val topicArray = ArrayList<String>()
        val qosArray = ArrayList<Int>()
        topic.forEach {
            if (it.headers.attachRecord) {
                topicsBodies.add(it)
            }
            topicArray.add(it.topic)
            qosArray.add(it.qos)
        }

        subscribeRealTopic(topicArray, qosArray)
    }

    /**
     * 清空订阅
     */
    internal fun clear() {
        topicsBodies.clear()
    }

}