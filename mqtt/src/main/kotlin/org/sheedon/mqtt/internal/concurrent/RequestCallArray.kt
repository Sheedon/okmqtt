package org.sheedon.mqtt.internal.concurrent

import org.sheedon.mqtt.*
import org.sheedon.mqtt.internal.IRelationBinder
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 请求的呼叫集合
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/28 11:05 下午
 */
internal class RequestCallArray {


    // 主题队列池，反馈主题为键，同样的反馈主题的内容，依次存入有序队列中
    private val topicCalls = LinkedHashMap<String, ConcurrentLinkedQueue<Long>>()

    // topic 任务锁
    private val topicLock = Any()

    // 关键字队列，关键字为键,同样的反馈主题的内容，依次存入有序队列中
    private val keywordCalls = LinkedHashMap<String, ConcurrentLinkedQueue<Long>>()

    // keyword 任务锁
    private val keywordLock = Any()

    /**
     * 1.将请求消息-以「主题/关键字」为键，由loadId()生产消息ID，存入topicCalls/keywordCalls中，
     * topicCalls/keywordCalls 为后续通过主题或绑定关键字查找消息得到[Dispatcher.readyCalls]中的Callback做连接关系
     *
     * 2.构建ReadyTask对象，反馈给[Dispatcher.readyCalls]
     *
     * @param request 请求对象，存储基本绑定信息，为反馈做关联
     * @param callback 反馈对象，回调信息
     * @param loadId 加载一个消息ID的方法
     */
    fun subscribe(
        request: IRelationBinder,
        callback: ICallback,
        loadId: () -> Long
    ): Pair<Long, ReadyTask> {
        // 获取消息ID
        val id = loadId()
        // 创建就绪的任务
        val readyTask = ReadyTask(request, id, CallbackEnum.SINGLE, callback)
        val relation = request.getRelation()

        // 关键字不存在，则说明必然是订阅主题消息
        val keyword = relation.keyword
        if (keyword.isNullOrEmpty()) {

            val subscribe = relation.subscribe
            check(subscribe != null) { "please add subscribe by $relation" }

            val queue = getQueueByTopic(subscribe.topic)
            queue.offer(id)
            return Pair(id, readyTask)
        }

        val queue = getQueueByKeyword(keyword)
        queue.offer(id)
        return Pair(id, readyTask)
    }

    /**
     * 根据主题获取keywordCalls的队列，双重加锁，若队列存在直接返回，不存在则创建后返回
     *
     * @param topic 主题
     * @return topicCalls 队列
     */
    private fun getQueueByTopic(topic: String): ConcurrentLinkedQueue<Long> {
        return getQueue(topic, topicCalls, topicLock)
    }

    /**
     * 根据关键字获取 keywordCalls 的队列，双重加锁，若队列存在直接返回，不存在则创建后返回
     *
     * @param keyword 关键字
     * @return keywordCalls 队列
     */
    private fun getQueueByKeyword(keyword: String): ConcurrentLinkedQueue<Long> {
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
        targetCalls: LinkedHashMap<String, ConcurrentLinkedQueue<Long>>,
        lock: Any
    ): ConcurrentLinkedQueue<Long> {
        var queue = targetCalls[key]
        if (queue == null) {
            synchronized(lock) {
                queue = targetCalls[key]
                if (queue == null) {
                    queue = ConcurrentLinkedQueue()
                    targetCalls[key] = queue!!
                }
            }
        }
        return queue!!
    }

