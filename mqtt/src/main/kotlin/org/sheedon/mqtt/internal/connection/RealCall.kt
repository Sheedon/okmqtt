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
import org.sheedon.mqtt.internal.concurrent.NamedRunnable
import org.sheedon.mqtt.internal.connection.responsibility.PublishPlan
import org.sheedon.mqtt.internal.connection.responsibility.SubscribePlan
import org.sheedon.mqtt.internal.log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real Call wrapper class for scheduling locally built enqueues
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 8:15 下午
 */
class RealCall(
    val dispatcher: IDispatchManager,
    val originalRequest: Request
) : Call {

    // 请求责任链
    private var planChain: Plan? = null
    private val executed = AtomicBoolean()

    @Volatile
    private var canceled = false

    internal var callback: Callback? = null

    override fun enqueue(callback: Callback?) {
        check(executed.compareAndSet(false, true))

        this.callback = callback

        val eventBehavior = this.dispatcher.eventBehavior()
        eventBehavior.enqueueRequestEvent(AsyncCall(callback))
    }

    override fun publish() {
        enqueue(null)
    }

    override fun request(): Request {
        return originalRequest
    }

    override fun isCanceled(): Boolean {
        return canceled
    }

    override fun cancel() {
        canceled = true
        // 未开始处理则结束，否则尝试停止请求调度
        if (!isExecuted()) {
            log.info(
                "Dispatcher",
                "The request not started by $originalRequest"
            )
            return
        }

        log.info(
            "Dispatcher",
            "The request cancel by $originalRequest"
        )

        planChain?.cancel()
    }

    override fun isExecuted(): Boolean {
        return executed.get()
    }

    internal inner class AsyncCall(
        private val responseCallback: Callback?
    ) : NamedRunnable("AsyncCall %s", originalRequest) {

        val request: Request
            get() = originalRequest

        val call: RealCall
            get() = this@RealCall

        override fun execute() {
            if (isCanceled()) {
                log.warning(
                    "Dispatcher",
                    "request is canceled by $originalRequest"
                )
                return
            }

            val isNeedCallback = responseCallback != null

            // 提交请求的流程
            val publishPlan = PublishPlan(this@RealCall, null)

            planChain = if (isNeedCallback) {
                SubscribePlan(this@RealCall, publishPlan)
            } else {
                publishPlan
            }

            // 流程调度
            planChain?.proceed()
        }

    }
}