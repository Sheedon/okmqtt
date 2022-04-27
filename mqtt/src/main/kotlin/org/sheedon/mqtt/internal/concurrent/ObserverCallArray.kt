package org.sheedon.mqtt.internal.concurrent

import org.sheedon.mqtt.*
import org.sheedon.mqtt.Observable
import org.sheedon.mqtt.internal.connection.RealObservable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

/**
 * 观察者呼叫集合
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/28 11:06 下午
 */
class ObserverCallArray {


    // 主题队列池，反馈主题为键，同样的反馈主题的内容，依次存入有序队列中
    private val topicCalls = LinkedHashMap<String, LinkedList<ReadyTask>>()

    // topic 任务锁
    private val topicLock = Any()

    // 关键字队列，关键字为键,同样的反馈主题的内容，依次存入有序队列中
    private val keywordCalls = LinkedHashMap<String, LinkedList<ReadyTask>>()

    // keyword 任务锁
    private val keywordLock = Any()

    /**
     * 订阅一个主题（Request所包含），或者订阅一个主题组（Subscribe所包含），并将订阅的消息，
     * 借由offerReadyCalls，添加到就绪任务后，返回订阅的所有消息的ID
     *
     * 1.核实 observable 持有的为Request/Subscribe
     * 若为 Request ，执行单项执行订阅
     * 若为 Subscribe ，执行多项执行订阅
     *
     * 2.以「主题/关键字」为键，由loadId()生产消息ID，存入topicCalls/keywordCalls中
     * 3.构建ReadyTask对象和ID，调度Dispatcher.offerReadyCalls 添加到队列中
     * 4.返回消息ID组，用于后续取消任务或移除任务
     *
     * @param observable 观察者对象，存储基本绑定信息，为反馈做关联
     * @param back 反馈对象，回调信息
     * @param loadId 加载一个消息ID的方法
     * @param offerReadyCalls 提供就绪任务的方法
     * */
    fun subscribe(
        observable: RealObservable,
        back: IBack,
        loadId: () -> Long,
        offerReadyCalls: (Long, ReadyTask) -> Unit
    ): List<Long> {

        if (observable.originalRequest != null) {
            val id = subscribeRequest(observable, back, loadId, offerReadyCalls)
            return listOf(id)
        }

        if (observable.originalSubscribe != null) {
            return subscribeSubscribe(observable, back, loadId, offerReadyCalls)
        }

        return emptyList()
    }

    /**
     * 订阅一个主题（Request所包含），并将订阅的消息，
     * 借由offerReadyCalls，添加到就绪任务后，返回订阅的所有消息的ID
     *
     * 1.以「主题/关键字」为键，由loadId()生产消息ID，存入topicCalls/keywordCalls中
     * 2.构建ReadyTask对象和ID，调度Dispatcher.offerReadyCalls 添加到队列中
     * 3.返回消息ID，用于后续取消任务或移除任务
     *
     * @param observable 观察者对象，存储基本绑定信息，为反馈做关联
     * @param back 反馈对象，回调信息
     * @param loadId 加载一个消息ID的方法
     * @param offerReadyCalls 提供就绪任务的方法
     * */
    private fun subscribeRequest(
        observable: RealObservable,
        back: IBack,
        loadId: () -> Long,
        offerReadyCalls: (Long, ReadyTask) -> Unit
    ): Long {
        // 获取消息ID
        val id = loadId()
        // 创建就绪的任务
        val readyTask = ReadyTask(observable, id, CallbackEnum.RETAIN, back)

        val request = observable.request()
        val relation = request.relation

        // 关键字不存在，则说明必然是订阅主题消息
        val keyword = relation.keyword
        if (keyword.isNullOrEmpty()) {

            val subscribe = relation.topics
            check(subscribe != null) { "please add subscribe by $relation" }

            val queue = getQueueByTopic(subscribe.topic)
            queue.offer(readyTask)
            offerReadyCalls(id, readyTask)
            return id
        }

        val queue = getQueueByKeyword(keyword)
        queue.offer(readyTask)
        offerReadyCalls(id, readyTask)
        return id
    }

