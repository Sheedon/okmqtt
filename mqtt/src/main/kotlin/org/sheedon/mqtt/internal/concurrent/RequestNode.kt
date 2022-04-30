package org.sheedon.mqtt.internal.concurrent

import org.sheedon.mqtt.Call
import org.sheedon.mqtt.Callback
import org.sheedon.mqtt.Response
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * 请求对象所订阅的节点，用于记录一个mqtt订阅主题和其就绪任务池，
 * 以及该订阅主题下绑定的关键字和关键字的就绪任务池
 *
 * 订阅MQTT主题
 *  + 就绪任务池集合
 *  + 关键字集合
 *      + 单个关键字的就绪任务池
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/28 21:42
 */
class RequestNode {

    // 任务池
    private val taskPools = ConcurrentLinkedQueue<ReadyTask>()

    // 关键字队列，关键字为键,同样的反馈主题的内容，依次存入有序队列中
    private val keywordCalls = LinkedHashMap<String, ConcurrentLinkedQueue<ReadyTask>>()

    // keyword 任务锁
    private val keywordLock = Any()

    // 记录数据是否为空
    private val empty = AtomicInteger(0)


    /**
     * 提供一个就绪任务和关键字
     * 若关键字不为空，则将就绪任务添加到关键字的队列中，否则添加到任务池
     */
    fun offer(keyword: String?, readyTask: ReadyTask) {
        if (keyword?.isNotEmpty() == true) {
            offerKeyword(keyword, readyTask)
            return
        }

        taskPools.offer(readyTask)
        empty.incrementAndGet()
    }

    /**
     * 提供一个就绪任务，添加到关键字的任务池中
     */
    private fun offerKeyword(keyword: String, readyTask: ReadyTask) {
        val queue = getQueueByKeyword(keyword)
        queue.offer(readyTask)
        empty.incrementAndGet()
    }

    /**
     * 推出一个就绪任务
     * 若keyword为空，则从taskPools推出，
     * 反之从keywordCalls按指定keyword推出一个就绪任务
     *
     * @param keyword 订阅关键字
     */
    fun poll(keyword: String?, task: ReadyTask?) {
        // 当前empty为0，则说明内部无数据，不需要推出动作
        if (empty.get() == 0) {
            return
        }

        if (keyword?.isNotEmpty() == true) {
            pollKeyword(keyword, task)
            return
        }

        taskPools.remove(task)
        empty.decrementAndGet()
    }

    /**
     * 按指定的keyword，从keywordCalls按指定keyword推出一个就绪任务
     * @param keyword 关键字
     */
    private fun pollKeyword(keyword: String, task: ReadyTask?) {
        val queue = getQueueByKeyword(keyword)
        queue.remove(task)
        empty.decrementAndGet()
    }

    /**
     * 根据关键字获取 keywordCalls 的队列，双重加锁，若队列存在直接返回，不存在则创建后返回
     *
     * @param keyword 关键字
     * @return keywordCalls 队列
     */
    private fun getQueueByKeyword(keyword: String): ConcurrentLinkedQueue<ReadyTask> {
        var queue = keywordCalls[keyword]
        if (queue == null) {
            synchronized(keywordLock) {
                queue = keywordCalls[keyword]
                if (queue == null) {
                    queue = ConcurrentLinkedQueue()
                    keywordCalls[keyword] = queue!!
                }
            }
        }
        return queue!!
    }

    /**
     * 呼叫响应结果
     * 若keyword为空，则取taskPools中的任务遍历响应，否则根据关键字反馈响应结果
     */
    fun callResponse(
        keyword: String?,
        response: Response,
        pollTaskById: (Long) -> ReadyTask?
    ) {
        if (keyword?.isNotEmpty() == true) {
            callResponseByKeyword(keyword, response, pollTaskById)
            return
        }

        // 推出就绪任务
        var task = taskPools.poll() ?: return
        // 推出超时的当前ID的就绪任务
        task = pollTaskById(task.id) ?: return

        val callback = task.back
        if (callback is Callback) {
            callback.onResponse(task.listen as Call, response)
        }
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
        keyword: String,
        response: Response,
        pollTaskById: (Long) -> ReadyTask?
    ) {
        // 查找关键字并且反馈响应结果
        val queue = getQueueByKeyword(keyword)
        // 推出就绪任务
        var task = queue.poll() ?: return
        // 推出超时的当前ID的就绪任务
        task = pollTaskById(task.id) ?: return

        val callback = task.back
        if (callback is Callback) {
            callback.onResponse(task.listen as Call, response)
        }
    }
}