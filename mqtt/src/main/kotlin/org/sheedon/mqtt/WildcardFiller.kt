package org.sheedon.mqtt

/**
 * 通配符过滤类
 *
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/25 5:58 下午
 */
internal class WildcardFiller @JvmOverloads constructor(
    _topicsBodies: List<Topics> = mutableListOf()
) {

    // 主题消息池
    private val topicsPool = TopicsPool()

    // 订阅的主题集合
    var topicsBodies: MutableSet<Topics> = mutableSetOf()

    init {
        val pop = topicsPool.pop(_topicsBodies)
        topicsBodies.addAll(pop.first)
    }

    /**
     * 订阅主题
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
     * 订阅主题
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
     * 取消订阅主题
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
     * 取消订阅主题
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