    /**
     * 订阅一个主题组（Subscribe所包含），并将订阅的消息，
     * 借由offerReadyCalls，添加到就绪任务后，返回订阅的所有消息的ID

     * 1.以「主题/关键字」为键，由loadId()生产消息ID，存入topicCalls/keywordCalls中
     * 2.构建ReadyTask对象和ID，调度Dispatcher.offerReadyCalls 添加到队列中
     * 3.返回消息ID组，用于后续取消任务或移除任务
     *
     * @param observable 观察者对象，存储基本绑定信息，为反馈做关联
     * @param back 反馈对象，回调信息
     * @param loadId 加载一个消息ID的方法
     * @param offerReadyCalls 提供就绪任务的方法
     * */
    private fun subscribeSubscribe(
        observable: RealObservable,
        back: IBack,
        loadId: () -> Long,
        offerReadyCalls: (Long, ReadyTask) -> Unit
    ): List<Long> {

        val ids = ArrayList<Long>()
        val subscribe = observable.originalSubscribe as Subscribe

        val relations = subscribe.relations
        relations.forEach {
            // 获取消息ID
            val id = loadId()
            // 创建就绪的任务
            val readyTask = ReadyTask(observable, id, CallbackEnum.RETAIN, back)

            // 关键字不存在，则说明必然是订阅主题消息
            val keyword = it.keyword
            if (keyword.isNullOrEmpty()) {

                val topics = it.topics
                check(topics != null) { "please add subscribe by $topics" }

                val queue = getQueueByTopic(topics.topic)
                queue.offer(readyTask)
                offerReadyCalls(id, readyTask)
                ids.add(id)
                return@forEach
            }

            val queue = getQueueByKeyword(keyword)
            queue.offer(readyTask)
            offerReadyCalls(id, readyTask)
            ids.add(id)
        }

        return ids
    }

    /**
     * 取消订阅一个主题（Request所包含），或者订阅一个主题组（Subscribe所包含）
     *
     * 核实 observable 持有的为Request/Subscribe
     * 若为 Request ，执行单项执行取消订阅
     * 若为 Subscribe ，执行多项执行取消订阅
     *
     * @param task 就绪的任务
     * */
    fun unsubscribe(task: ReadyTask) {
        val listen = task.listen
        if (listen !is RealObservable) {
            return
        }

        // 取消订阅「请求对象」中所包含的订阅内容
        val request = listen.originalRequest
        if (request != null) {
            unsubscribeRequest(request)
            return
        }

        // 取消订阅「订阅对象」中所包含的订阅内容
        val subscribe = listen.originalSubscribe
        if (subscribe != null) {
            unsubscribeSubscribe(subscribe)
        }
    }

    /**
     * 取消订阅一个主题 Request所包含
     *
     * 核实 request 请求数据对象
     * */
    private fun unsubscribeRequest(request: Request) {
        // 请求对象的关联信息
        val relation = request.relation
        val keyword = relation.keyword
        // 关键字不存在，则说明必然是订阅主题消息
        if (keyword.isNullOrEmpty()) {

            val subscribe = relation.topics
            check(subscribe != null) { "please add subscribe by $relation" }

            val queue = getQueueByTopic(subscribe.topic)
            queue.poll()
            return
        }
        val queue = getQueueByKeyword(keyword)
        queue.poll()
    }

    /**
     * 取消订阅一个主题 Subscribe所包含的订阅信息
     *
     * 核实 request 请求数据对象
     * */
    private fun unsubscribeSubscribe(subscribe: Subscribe) {
        // 请求对象的关联信息
        subscribe.relations.forEach {
            val keyword = it.keyword
            // 关键字不存在，则说明必然是订阅主题消息
            if (keyword.isNullOrEmpty()) {

                val topics = it.topics
                check(topics != null) { "please add subscribe by $topics" }

                val queue = getQueueByTopic(topics.topic)
                queue.poll()
                return
            }
            val queue = getQueueByKeyword(keyword)
            queue.poll()
        }
    }

    /**
     * 按照匹配条件反馈响应结果，有一下三种情况
     * 1.关键字得到匹配
     * 2.订阅主题得到匹配
     * 3.订阅主题 通配符匹配「+」or「#」
     *
     * @param keyword 关键字
     * @param responseBody 响应内容
     */
    fun callResponse(
        keyword: String?,
        responseBody: ResponseBody
    ) {
        val response = Response(keyword, responseBody)

        // 响应关键字的反馈
        callResponseByKeyword(keyword, response)

        // 响应主题的反馈
        callResponseByTopic(responseBody.topic, response)
    }

    /**
     * 根据主题获取keywordCalls的队列，双重加锁，若队列存在直接返回，不存在则创建后返回
     *
     * @param topic 主题
     * @return topicCalls 队列
     */
    private fun getQueueByTopic(topic: String): LinkedList<ReadyTask> {
        return getQueue(topic, topicCalls, topicLock)
    }

    /**
     * 根据关键字获取 keywordCalls 的队列，双重加锁，若队列存在直接返回，不存在则创建后返回
     *
     * @param keyword 关键字
     * @return keywordCalls 队列
     */
    private fun getQueueByKeyword(keyword: String): LinkedList<ReadyTask> {
        return getQueue(keyword, keywordCalls, keywordLock)
    }

