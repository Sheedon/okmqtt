package org.sheedon.mqtt.internal.concurrent

import org.sheedon.mqtt.*
import org.sheedon.mqtt.internal.Contract.PLUS
import org.sheedon.mqtt.internal.Contract.ROOT_OBSERVER
import org.sheedon.mqtt.internal.Contract.SIGN
import org.sheedon.mqtt.internal.Contract.SLASH
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
    private val topicCalls = LinkedHashMap<String, ObserverNode>()

    // topic 任务锁
    private val topicLock = Any()

    /**
     * 订阅一个主题（Request所包含），或者订阅一个主题组（Subscribe所包含），并将订阅的消息，
     * 借由offerReadyCalls，添加到就绪任务后，返回订阅的所有消息的ID
     *
     * 1.核实 observable 持有的为Request/Subscribe
     * 若为 Request ，执行单项执行订阅
     * 若为 Subscribe ，执行多项执行订阅
     *
     * 2.以「主题/关键字」为键，由loadId()生产消息ID，存入topicCalls/存入topicCalls.keywordCalls中
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

        // 从observable中取得订阅的关联内容
        val request = observable.request()
        val relation = request.relation

        // 执行真实的订阅方法
        subscribeRealTask(relation, observable, back, id, offerReadyCalls)
        return id
    }

    /**
     * 订阅一个主题组（Subscribe所包含），并将订阅的消息，
     * 借由offerReadyCalls，添加到就绪任务后，返回订阅的所有消息的ID
     *
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

        // 得到响应所需关联的主题集合
        val relations = subscribe.relations
        relations.forEach {
            // 获取消息ID
            val id = loadId()
            subscribeRealTask(it, observable, back, loadId(), offerReadyCalls)
            ids.add(id)
        }

        return ids
    }

    /**
     * 执行真实的订阅任务行为,将传入的observable、id、back 构建出ReadyTask
     * 拿到relation中的topic和keyword做订阅
     * 最终让dispatcher将id和readyTask绑定
     * */
    private fun subscribeRealTask(
        relation: Relation,
        observable: RealObservable,
        back: IBack,
        id: Long,
        offerReadyCalls: (Long, ReadyTask) -> Unit
    ) {
        // 创建就绪的任务
        val readyTask = ReadyTask(observable, id, CallbackEnum.RETAIN, back)

        // 从Relation中获取mqtt订阅主题
        val topic = getTopic(relation.topics?.topic)
        // 得到关键字
        val keyword = relation.keyword
        // 订阅主题和关键字
        subscribeTopicAndKeyword(topic, keyword, readyTask)

        // 执行dispatcher.offerReadyCalls，将id和readyTask绑定到readyCalls
        offerReadyCalls(id, readyTask)
    }

    /**
     * 订阅主题和关键字，将订阅主题为键，并且构造出订阅节点对象，将订阅内容和keyword所绑定内容存入其中
     *
     * @param topic 主题
     * @param keyword 关键字
     * @param readyTask 就绪的任务
     */
    private fun subscribeTopicAndKeyword(topic: String, keyword: String?, readyTask: ReadyTask) {
        val node = getQueueByTopic(topic)
        node.offer(keyword, readyTask)
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
            unsubscribeRequest(request, task)
            return
        }

        // 取消订阅「订阅对象」中所包含的订阅内容
        val subscribe = listen.originalSubscribe
        if (subscribe != null) {
            unsubscribeSubscribe(subscribe, task)
        }
    }

    /**
     * 取消订阅一个主题 Request所包含
     *
     * 核实 request 请求数据对象
     * */
    private fun unsubscribeRequest(request: Request, task: ReadyTask) {
        // 请求对象的关联信息
        val relation = request.relation
        pollByRelation(relation, task)
    }

    /**
     * 取消订阅一个主题 Subscribe所包含的订阅信息
     *
     * 核实 request 请求数据对象
     * */
    private fun unsubscribeSubscribe(subscribe: Subscribe, task: ReadyTask) {
        // 请求对象的关联信息
        subscribe.relations.forEach {
            pollByRelation(it, task)
        }
    }

    /**
     * 从relation中推出一组订阅消息
     */
    private fun pollByRelation(relation: Relation, task: ReadyTask) {
        // 从Relation中获取mqtt主题
        val topic = getTopic(relation.topics?.topic)
        // 得到关键字
        val keyword = relation.keyword

        val node = getQueueByTopic(topic)
        node.poll(keyword, task)
    }

    /**
     * 反馈响应结果
     * 从responseBody取出topic，根据该topic，反馈响应结果。
     * 若keyword 不为空，则对该keyword下的就绪任务反馈响应结果。
     * 随后对带通配符的主题依次响应
     * 依次从指定的topic，反馈
     *
     * @param keyword 关键字
     * @param responseBody 响应内容
     */
    fun callResponse(
        keyword: String?,
        responseBody: ResponseBody
    ) {
        // 构建响应对象
        val response = Response(keyword, responseBody)

        // 取得关键字
        val topic = getTopic(responseBody.topic)

        // 标准完全匹配的主题查找并且反馈响应结果
        findAndCallResponse(topic, keyword, response)

        // 订阅主题为空的关键字匹配
        if (topic != ROOT_OBSERVER) {
            findAndCallResponse(ROOT_OBSERVER, keyword, response)
        }

        // 订阅信息带通配符的反馈
        val lastIndex = topic.lastIndexOf(SLASH)
        val fullTopic = if (lastIndex == -1) {
            ""
        } else {
            topic.substring(0, lastIndex)
        }

        // 反馈通配符+
        callResponseByPlus(fullTopic, lastIndex, keyword, response)
        // 反馈通配符#
        callResponseBySign(fullTopic, lastIndex, keyword, response)
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
        keyword: String?,
        response: Response
    ) {
        if (index == -1) {
            findAndCallResponse(PLUS, keyword, response)
            return
        }

        findAndCallResponse("$topic/$PLUS", keyword, response)
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
        keyword: String?,
        response: Response
    ) {
        if (index == -1) {
            findAndCallResponse(SIGN, keyword, response)
            return
        }

        findAndCallResponse("$topic/$SIGN", keyword, response)

        val lastIndex = topic.lastIndexOf(SLASH)
        val fullTopic = if (lastIndex == -1) {
            ""
        } else {
            topic.substring(0, lastIndex)
        }

        // 递归处理 反馈响应结果带通配符#
        callResponseBySign(fullTopic, lastIndex, keyword, response)
    }

    /**
     * 查找主题并且反馈响应结果
     */
    private fun findAndCallResponse(
        topic: String,
        keyword: String?,
        response: Response
    ) {
        // 找到主题所对应的观察节点
        val node = getNodeByTopic(topic)
        node?.callResponse(keyword, response)
    }

    /**
     * 传入一个主题，若主题不存在，则取[ROOT_OBSERVER]作为主题返回
     * @param topic mqtt订阅主题
     */
    private fun getTopic(topic: String?): String {
        return topic ?: ROOT_OBSERVER
    }

    /**
     * 根据主题获取keywordCalls的队列，双重加锁，若队列存在直接返回，不存在则创建后返回
     *
     * @param topic 主题
     * @return topicCalls 队列
     */
    private fun getQueueByTopic(topic: String): ObserverNode {
        var queue = topicCalls[topic]
        if (queue == null) {
            synchronized(topicLock) {
                queue = topicCalls[topic]
                if (queue == null) {
                    queue = ObserverNode()
                    topicCalls[topic] = queue!!
                }
            }
        }
        return queue!!
    }

    /**
     * 根据主题获取keywordCalls的队列，双重加锁，若队列存在直接返回，不存在返回null
     *
     * @param topic 主题
     * @return topicCalls 队列
     */
    private fun getNodeByTopic(topic: String): ObserverNode? {
        val queue = topicCalls[topic]
            ?: synchronized(topicLock) {
                return topicCalls[topic]
            }
        return queue
    }

    /**
     * 清除所有「主题集合」和「关键字集合」数据
     */
    fun clear() {
        topicCalls.clear()
    }
}