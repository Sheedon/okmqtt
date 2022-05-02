package org.sheedon.mqtt.internal.concurrent

import org.sheedon.mqtt.*
import org.sheedon.mqtt.internal.Contract.PLUS
import org.sheedon.mqtt.internal.Contract.ROOT_OBSERVER
import org.sheedon.mqtt.internal.Contract.SIGN
import org.sheedon.mqtt.internal.Contract.SLASH
import org.sheedon.mqtt.internal.connection.RealCall

/**
 * 请求的呼叫集合
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/28 11:05 下午
 */
internal class RequestCallArray {


    // 主题队列池，反馈主题为键，同样的反馈主题的内容，依次存入有序队列中
    private val topicCalls = LinkedHashMap<String, RequestNode>()

    // topic 任务锁
    private val topicLock = Any()

    /**
     * 1.将请求消息-以「主题/关键字」为键，由loadId()生产消息ID，存入topicCalls/keywordCalls中，
     * topicCalls/keywordCalls 为后续通过主题或绑定关键字查找消息得到[Dispatcher.readyCalls]中的Callback做连接关系
     *
     * 2.构建ReadyTask对象，反馈给[Dispatcher.readyCalls]
     *
     * @param call 呼叫对象，存储基本绑定信息，为反馈做关联
     * @param back 反馈对象，回调信息
     * @param loadId 加载一个消息ID的方法
     */
    fun subscribe(
        call: Call,
        back: IBack,
        loadId: () -> Long,
        offerReadyCalls: (Long, ReadyTask) -> Unit
    ): Long {
        // 获取消息ID
        val id = loadId()
        // 创建就绪的任务
        val readyTask = ReadyTask(call, id, CallbackEnum.SINGLE, back)

        val request = call.request()
        val relation = request.relation

        // 从Relation中获取mqtt主题
        val topic = getTopic(relation.topics?.topic)
        // 得到关键字
        val keyword = relation.keyword

        // 订阅主题和关键字
        subscribeTopicAndKeyword(topic, keyword, readyTask)

        offerReadyCalls(id, readyTask)
        return id
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
     * 取消订阅，根据传入的 关联项，核实该消息是 通过「主题/关键字」订阅，
     * 从对应 topicCalls / keywordCalls 中移除消息ID
     *
     * @param task 就绪的任务
     */
    fun unsubscribe(task: ReadyTask) {
        val listen = task.listen
        if (listen !is RealCall) {
            return
        }

        // 请求对象的关联信息
        val relation = listen.originalRequest.relation

        // 从Relation中获取mqtt主题
        val topic = getTopic(relation.topics?.topic)
        // 得到关键字
        val keyword = relation.keyword

        val node = getQueueByTopic(topic)
        node.poll(keyword, task)
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
        responseBody: ResponseBody,
        pollTaskById: (Long) -> ReadyTask?
    ) {

        val response = Response(keyword, responseBody)

        // 取得订阅主题
        val topic = getTopic(responseBody.topic)

        // 标准完全匹配的通配符查找并且反馈响应结果
        findAndCallResponse(topic, keyword, response, pollTaskById)

        // 订阅主题为空的关键字匹配
        if (topic != ROOT_OBSERVER) {
            findAndCallResponse(ROOT_OBSERVER, keyword, response, pollTaskById)
        }

        // 订阅信息带通配符的反馈
        val lastIndex = topic.lastIndexOf(SLASH)
        val fullTopic = if (lastIndex == -1) {
            ""
        } else {
            topic.substring(0, lastIndex)
        }

        // 反馈通配符+
        callResponseByPlus(fullTopic, lastIndex, keyword, response, pollTaskById)
        // 反馈通配符#
        callResponseBySign(fullTopic, lastIndex, keyword, response, pollTaskById)

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
        keyword: String?,
        response: Response,
        pollTaskById: (Long) -> ReadyTask?
    ) {
        if (index == -1) {
            findAndCallResponse(PLUS, keyword, response, pollTaskById)
            return
        }

        findAndCallResponse("$topic/$PLUS", keyword, response, pollTaskById)
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
        response: Response,
        pollTaskById: (Long) -> ReadyTask?
    ) {
        if (index == -1) {
            findAndCallResponse(SIGN, keyword, response, pollTaskById)
            return
        }

        findAndCallResponse("$topic/$SIGN", keyword, response, pollTaskById)

        val lastIndex = topic.lastIndexOf(SLASH)
        val fullTopic = if (lastIndex == -1) {
            ""
        } else {
            topic.substring(0, lastIndex)
        }

        // 递归处理 反馈响应结果带通配符#
        callResponseBySign(fullTopic, lastIndex, keyword, response, pollTaskById)
    }


    /**
     * 查找关键字并且反馈响应结果
     * 若该节点未订阅过，则节点为空，不需要反馈响应
     */
    private fun findAndCallResponse(
        topic: String,
        keyword: String?,
        response: Response,
        pollTaskById: (Long) -> ReadyTask?
    ) {
        // 找到主题所对应的请求节点
        val node = getNodeByTopic(topic)
        node?.callResponse(keyword, response, pollTaskById)
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
     * @return RequestNode 请求节点
     */
    private fun getQueueByTopic(topic: String): RequestNode {
        var queue = topicCalls[topic]
        if (queue == null) {
            synchronized(topicLock) {
                queue = topicCalls[topic]
                if (queue == null) {
                    queue = RequestNode()
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
     * @return RequestNode 请求节点
     */
    private fun getNodeByTopic(topic: String): RequestNode? {
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