    /**
     * 根据key获取 keyCalls 的消息ID队列，双重加锁，若队列存在直接返回，不存在则创建后返回
     *
     * @param key 关键字
     * @param targetCalls 目标集合
     * @param lock 锁
     *
     * @return 目标队列
     */
    private fun getQueue(
        key: String,
        targetCalls: LinkedHashMap<String, LinkedList<ReadyTask>>,
        lock: Any
    ): LinkedList<ReadyTask> {
        var queue = targetCalls[key]
        if (queue == null) {
            synchronized(lock) {
                queue = targetCalls[key]
                if (queue == null) {
                    queue = LinkedList()
                    targetCalls[key] = queue!!
                }
            }
        }
        return queue!!
    }


    /**
     * 通过关键字来反馈响应结果
     * 1.从关键字集合中取得队列
     * 2.队列中推出第一项
     * 3.推出当前ID的就绪任务
     * 4.处理反馈操作
     *
     * @param keyword 关键字
     * @param response 响应内容
     */
    private fun callResponseByKeyword(
        keyword: String?,
        response: Response
    ) {
        // 关键字为空，则无需接下来操作
        if (keyword.isNullOrEmpty()) {
            return
        }

        // 查找关键字并且反馈响应结果
        findAndCallResponse(keyword, response, ::getQueueByKeyword)
    }

    /**
     * 通过主题来反馈响应结果
     * 1.从主题集合中取得队列
     * 2.队列中推出第一项
     * 3.推出当前ID的就绪任务
     * 4.处理反馈操作
     *
     * @param topic 主题
     * @param response 响应内容
     */
    private fun callResponseByTopic(
        topic: String?,
        response: Response
    ) {
        // 关键字为空，则无需接下来操作
        if (topic.isNullOrEmpty()) {
            return
        }

        // 标准完全匹配的通配符查找并且反馈响应结果
        findAndCallResponse(topic, response, ::getQueueByTopic)


        // 订阅信息带通配符的反馈
        val lastIndex = topic.lastIndexOf(SLASH)
        val fullTopic = if (lastIndex == -1) {
            ""
        } else {
            topic.substring(0, lastIndex)
        }

        // 反馈通配符+
        callResponseByPlus(fullTopic, lastIndex, response)
        // 反馈通配符#
        callResponseBySign(fullTopic, lastIndex, response)

    }

    /**
     * 查找关键字并且反馈响应结果
     */
    private fun findAndCallResponse(
        topic: String,
        response: Response,
        findQueue: (String) -> LinkedList<ReadyTask>
    ) {
        // 从集合中取得队列
        val queue = findQueue(topic)
        queue.forEach { task ->
            // 完全匹配的反馈
            val callback = task.back
            if (callback is ObservableBack) {
                callback.onResponse(task.listen as Observable, response)
            }
        }
    }

    /**
     * 通过主题来反馈响应结果
     * 原有主题，截取到最后一个/，并且加上「+」
     *
     * @param topic 主题
     * @param index 最后一位/的坐标，若为-1，则需要去匹配的是+，否则为topic/+
     * @param response 响应内容
     */
    private fun callResponseByPlus(
        topic: String,
        index: Int,
        response: Response
    ) {
        if (index == -1) {
            findAndCallResponse(PLUS, response, ::getQueueByTopic)
            return
        }

        findAndCallResponse("$topic/$PLUS", response, ::getQueueByTopic)
    }

    /**
     * 通过主题来反馈响应结果
     * 原有主题，截取到最后一个/，并且加上「#」
     *
     * @param topic 主题
     * @param index 最后一位/的坐标，若为-1，则需要去匹配的是#，否则为topic/#
     * @param response 响应内容
     * @param pollTaskById 根据ID获取任务的方法
     * @param callResponse 反馈响应结果的方法
     */
    private fun callResponseBySign(
        topic: String,
        index: Int,
        response: Response
    ) {
        if (index == -1) {
            findAndCallResponse(SIGN, response, ::getQueueByTopic)
            return
        }

        findAndCallResponse("$topic/$SIGN", response, ::getQueueByTopic)

        val lastIndex = topic.lastIndexOf(SLASH)
        val fullTopic = if (lastIndex == -1) {
            ""
        } else {
            topic.substring(0, lastIndex)
        }

        // 递归处理 反馈响应结果带通配符#
        callResponseBySign(fullTopic, lastIndex, response)
    }

    /**
     * 清除所有「主题集合」和「关键字集合」数据
     */
    fun clear() {
        topicCalls.clear()
        keywordCalls.clear()
    }

    companion object {
        // 斜杠
        private const val SLASH = "/"
        private const val PLUS = "+"
        private const val SIGN = "#"
    }
}