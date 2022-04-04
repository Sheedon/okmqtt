package org.sheedon.mqtt.internal.connection.responsibility

import org.sheedon.mqtt.internal.concurrent.CallbackEnum
import org.sheedon.mqtt.internal.connection.Plan
import org.sheedon.mqtt.internal.connection.RealObservable
import org.sheedon.mqtt.internal.connection.RealPlan
import org.sheedon.mqtt.internal.log

/**
 * 监听职责环节 plan
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/4 12:02 上午
 */
class ListenPlan(
    call: RealObservable,
    nextPlan: Plan?
) : RealPlan(call, nextPlan) {

    private var executed = false
    private var publishIds = ArrayList<Long>()

    override fun proceed() {
        check(call is RealObservable) { "call must RealObservable in ListenPlan" }

        if (call.originalRequest != null) {
            proceedRequest(call)
            return
        }

        if (call.originalSubscribe != null) {
            proceedSubscribe(call)
            return
        }

        if (call.isCanceled()) {
            unBind()
        }

    }


    private fun proceedRequest(call: RealObservable) {
        if (call.isCanceled()) {
            log.info("Dispatcher", "listenPlan to cancel proceed by ${call.originalRequest}")
            return
        }

        val request = call.originalRequest
        log.info("Dispatcher", "listenPlan to proceed by $request")


        val dispatcher = call.dispatcher
        val relation = request?.relation
        // 需要有绑定内容
        if (relation?.keyword.isNullOrEmpty() && relation?.topics?.topic.isNullOrEmpty()) {
            throw IllegalArgumentException(
                "request's relation bind topic or keyword is null by $request"
            )
        }

        // 添加绑定
        val callback = call.callback ?: return

        // 本地分发事件池中订阅分发的绑定行为
        val bindHandler = dispatcher.bindHandler()
        val ids = bindHandler.subscribe(call, callback, CallbackEnum.RETAIN)
        publishIds.addAll(ids)
        executed = true
    }


    private fun proceedSubscribe(call: RealObservable) {
        if (call.isCanceled()) {
            log.info("Dispatcher", "listenPlan to cancel proceed by ${call.originalSubscribe}")
            return
        }

        val subscribe = call.originalSubscribe
        log.info("Dispatcher", "listenPlan to proceed by $subscribe")
        val dispatcher = call.dispatcher
        val relations = subscribe?.relations

        // 核实需要有绑定内容
        if (relations.isNullOrEmpty()) {
            throw IllegalArgumentException(
                "request's relations is null by $subscribe"
            )
        }

        // 添加绑定
        val callback = call.callback ?: return

        // 本地分发事件池中订阅分发的绑定行为
        val bindHandler = dispatcher.bindHandler()
        val ids = bindHandler.subscribe(call, callback, CallbackEnum.RETAIN)
        publishIds.addAll(ids)
        executed = true
    }

    override fun cancel() {
        if (executed) {
            unBind()
        }
    }

    /**
     * 解除绑定
     */
    private fun unBind() {
        if (call !is RealObservable) {
            return
        }
        if (call.callback == null) {
            return
        }

        val dispatcher = call.dispatcher
        val bindHandler = dispatcher.bindHandler()
        bindHandler.unsubscribe(*publishIds.toLongArray())
    }

}