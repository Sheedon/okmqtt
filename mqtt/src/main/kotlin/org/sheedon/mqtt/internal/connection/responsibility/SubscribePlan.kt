package org.sheedon.mqtt.internal.connection.responsibility

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.sheedon.mqtt.Request
import org.sheedon.mqtt.internal.IDispatchManager
import org.sheedon.mqtt.internal.connection.Plan
import org.sheedon.mqtt.internal.connection.RealCall
import org.sheedon.mqtt.internal.connection.RealPlan
import org.sheedon.mqtt.internal.log

/**
 * 订阅职责环节 plan
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/27 9:48 下午
 */
class SubscribePlan(
    call: RealCall,
    nextPlan: Plan?,
    request: Request,
    dispatcher: IDispatchManager
) : RealPlan(call, nextPlan, request, dispatcher), IMqttActionListener {

    // 是否完成订阅
    private var subscribed = false


    override fun proceed() {
        if (call.isCanceled()) {
            log.info("Dispatcher", "subscribePlan to cancel proceed by $request")
            return
        }
        log.info("Dispatcher", "subscribePlan to proceed by $request")
        val relation = request.relation

        // 若不存在订阅主题，则直接调用下一个流程
        // 否则，执行订阅操作
        if (relation.subscribe?.topic.isNullOrEmpty()) {
            super.proceed()
            return
        }

        // 订阅主题
        dispatcher.requestHandler().subscribe(relation.subscribe!!, this)
    }

    /**
     * 取消订阅
     */
    override fun cancel() {
        if (subscribed) {
            unSubscribe()
        }
        super.cancel()
    }

    /**
     * 订阅成功
     */
    override fun onSuccess(asyncActionToken: IMqttToken?) {
        subscribed = true
        if (call.isCanceled()) {
            unSubscribe()
            return
        }
        // 若不需要取消，则执行下一步
        super.proceed()
    }

    /**
     * 订阅失败
     */
    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        call.callback?.onFailure(exception)
    }

    private fun unSubscribe() {
        log.info("Dispatcher", "subscribePlan to cancel by $request")
        val relation = request.relation
        dispatcher.requestHandler().unsubscribe(relation.subscribe!!)
    }

}