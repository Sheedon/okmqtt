package org.sheedon.mqtt

import com.github.yitter.contract.IIdGenerator
import com.github.yitter.contract.IdGeneratorOptions
import com.github.yitter.idgen.DefaultIdGenerator
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.sheedon.mqtt.*
import org.sheedon.mqtt.internal.DataConverter
import org.sheedon.mqtt.internal.binder.IBindHandler
import org.sheedon.mqtt.internal.concurrent.CallbackEnum
import org.sheedon.mqtt.internal.concurrent.ObserverCallArray
import org.sheedon.mqtt.internal.concurrent.ReadyTask
import org.sheedon.mqtt.internal.concurrent.RequestCallArray
import org.sheedon.mqtt.internal.connection.RealCall
import org.sheedon.mqtt.internal.connection.RealObservable
import org.sheedon.mqtt.utils.Logger
import org.sheedon.rr.timeout.DelayEvent.Companion.build
import org.sheedon.rr.timeout.OnTimeOutListener
import org.sheedon.rr.timeout.android.TimeOutHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException


/**
 * MQTT request-response bound dispatcher, that policy on manage ready requests.
 *
 * 在调度者中，主要维持ID-Map、请求存储资源、订阅存储资源 和 超时触发器。
 *
 * ID-Map：所有的请求和订阅任务，统一会产生一个id-key作为键，将就绪任务作为值，存入到map中[readyCalls]，
 * 以便于在取消/超时触发时，根据ID得到目标任务，从而将task由[requestCalls]或[observerCalls]中移除。
 *
 * requestCalls：作为请求响应模式下所记录的任务池，单一针对于反馈某一个请求所关联的响应消息，而且只反馈一次（未取消情况下，
 * 超时反馈，或者响应结果反馈）。因此，该对象提供三个方法:
 * [RequestCallArray.subscribe]，订阅一个响应主题，topic/keyword/topic+keyword的组合形式，将任务添加到队列尾部。
 * [RequestCallArray.unsubscribe]，取消订阅一个响应主题，根据传入的 关联项，核实该消息是 通过「主题/关键字」取消队列中目标订阅。
 * [RequestCallArray.callResponse]，响应结果，根据传入的 关联项，核实该消息是 通过「主题/关键字」提取队列首个订阅，并且反馈这个响应。
 *
 * observerCalls：作为订阅模式下所记录的任务池，在未执行取消的前提下，将永久保存在队列中。提供的方法如[requestCalls]一致，
 * [ObserverCallArray.subscribe], [ObserverCallArray.unsubscribe], [ObserverCallArray.callResponse]
 * 其中[ObserverCallArray.callResponse]，将反馈所有符合条件的订阅信息。
 *
 * timeoutHandler：超时触发器，登记并且执行超时事件，在超时的时间点回调记录的事件所包含的任务ID，dispatcher将对超时的任务ID，
 * 从readyCalls中找到对应的任务，并且向requestCalls执行取消订阅的行为。
 *
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/28 9:47 下午
 */
internal class Dispatcher(
    private val convertArray: ArrayList<DataConverter<ResponseBody, String>>,
    private val defaultTimeout: Long // 默认超时时间
) : IBindHandler {

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
     * @param back 呼叫对象，用于反馈结果
     * @param type 呼叫类型，单次订阅/保留订阅
     */
    override fun subscribe(
        call: RealCall,
        back: IBack
    ): Pair<Long, Long> {

        Logger.info("Dispatcher", "subscribe to call: $call and type: ${CallbackEnum.SINGLE}")

        val request = call.originalRequest
        // 记录并且得到内部消息ID
        val id = requestCalls.subscribe(
            call, back, ::loadId,
            ::offerReadyCalls
        )

        val relation = request.relation

        // 计算超时时长，若请求中未填，则采用默认超时时长
        val timeout = relation.timeout.let {
            if (it == null || it == 0L) {
                return@let defaultTimeout
            }
            return@let it
        }

        // 追加超时事件
        appendTimeoutEvent(timeout, id)

        // 将ID返回到调度者，以做后续取消订阅动作
        // timeout 超时时长
        return Pair(id, timeout)
    }

    /**
     * 订阅一个主题/关键字，通过传递请求对象/反馈呼叫对象/呼叫类型，转换成「准备就绪任务」，存入 readyCalls 中，
     * 并且将作为readyCalls键的ID返回，用于后续取消订阅的动作
     *
     * @param observable 请求或监听对象，其一为了响应时反馈请求或订阅信息，其二用于提取关键字，超时时间间隔
     * @param back 呼叫对象，用于反馈结果
     */
    override fun subscribe(observable: RealObservable, back: IBack): List<Long> {
        Logger.info(
            "Dispatcher",
            "subscribe to call: $observable and type: ${CallbackEnum.RETAIN}"
        )

        // 将ID返回到调度者，以做后续取消订阅动作
        return observerCalls.subscribe(
            observable,
            back,
            ::loadId,
            ::offerReadyCalls
        )
    }

    /**
     * 追加一个超时事件到「超时处理器」中，由「超时处理器」在到达超时期限后，返回到当前执行超时反馈
     *
     * @param timeout 超时时长
     * @param id 消息ID
     */
    private fun appendTimeoutEvent(timeout: Long, id: Long) {
        val event = build(id, timeout + System.currentTimeMillis())
        Logger.info("Dispatcher", "addBinder to addEvent success")
        timeoutHandler.addEvent(event)
    }

    /**
     * 根据一个消息ID组，取消订阅一个就绪的任务
     *
     * @param id 消息ID组
     */
    override fun unsubscribe(vararg id: Long) {
        id.forEach {
            val readyTask = readyCalls.remove(it) ?: return@forEach

            if (readyTask.type == CallbackEnum.SINGLE) {
                // 取消超时事件
                timeoutHandler.removeEvent(it)
                // 取消单次订阅
                requestCalls.unsubscribe(readyTask)
            } else if (readyTask.type == CallbackEnum.RETAIN) {
                // 取消保留订阅
                observerCalls.unsubscribe(readyTask)
            }
        }
    }

    /**
     * 反馈mqtt结果
     */
    fun callResponse(topic: String, message: MqttMessage) {
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
        requestCalls.callResponse(keyword, responseBody, ::pollReadyCallsById)
        // 订阅消息的通知
        observerCalls.callResponse(keyword, responseBody)
    }

    /**
     * 根据消息ID 添加一个就绪的呼叫任务
     *
     * @param id 消息ID
     */
    private fun offerReadyCalls(id: Long, task: ReadyTask) {
        readyCalls[id] = task
    }

    /**
     * 根据消息ID 移除一个就绪的呼叫任务
     *
     * @param id 消息ID
     */
    private fun pollReadyCallsById(id: Long): ReadyTask? {
        // 同时移除超时任务
        timeoutHandler.removeEvent(id)
        return readyCalls.remove(id)
    }

    /**
     * 获取消息ID
     */
    private fun loadId(): Long {
        return idGenerator.newLong()
    }

    /**
     * 超时监听器
     */
    private inner class TimeOutListener : OnTimeOutListener<Long> {
        override fun onTimeOut(id: Long, e: TimeoutException?) {
            val readyTask = readyCalls.remove(id) ?: return

            Logger.info("Dispatcher", "onTimeout task($readyTask)")

            // 移除请求订阅内容
            requestCalls.unsubscribe(readyTask)
            // 反馈超时消息
            readyTask.back?.onFailure(e)
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