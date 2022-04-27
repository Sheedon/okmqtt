package org.sheedon.mqtt.internal.connection.responsibility

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe
import org.sheedon.mqtt.*
import org.sheedon.mqtt.internal.IDispatchManager
import org.sheedon.mqtt.internal.connection.*
import org.sheedon.mqtt.utils.Logger

/**
 * 取消订阅职责环节 plan
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/27 9:48 下午
 */
class SubscribePlan(
    observable: Listen,
    nextPlan: Plan?
) : RealPlan(observable, nextPlan), IMqttActionListener {

    // 是否完成订阅
    private var subscribed = false


    /**
     * 流程执行，按照call类型执行对应调度方法，
     * 若call是RealCall，则调度proceedRealCall
     * 若call是RealObservable，则调度proceedRealObservable
     * */
    override fun proceed() {
        if (call is RealCall) {
            proceedRealCall(call)
            return
        }

        if (call is RealObservable) {
            proceedRealObservable(call)
            return
        }

        throw IllegalAccessException("Not support by call:$call")
    }


    /**
     * 继续 真实的Call的调度行为
     *
     * @param call 真实回调
     */
    private fun proceedRealCall(call: RealCall) {
        if (call.isCanceled()) {
            Logger.info("Dispatcher", "subscribePlan to cancel proceed by ${call.originalRequest}")
            return
        }

        val request = call.originalRequest
        Logger.info("Dispatcher", "subscribePlan to proceed by $request")
        val relation = request.relation
        val dispatcher = call.dispatcher

        proceed(relation, dispatcher)
    }

    /**
     * 继续 真实的Observable的调度行为
     *
     * @param observable 真实观察者
     */
    private fun proceedRealObservable(observable: RealObservable) {
        // 以Request继续调度流程
        if (observable.originalRequest != null) {
            proceedRequest(observable)
            return
        }

        // 以Subscribe继续调度流程
        if (observable.originalSubscribe != null) {
            proceedSubscribe(observable)
            return
        }

        throw IllegalAccessException("Not support by observable:$observable")
    }

    /**
     * 执行 RealObservable 中 持有Request 流程
     */
    private fun proceedRequest(observable: RealObservable) {
        val request = observable.originalRequest!!
        if (observable.isCanceled()) {
            Logger.info(
                "Dispatcher",
                "subscribePlan to cancel proceed by $request"
            )
            return
        }

        Logger.info("Dispatcher", "subscribePlan to proceed by $request")
        val relation = request.relation
        val dispatcher = observable.dispatcher
        proceed(relation, dispatcher)
    }


    /**
     * 执行 RealObservable 中 持有Subscribe 流程
     */
    private fun proceedSubscribe(observable: RealObservable) {
        // subscribe 订阅数据
        val subscribe = observable.originalSubscribe
        if (observable.isCanceled()) {
            Logger.info(
                "Dispatcher",
                "subscribePlan to cancel proceed by $subscribe"
            )
            return
        }

        Logger.info("Dispatcher", "subscribePlan to proceed by $subscribe")
        val topicArray = subscribe?.run {
            Logger.info("Dispatcher", "subscribePlan to proceed by $this")
            // 关联者，得到主题集合
            relations
        }?.mapNotNull {
            if (it.topics?.headers?.subscriptionType == SubscriptionType.REMOTE) {
                it.topics
            } else {
                null
            }
        }

        if (topicArray.isNullOrEmpty()) {
            super.proceed()
            return
        }

        // 取消订阅
        val dispatcher = observable.dispatcher
        dispatcher.requestHandler().subscribe(*topicArray.toTypedArray())
    }


    /**
     * 根据「relation」和「dispatcher」调度流程
     *
     * @param relation 关联对象，若存在订阅主题，并且订阅类型为远程订阅，则执行订阅mqtt流程，否则执行下一个流程
     * @param dispatcher 调度管理者，执行请求行为
     * */
    private fun proceed(relation: Relation, dispatcher: IDispatchManager) {
        // 若不存在订阅主题，则直接调用下一个流程
        // 否则，执行订阅操作
        val topics = relation.topics
        if (topics?.topic.isNullOrEmpty()
            || topics?.headers?.subscriptionType == SubscriptionType.LOCAL
        ) {
            super.proceed()
            return
        }
        // 订阅主题
        dispatcher.requestHandler().subscribe(relation.topics!!, listener = this)
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

        // 回调成功订阅的反馈
        if (call is RealObservable) {
            val callback = call.back
            if (callback is SubscribeBack) {

                val (topicArray, qosArray) = call.originalSubscribe?.getTopicArray() ?: Pair(
                    arrayOf(),
                    intArrayOf()
                )

                callback.onResponse(MqttSubscribe(topicArray, qosArray))
            }

            // 无需结果反馈，则不需要执行下一步
            if (callback !is Callback && callback !is ObservableBack) {
                return
            }
        }

        // 若不需要取消，则执行下一步
        super.proceed()
    }

    /**
     * 订阅失败
     */
    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
        if (call is RealCall) {
            call.callback?.onFailure(exception)
        } else if (call is RealObservable) {
            call.back?.onFailure(exception)
        }
    }

    /**
     * 取消订阅
     * unSubscribeByRealCall：对 RealCall 取消订阅
     * unSubscribeByRealObservable：对 RealObservable 取消订阅
     */
    private fun unSubscribe() {
        if (call is RealCall) {
            unSubscribeByRealCall(call)
        } else if (call is RealObservable) {
            unSubscribeByRealObservable(call)
        }
    }

    /**
     * 取消订阅RealCall所发起的请求行为
     * */
    private fun unSubscribeByRealCall(call: RealCall) {
        val request = call.originalRequest
        Logger.info("Dispatcher", "subscribePlan to cancel by $request")
        val relation = request.relation
        val dispatcher = call.dispatcher
        dispatcher.requestHandler().unsubscribe(relation.topics!!)
    }

    /**
     * 取消订阅 RealObservable 所发起的请求行为
     *
     * @param observable 真实的观察者对象，将内部的request/subscribe取消订阅
     * */
    private fun unSubscribeByRealObservable(observable: RealObservable) {
        // request 取消订阅行为
        val request = observable.originalRequest
        if (request != null) {
            Logger.info("Dispatcher", "subscribePlan to cancel by $request")
            val relation = request.relation
            val dispatcher = observable.dispatcher
            dispatcher.requestHandler().unsubscribe(relation.topics!!)
            return
        }

        // subscribe 取消订阅行为
        val subscribe = observable.originalSubscribe
        subscribe?.run {
            Logger.info("Dispatcher", "subscribePlan to cancel by $this")
            // 关联者，得到主题集合
            relations
        }?.mapNotNull {
            it.topics
        }?.run {
            // 取消订阅
            val dispatcher = observable.dispatcher
            dispatcher.requestHandler().unsubscribe(*this.toTypedArray())
        }

    }
}