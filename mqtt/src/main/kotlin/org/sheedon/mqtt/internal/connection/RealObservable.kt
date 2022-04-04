/*
 * Copyright (C) 2022 Sheedon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sheedon.mqtt.internal.connection

import org.sheedon.mqtt.*
import org.sheedon.mqtt.internal.IDispatchManager
import org.sheedon.mqtt.internal.connection.responsibility.ListenPlan
import org.sheedon.mqtt.internal.connection.responsibility.SubscribePlan
import org.sheedon.mqtt.internal.connection.responsibility.UnSubscribePlan
import org.sheedon.mqtt.internal.log
import org.sheedon.rr.core.NamedRunnable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real Observer wrapper class for dispatching locally constructed subscribes
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 8:31 下午
 */
class RealObservable private constructor(
    val dispatcher: IDispatchManager,
    val originalRequest: Request? = null,
    val originalSubscribe: Subscribe? = null
) : Observable {

    // 请求责任链
    private var planChain: Plan? = null
    private val executed = AtomicBoolean()

    // 执行是否为订阅,true:订阅，false:取消订阅
    private var isSubscribe = false

    @Volatile
    private var canceled = false


    var back: IBack? = null


    /**
     * 订阅一个主题，监听响应结果的反馈
     *
     * @param callback 结果响应，[callback.onFailure] 调度失败的结果反馈，
     * [callback.onResponse] 消息正常响应
     */
    override fun enqueue(callback: Callback) {
        isSubscribe = true
        enqueueReal(callback)
    }

    /**
     * 订阅一个主题，监听订阅结果
     *
     * @param callback 结果响应，[callback.onFailure] 订阅失败的结果反馈，
     * [callback.onResponse] 订阅成功反馈
     */
    override fun enqueue(callback: SubscribeBack?) {
        isSubscribe = true
        enqueueReal(callback)
    }

    /**
     * 订阅一个主题，监听订阅结果
     *
     * @param callback 结果响应，[callback.onFailure] 订阅/调度失败的结果反馈，
     * [callback.onResponse(response: MqttWireMessage?)] 订阅成功反馈
     * [callback.onResponse(request: Request, response: Response)] 消息正常响应
     */
    override fun enqueue(callback: FullCallback) {
        isSubscribe = true
        enqueueReal(callback)
    }

    /**
     * 订阅一个主题
     * 存在4种callback内容
     *
     * 1. 单一监听订阅是否成功
     * 2. 单一监听响应结果
     * 3. 监听订阅结果+响应结果
     * 4. callback == null，只是订阅，无需反馈
     */
    private fun enqueueReal(back: IBack?) {
        check(executed.compareAndSet(false, true))

        this.back = back
        this.dispatcher.enqueueRequest(AsyncObservable(back))
    }

    /**
     * 取消订阅一个主题，监听取消订阅结果
     *
     * @param callback 结果响应，[callback.onFailure] 取消订阅失败的结果反馈，
     * [callback.onResponse] 取消订阅成功反馈
     */
    override fun unsubscribe(callback: SubscribeBack?) {
        isSubscribe = false
        enqueueReal(callback)
    }

    /**
     * 请求订阅对象
     * 单条订阅
     * */
    override fun request(): Request {
        return originalRequest ?: Request(Relation.Builder().build(), null)
    }

    /**
     * 订阅对象
     * 多条订阅
     * */
    override fun subscribe(): Subscribe {
        return originalSubscribe ?: Subscribe(arrayOf())
    }

    override fun isCanceled(): Boolean {
        return canceled
    }

    /**
     * 取消动作
     * 如果是订阅操作，则取消订阅
     * 但是取消订阅，不操作
     */
    override fun cancel() {
        canceled = true
        // 未开始处理则结束，否则尝试停止请求调度
        if (!isExecuted()) {
            log.info(
                "Dispatcher",
                "The body not started by ${originalRequest ?: originalSubscribe}"
            )
            return
        }

        log.info(
            "Dispatcher",
            "The body cancel by ${originalRequest ?: originalSubscribe}"
        )


        planChain?.cancel()
    }

    override fun isExecuted(): Boolean {
        return executed.get()
    }

    internal inner class AsyncObservable(
        private val responseBack: IBack?
    ) : NamedRunnable("AsyncCall %s", originalRequest) {

        val request: Request?
            get() = originalRequest

        val subscribe: Subscribe?
            get() = originalSubscribe

        val call: RealObservable
            get() = this@RealObservable

        override fun execute() {
            if (isCanceled()) {
                log.warning(
                    "Dispatcher",
                    "request is canceled by $originalRequest"
                )
                return
            }

            val isNeedCallback = responseBack != null

            // 监听请求的流程
            val listenPlan = ListenPlan(this@RealObservable, null)

            planChain = if (isNeedCallback) {
                if (isSubscribe) {
                    SubscribePlan(this@RealObservable, listenPlan)
                } else {
                    UnSubscribePlan(this@RealObservable, listenPlan)
                }
            } else {
                listenPlan
            }

            // 流程调度
            planChain?.proceed()

        }
    }


    companion object {

        /**
         * 构建一个真实的观察者对象
         *
         * @param dispatcher 订阅调度者
         * @param originalRequest 真实的请求
         * @return Observable 观察执行职责
         * */
        fun newRealObservable(
            dispatcher: IDispatchManager,
            originalRequest: Request
        ): Observable {
            return RealObservable(dispatcher, originalRequest)
        }


        /**
         * 构建一个真实的观察者对象
         *
         * @param dispatcher 订阅调度者
         * @param originalSubscribe 真实订阅对象
         * @return Observable 观察执行职责
         * */
        fun newRealObservable(
            dispatcher: IDispatchManager,
            originalSubscribe: Subscribe
        ): Observable {
            return RealObservable(dispatcher, originalSubscribe = originalSubscribe)
        }
    }
}