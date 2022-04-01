package org.sheedon.mqtt.internal.concurrent

import org.sheedon.mqtt.ICallback
import org.sheedon.mqtt.Relation
import org.sheedon.mqtt.Request
import org.sheedon.mqtt.ResponseBody
import org.sheedon.mqtt.internal.IRelationBinder
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
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

    // 关键字队列，关键字为键,同样的反馈主题的内容，依次存入有序队列中
    private val keywordCalls = LinkedHashMap<String, LinkedList<ReadyTask>>()

    fun subscribe(request: IRelationBinder, callback: ICallback): Pair<Long, ReadyTask> {
        TODO("Not yet implemented")
    }

    fun unsubscribe(task: ReadyTask) {
        TODO("Not yet implemented")
    }

    fun clear() {
        TODO("Not yet implemented")
    }

    fun callResponse(keyword: String?, responseBody: ResponseBody) {
        TODO("Not yet implemented")
    }
}