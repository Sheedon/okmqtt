package org.sheedon.mqtt.internal.concurrent

import com.github.yitter.contract.IIdGenerator
import com.github.yitter.contract.IdGeneratorOptions
import com.github.yitter.idgen.DefaultIdGenerator
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.sheedon.mqtt.*
import org.sheedon.mqtt.internal.DataConverter
import org.sheedon.mqtt.internal.IRelationBinder
import org.sheedon.mqtt.internal.binder.IBindHandler
import org.sheedon.mqtt.internal.binder.IResponseHandler
import org.sheedon.mqtt.internal.log
import org.sheedon.rr.timeout.DelayEvent.Companion.build
import org.sheedon.rr.timeout.OnTimeOutListener
import org.sheedon.rr.timeout.android.TimeOutHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException


/**
 * 事件处理池，用于管理未完成的请求响应记录
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/28 9:47 下午
 */
internal class Dispatcher(
    private val convertArray: List<DataConverter<ResponseBody, String>>,
    private val defaultTimeout: Long // 默认超时时间
) : IBindHandler, IResponseHandler {

    // 以请求ID为键，以请求任务为值的请求数据池
    private val readyCalls = ConcurrentHashMap<Long, ReadyTask>()

    // 请求队列池，反馈主题/关键字为键，同样的反馈主题的内容，依次存入有序队列中
    private val requestCalls = RequestCallArray()

    // 请求队列池，反馈主题/关键字为键，同样的反馈主题的内容，依次存入有序队列中
    private val observerCalls = ObserverCallArray()

    // 雪花飘移算法配置
    private val idGenerator: IIdGenerator by lazy {
        val options = IdGeneratorOptions(1).apply { WorkerIdBitLength = 10 }
        DefaultIdGenerator(options)
    }


    // 超时执行者
    private val timeoutHandler: TimeOutHandler<Long> = TimeOutHandler<Long>()
        .apply {
            setListener(TimeOutListener())
        }


    /**
     * 订阅一个主题/关键字，通过传递请求对象/反馈呼叫对象/呼叫类型，转换成「准备就绪任务」，存入 readyCalls 中，
     * 并且将作为readyCalls键的ID返回，用于后续取消订阅的动作
     *
     * @param request 请求或监听对象，其一为了响应时反馈请求或订阅信息，其二用于提取关键字，超时时间间隔
     * @param callback 呼叫对象，用于反馈结果
     * @param type 呼叫类型，单次订阅/保留订阅
     */
    override fun subscribe(
        request: IRelationBinder,
        callback: ICallback,
        type: CallbackEnum
    ): Pair<Long, Long> {

        log.error("Dispatcher", "subscribe to request: $request and type: $type")

        val (id, task) = when (type) {
            // 单次订阅
            CallbackEnum.SINGLE -> requestCalls.subscribe(request, callback, ::loadId)
            // 保留订阅
            CallbackEnum.RETAIN -> observerCalls.subscribe(request, callback)
        }

        // 以请求ID为键，以请求任务为值的请求数据池
        readyCalls[id] = task
        val relation = request.getRelation()

        // 计算超时时长，若请求中未填，则采用默认超时时长
        val timeout = relation.timeout.let {
            if (it == null || it == 0L) {
                return@let defaultTimeout
            }
            return@let it
        }

        // 追加超时事件
        if (type == CallbackEnum.SINGLE) {
            appendTimeoutEvent(timeout, id)
        }

        // 将ID返回到调度者，以做后续取消订阅动作
        return Pair(id, timeout)
    }

    /**
     * 追加一个超时事件到「超时处理器」中，由「超时处理器」在到达超时期限后，返回到当前执行超时反馈
     *
     * @param timeout 超时时长
     * @param id 消息ID
     */
    private fun appendTimeoutEvent(timeout: Long, id: Long) {
        val event = build(id, timeout + System.currentTimeMillis())
        log.error("Dispatcher", "addBinder to addEvent success")
        timeoutHandler.addEvent(event)
    }

    /**
     * 根据一个消息ID，取消订阅一个就绪的任务
     *
     * @param id 消息ID
     */
    override fun unsubscribe(id: Long) {
        val readyTask = readyCalls.remove(id) ?: return

        val request = readyTask.request
        if (readyTask.type == CallbackEnum.SINGLE) {
            // 取消超时事件
            timeoutHandler.removeEvent(id)
            // 取消单次订阅
            requestCalls.unsubscribe(request.getRelation())
        } else if (readyTask.type == CallbackEnum.RETAIN) {
            // 取消保留订阅
            observerCalls.unsubscribe(readyTask)
        }
    }

    /**
     * 反馈mqtt结果
     */
    override fun callResponse(topic: String, message: MqttMessage) {
        val responseBody = ResponseBody(topic, message)
        val keyword = responseBody.run {
            convertArray.forEach {
                val backKeyword = it.convert(this)
                if (backKeyword != null) {
                    return@run backKeyword
                }
            }
            null
        }

        // 请求的反馈执行
        requestCalls.callResponse(keyword, responseBody, ::pollReadyCallsById, ::callResponse)
        // 订阅消息的通知
        observerCalls.callResponse(keyword, responseBody)
    }

    /**
     * 根据消息ID 移除一个就绪的呼叫任务
     *
     * @param id 消息ID
     */
    private fun pollReadyCallsById(id: Long): ReadyTask? {
        return readyCalls.remove(id)
    }

    /**
     * 获取消息ID
     */
    private fun loadId(): Long {
        return idGenerator.newLong()
    }

    /**
     * 反馈响应结果，分别核实callback的实现类，并且执行反馈操作
     *
     * @param callback ICallback
     * @param response 响应结果
     */
    private fun callResponse(callback: ICallback?, binder: IRelationBinder, response: Response) {
        if (callback == null) return

        // 反馈为Callback类型
        if (callback is Callback) {
            callResponse(callback, binder, response)
        }
    }

    /**
     * 响应反馈结果
     *
     * @param callback 反馈
     */
    private fun callResponse(callback: Callback, binder: IRelationBinder, response: Response) {
        // 响应对象为Request对象
        if (binder is Request) {
            callback.onResponse(binder, response)
        }
    }

    /**
     * 超时监听器
     */
    private inner class TimeOutListener : OnTimeOutListener<Long> {
        override fun onTimeOut(id: Long, e: TimeoutException?) {
            val readyTask = readyCalls.remove(id) ?: return

            log.info("Dispatcher", "onTimeout task($readyTask)")

            // 移除请求订阅内容
            requestCalls.unsubscribe(readyTask.request.getRelation())
            // 反馈超时消息
            readyTask.callback?.onFailure(e)
        }
    }

    /**
     * 取消全部任务事件
     */
    internal fun clearAll() {
        readyCalls.clear()
        requestCalls.clear()
        observerCalls.clear()
    }

    /**
     * 销毁动作
     * 销毁超时执行器
     */
    internal fun destroy() {
        clearAll()
        timeoutHandler.destroy()
    }

}