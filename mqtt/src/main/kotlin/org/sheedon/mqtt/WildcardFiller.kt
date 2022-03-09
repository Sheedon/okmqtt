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
    var subscribeBodies: MutableMap<String, Subscribe> = mutableMapOf(),
    val subscribeOptimize: Boolean = false
) {

    private val fillerBodies: MutableMap<String, Subscribe> = mutableMapOf()
    private val rootNote = SubscribeNote("root")

    /**
     * 订阅主题
     *
     * @param body 单项订阅内容
     */
    internal fun subscribe(body: Subscribe, client: MqttWrapperClient) {
        subscribeBodies
            .takeUnless {
                subscribeBodies.containsKey(body.convertKey()) || !needSubscribe(body)
            }?.let {
                it[body.convertKey()] = body
            }?.also {
                var filter: Set<Subscribe> = emptySet()
                body.topic?.also {
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
     * @param attachRecord 是否附加到记录中
     */
    internal fun subscribe(
        bodies: List<Subscribe>,
        attachRecord: Boolean = false,
        client: MqttWrapperClient
    ): Pair<List<String>, List<Int>> {

        val topic = mutableListOf<String>()
        val qos = mutableListOf<Int>()
        val filter: MutableSet<Subscribe> = mutableSetOf()

        bodies.forEach { body ->
            subscribeBodies.takeIf {
                !subscribeBodies.containsKey(body.convertKey()) && needSubscribe(body)
            }?.also {
                topic.add(body.topic!!)
                qos.add(body.qos)
            }.takeIf {
                attachRecord
            }?.let {
                it[body.convertKey()] = body
                body
            }?.also {
                // TODO 存在隐患，主题级别
                body.topic?.also {
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
    private fun filterByWildcard1(topic: String): MutableSet<Subscribe> {
        val target = topic.take(topic.length - 1)
        val result = mutableSetOf<Subscribe>()

        val iterator = subscribeBodies.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.startsWith(target)) {
                result.add(subscribeBodies[key]!!)
                fillerBodies[key] = subscribeBodies[key]!!
                iterator.remove()
                subscribeBodies.remove(key)
            }
        }
        fillerBodies[topic]?.also {
            result.remove(it)
            subscribeBodies[topic] = it
        }
        fillerBodies.remove(topic)
        return result
    }

    /**
     * 过滤通配符「+」
     * 当新增的主题后缀为+，要过滤能用该通配符匹配的主题
     */
    private fun filterByWildcard2(topic: String): MutableSet<Subscribe> {
        val target = topic.take(topic.length - 1)
        val result = mutableSetOf<Subscribe>()

        val iterator = subscribeBodies.keys.iterator()
        val length = topic.length
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.startsWith(target) && key.lastIndexOf("/") < length) {
                result.add(subscribeBodies[key]!!)
                fillerBodies[key] = subscribeBodies[key]!!
                iterator.remove()
                subscribeBodies.remove(key)
            }
        }
        fillerBodies[topic]?.also {
            result.remove(it)
            subscribeBodies[topic] = it
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
        body: Subscribe,
        client: MqttWrapperClient
    ) {
        var filter: MutableSet<Subscribe> = mutableSetOf()

        subscribeBodies
            .takeIf {
                needUnsubscribe(body)
                subscribeBodies.containsKey(body.convertKey())
            }?.remove(body.convertKey())
            ?.takeIf {
                body.topic?.endsWith("#") ?: false || body.topic?.endsWith("+") ?: false
            }?.run {
                val topic = body.topic!!
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


        if(filter.isNotEmpty()){
            client.subscribe(filter.toList())
        }
    }

    /**
     * 取消订阅主题
     *
     * @param bodies 订阅内容集合
     * @param attachRecord 是否附加到记录中
     */
    internal fun unsubscribe(
        bodies: List<Subscribe>,
        attachRecord: Boolean = false,
        client: MqttWrapperClient
    ): List<String> {

        val topic = mutableListOf<String>()
        val qos = mutableListOf<Int>()
        val filter: MutableSet<Subscribe> = mutableSetOf()

        bodies.forEach { body ->
            subscribeBodies
                .takeIf {
                    needUnsubscribe(body)
                    subscribeBodies.containsKey(body.convertKey())
                }?.also {
                    topic.add(body.topic!!)
                    qos.add(body.qos)
                }.takeIf {
                    attachRecord
                }?.remove(body.convertKey())
                ?.takeIf {
                    body.topic?.endsWith("#") ?: false || body.topic?.endsWith("+") ?: false
                }?.run {
                    val topic = body.topic!!
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

        if(filter.isNotEmpty()){
            client.subscribe(filter.toList())
        }

        return topic
    }

    /**
     * 过滤通配符「#」
     * 当新增的主题后缀为#，要过滤能用该通配符匹配的主题
     */
    private fun appendTopic1(topic: String): MutableSet<Subscribe> {
        val target = topic.take(topic.length - 1)
        val result = mutableSetOf<Subscribe>()

        val iterator = fillerBodies.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.startsWith(target)) {
                result.add(fillerBodies[key]!!)
                subscribeBodies[key] = fillerBodies[key]!!
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
    private fun appendTopic2(topic: String): MutableSet<Subscribe> {
        val target = topic.take(topic.length - 1)
        val result = mutableSetOf<Subscribe>()

        val iterator = fillerBodies.keys.iterator()
        val length = topic.length
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.startsWith(target) && key.lastIndexOf("/") < length) {
                result.add(fillerBodies[key]!!)
                subscribeBodies[key] = fillerBodies[key]!!
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
    private fun needSubscribe(body: Subscribe): Boolean {
        if (!subscribeOptimize) return true

        var needAddSubscribe = true
        var currentNote = rootNote
        var hierarchy = -1

        body.topic?.split("/")?.forEach {
            if (needAddSubscribe) {
                when {
                    currentNote.child["#"] != null -> needAddSubscribe = false
                    currentNote.child["+"] != null -> hierarchy = 0
                    else -> hierarchy--
                }
            }

            if (currentNote.child[it] == null) {
                currentNote.child[it] = SubscribeNote(it)
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
    private fun needUnsubscribe(body: Subscribe) {
        if (!subscribeOptimize) return

        val array = body.topic?.split("/") ?: arrayListOf()
        val currentNote = rootNote
        removeLastNote(currentNote, array, 0)
    }

    private fun removeLastNote(subscribeNote: SubscribeNote, array: List<String>, position: Int) {
        if (position == array.size) {
            return
        }
        val noteName = array[position]
        val nextNote = subscribeNote.child[noteName] ?: return
        removeLastNote(nextNote, array, position + 1)
        if (position == array.size - 1 || (nextNote.child.isEmpty() && !nextNote.enable)) {
            subscribeNote.child.remove(noteName)
        }
    }

    /**
     * 获取订阅主题内容集合
     */
    internal fun getSubscribeBodyList(): List<Subscribe> {
        return subscribeBodies.values.toMutableList()
    }

    /**
     * 清空订阅
     */
    internal fun clear() {
        subscribeBodies.clear()
    }

}