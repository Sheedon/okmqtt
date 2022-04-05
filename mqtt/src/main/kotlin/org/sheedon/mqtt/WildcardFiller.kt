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
    var topicsBodies: MutableMap<String, Topics> = mutableMapOf(),
    val subscribeOptimize: Boolean = false
) {

    private val fillerBodies: MutableMap<String, Topics> = mutableMapOf()
    private val rootNote = TopicsNote("root")

    /**
     * 订阅主题
     *
     * @param body 单项订阅内容
     */
    internal fun subscribe(body: Topics, client: MqttWrapperClient) {
        topicsBodies
            .takeUnless {
                topicsBodies.containsKey(body.convertKey()) || !needSubscribe(body)
            }?.let {
                it[body.convertKey()] = body
            }?.also {
                var filter: Set<Topics> = emptySet()
                body.topic.also {
                    if (it.endsWith("#")) {
                        filter = filterByWildcard1(it)

                    } else if (it.endsWith("+")) {
                        filter = filterByWildcard2(it)
                    }
                }
                if (filter.isNotEmpty()) {
                    client.unsubscribe(filter.toList())
                }
            }
    }

    /**
     * 订阅主题
     *
     * @param bodies 订阅内容集合
     */
    internal fun subscribe(
        bodies: List<Topics>,
        client: MqttWrapperClient
    ): Pair<List<String>, List<Int>> {

        val topic = mutableListOf<String>()
        val qos = mutableListOf<Int>()
        val filter: MutableSet<Topics> = mutableSetOf()

        bodies.forEach { body ->
            topicsBodies.takeIf {
                !topicsBodies.containsKey(body.convertKey()) && needSubscribe(body)
            }?.also {
                topic.add(body.topic)
                qos.add(body.qos)
            }?.takeIf {
                body.headers.attachRecord
            }?.let {
                it[body.convertKey()] = body
                body
            }?.also {
                // TODO 存在隐患，主题级别
                body.topic.also {
                    if (it.endsWith("#")) {
                        filter.addAll(filterByWildcard1(it))
                    } else if (it.endsWith("+")) {
                        filter.addAll(filterByWildcard2(it))
                    }
                }
            }
        }

        if (filter.isNotEmpty()) {
            client.unsubscribe(filter.toList())
        }

        return Pair(topic, qos)
    }

    /**
     * 过滤通配符「#」
     * 当新增的主题后缀为#，要过滤能用该通配符匹配的主题
     */
    private fun filterByWildcard1(topic: String): MutableSet<Topics> {
        val target = topic.take(topic.length - 1)
        val result = mutableSetOf<Topics>()

        val iterator = topicsBodies.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.startsWith(target)) {
                result.add(topicsBodies[key]!!)
                fillerBodies[key] = topicsBodies[key]!!
                iterator.remove()
                topicsBodies.remove(key)
            }
        }
        fillerBodies[topic]?.also {
            result.remove(it)
            topicsBodies[topic] = it
        }
        fillerBodies.remove(topic)
        return result
    }

    /**
     * 过滤通配符「+」
     * 当新增的主题后缀为+，要过滤能用该通配符匹配的主题
     */
    private fun filterByWildcard2(topic: String): MutableSet<Topics> {
        val target = topic.take(topic.length - 1)
        val result = mutableSetOf<Topics>()

        val iterator = topicsBodies.keys.iterator()
        val length = topic.length
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.startsWith(target) && key.lastIndexOf("/") < length) {
                result.add(topicsBodies[key]!!)
                fillerBodies[key] = topicsBodies[key]!!
                iterator.remove()
                topicsBodies.remove(key)
            }
        }
        fillerBodies[topic]?.also {
            result.remove(it)
            topicsBodies[topic] = it
        }
        fillerBodies.remove(topic)
        return result
    }


    /**
     * 取消订阅主题
     *
     * @param body 单项订阅内容
     */
    internal fun unsubscribe(
        body: Topics,
        client: MqttWrapperClient
    ) {
        var filter: MutableSet<Topics> = mutableSetOf()

        topicsBodies
            .takeIf {
                needUnsubscribe(body)
                topicsBodies.containsKey(body.convertKey())
            }?.remove(body.convertKey())
            ?.takeIf {
                body.topic.endsWith("#") || body.topic.endsWith("+")
            }?.run {
                val topic = body.topic
                when {
                    topic.endsWith("#") -> {
                        filter = appendTopic1(topic)
                    }
                    topic.endsWith("+") -> {
                        filter = appendTopic2(topic)
                    }
                    else -> {

                    }
                }
            }


        if (filter.isNotEmpty()) {
            client.subscribe(filter.toList())
        }
    }

    /**
     * 取消订阅主题
     *
     * @param bodies 订阅内容集合
     */
    internal fun unsubscribe(
        bodies: List<Topics>,
        client: MqttWrapperClient
    ): List<String> {

        val topic = mutableListOf<String>()
        val qos = mutableListOf<Int>()
        val filter: MutableSet<Topics> = mutableSetOf()

        bodies.forEach { body ->
            topicsBodies
                .takeIf {
                    needUnsubscribe(body)
                    topicsBodies.containsKey(body.convertKey())
                }?.also {
                    topic.add(body.topic)
                    qos.add(body.qos)
                }?.takeIf {
                    body.headers.attachRecord
                }?.remove(body.convertKey())
                ?.takeIf {
                    body.topic.endsWith("#") || body.topic.endsWith("+")
                }?.run {
                    val topic = body.topic
                    when {
                        topic.endsWith("#") -> {
                            filter.addAll(appendTopic1(topic))
                        }
                        topic.endsWith("+") -> {
                            filter.addAll(appendTopic2(topic))
                        }
                        else -> {

                        }
                    }
                }
        }

        if (filter.isNotEmpty()) {
            client.subscribe(filter.toList())
        }

        return topic
    }

    /**
     * 过滤通配符「#」
     * 当新增的主题后缀为#，要过滤能用该通配符匹配的主题
     */
    private fun appendTopic1(topic: String): MutableSet<Topics> {
        val target = topic.take(topic.length - 1)
        val result = mutableSetOf<Topics>()

        val iterator = fillerBodies.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.startsWith(target)) {
                result.add(fillerBodies[key]!!)
                topicsBodies[key] = fillerBodies[key]!!
                iterator.remove()
                fillerBodies.remove(key)
            }
        }
        return result
    }

    /**
     * 过滤通配符「+」
     * 当新增的主题后缀为+，要过滤能用该通配符匹配的主题
     */
    private fun appendTopic2(topic: String): MutableSet<Topics> {
        val target = topic.take(topic.length - 1)
        val result = mutableSetOf<Topics>()

        val iterator = fillerBodies.keys.iterator()
        val length = topic.length
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.startsWith(target) && key.lastIndexOf("/") < length) {
                result.add(fillerBodies[key]!!)
                topicsBodies[key] = fillerBodies[key]!!
                iterator.remove()
                fillerBodies.remove(key)
            }
        }
        return result
    }

    /**
     * 是否需要订阅新主题
     * @param body 订阅主题
     * @return true:需要订阅，false:无需订阅
     */
    private fun needSubscribe(body: Topics): Boolean {
        if (!subscribeOptimize) return true

        var needAddSubscribe = true
        var currentNote = rootNote
        var hierarchy = -1

        body.topic.split("/").forEach {
            if (needAddSubscribe) {
                when {
                    currentNote.child["#"] != null -> needAddSubscribe = false
                    currentNote.child["+"] != null -> hierarchy = 0
                    else -> hierarchy--
                }
            }

            if (currentNote.child[it] == null) {
                currentNote.child[it] = TopicsNote(it)
            }
            currentNote = currentNote.child[it]!!
        }
        currentNote.enable = true

        if (hierarchy == 0) {
            return false
        }
        return needAddSubscribe
    }

    /**
     * 是否需要取消订阅新主题
     * @param body 订阅主题
     * @return true:需要取消订阅，false:无需取消订阅
     */
    private fun needUnsubscribe(body: Topics) {
        if (!subscribeOptimize) return

        val array = body.topic.split("/")
        val currentNote = rootNote
        removeLastNote(currentNote, array, 0)
    }

    private fun removeLastNote(topicsNote: TopicsNote, array: List<String>, position: Int) {
        if (position == array.size) {
            return
        }
        val noteName = array[position]
        val nextNote = topicsNote.child[noteName] ?: return
        removeLastNote(nextNote, array, position + 1)
        if (position == array.size - 1 || (nextNote.child.isEmpty() && !nextNote.enable)) {
            topicsNote.child.remove(noteName)
        }
    }

    /**
     * 获取订阅主题内容集合
     */
    internal fun getSubscribeBodyList(): List<Topics> {
        return topicsBodies.values.toMutableList()
    }

    /**
     * 清空订阅
     */
    internal fun clear() {
        topicsBodies.clear()
    }

}