    /**
     * 按照匹配条件反馈响应结果，有一下三种情况
     * 1.关键字得到匹配
     * 2.订阅主题得到匹配
     * 3.订阅主题 通配符匹配「+」or「#」
     *
     * @param keyword 关键字
     * @param responseBody 响应内容
     * @param pollTaskById 根据ID获取任务的方法
     * @param callResponse 反馈响应结果的方法
     */
    fun callResponse(
        keyword: String?,
        responseBody: ResponseBody,
        pollTaskById: (Long) -> ReadyTask?,
        callResponse: (ICallback?, IRelationBinder, Response) -> Unit
    ) {

        val response = Response(keyword, responseBody)

        // 响应关键字的反馈
        callResponseByKeyword(keyword, response, pollTaskById, callResponse)

        // 响应主题的反馈
        callResponseByTopic(responseBody.topic, response, pollTaskById, callResponse)
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
     * @param pollTaskById 根据ID获取任务的方法
     * @param callResponse 反馈响应结果的方法
     */
    private fun callResponseByKeyword(
        keyword: String?,
        response: Response,
        pollTaskById: (Long) -> ReadyTask?,
        callResponse: (ICallback?, IRelationBinder, Response) -> Unit
    ) {
        // 关键字为空，则无需接下来操作
        if (keyword.isNullOrEmpty()) {
            return
        }

        // 查找关键字并且反馈响应结果
        findAndCallResponse(keyword, response, pollTaskById, callResponse, ::getQueueByKeyword)
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
     * @param pollTaskById 根据ID获取任务的方法
     * @param callResponse 反馈响应结果的方法
     */
    private fun callResponseByTopic(
        topic: String?,
        response: Response,
        pollTaskById: (Long) -> ReadyTask?,
        callResponse: (ICallback?, IRelationBinder, Response) -> Unit
    ) {
        // 关键字为空，则无需接下来操作
        if (topic.isNullOrEmpty()) {
            return
        }

        // 标准完全匹配的通配符查找并且反馈响应结果
        findAndCallResponse(topic, response, pollTaskById, callResponse, ::getQueueByTopic)

        // 订阅信息带通配符的反馈
        val lastIndex = topic.lastIndexOf(SLASH)
        val fullTopic = if (lastIndex == -1) {
            ""
        } else {
            topic.substring(0, lastIndex)
        }

        // 反馈通配符+
        callResponseByPlus(fullTopic, lastIndex, response, pollTaskById, callResponse)
        // 反馈通配符#
        callResponseBySign(fullTopic, lastIndex, response, pollTaskById, callResponse)

    }

    /**
     * 通过主题来反馈响应结果
     * 原有主题，截取到最后一个/，并且加上「+」
     *
     * @param topic 主题
     * @param index 最后一位/的坐标，若为-1，则需要去匹配的是+，否则为topic/+
     * @param response 响应内容
     * @param pollTaskById 根据ID获取任务的方法
     * @param callResponse 反馈响应结果的方法
     */
    private fun callResponseByPlus(
        topic: String,
        index: Int,
        response: Response,
        pollTaskById: (Long) -> ReadyTask?,
        callResponse: (ICallback?, IRelationBinder, Response) -> Unit
    ) {
        if (index == -1) {
            findAndCallResponse(PLUS, response, pollTaskById, callResponse, ::getQueueByTopic)
            return
        }

        findAndCallResponse("$topic/$PLUS", response, pollTaskById, callResponse, ::getQueueByTopic)
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
        response: Response,
        pollTaskById: (Long) -> ReadyTask?,
        callResponse: (ICallback?, IRelationBinder, Response) -> Unit
    ) {
        if (index == -1) {
            findAndCallResponse(SIGN, response, pollTaskById, callResponse, ::getQueueByTopic)
            return
        }

        findAndCallResponse("$topic/$SIGN", response, pollTaskById, callResponse, ::getQueueByTopic)

        val lastIndex = topic.lastIndexOf(SLASH)
        val fullTopic = if (lastIndex == -1) {
            ""
        } else {
            topic.substring(0, lastIndex)
        }

        // 递归处理 反馈响应结果带通配符#
        callResponseBySign(fullTopic, lastIndex, response, pollTaskById, callResponse)
    }


    /**
     * 查找关键字并且反馈响应结果
     *
     */
    private fun findAndCallResponse(
        topic: String,
        response: Response,
        pollTaskById: (Long) -> ReadyTask?,
        callResponse: (ICallback?, IRelationBinder, Response) -> Unit,
        findQueue: (String) -> ConcurrentLinkedQueue<Long>
    ) {
        // 从集合中取得队列
        val queue = findQueue(topic)
        // 队列中推出第一项ID
        val id = queue.poll() ?: return
        // 推出当前ID的就绪任务
        val task = pollTaskById(id) ?: return

        // 完全匹配的反馈
        callResponse(task.callback, task.request, response)
    }

    /**
     * 取消订阅，根据传入的 关联项，核实该消息是 通过「主题/关键字」订阅，
     * 从对应 topicCalls / keywordCalls 中移除消息ID
     *
     * @param relation 关联项
     */
    fun unsubscribe(relation: Relation) {
        val keyword = relation.keyword
        // 关键字不存在，则说明必然是订阅主题消息
        if (keyword.isNullOrEmpty()) {

            val subscribe = relation.subscribe
            check(subscribe != null) { "please add subscribe by $relation" }

            val queue = getQueueByTopic(subscribe.topic)
            queue.poll()
            return
        }
        val queue = getQueueByKeyword(keyword)
        queue.poll()
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