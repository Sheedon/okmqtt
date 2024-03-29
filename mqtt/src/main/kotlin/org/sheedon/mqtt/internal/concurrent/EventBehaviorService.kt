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
package org.sheedon.mqtt.internal.concurrent

import org.sheedon.mqtt.utils.Logger
import java.util.concurrent.*

/**
 * The event behavior service of mqtt puts the event into the thread pool for execution
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/31 1:35 下午
 */
class EventBehaviorService : EventBehavior {

    // Thread pool processing tasks submit feedback data,
    // which may be high concurrency, use cache thread pool to share a unified thread pool
    private var executorServiceOrNull: ExecutorService? = null

    @get:Synchronized
    @get:JvmName("executorService")
    val executorService: ExecutorService
        get() {
            if (executorServiceOrNull == null) {
                executorServiceOrNull = ThreadPoolExecutor(
                    0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
                    SynchronousQueue()
                ) { runnable ->
                    Thread(runnable, "MQTT Dispatcher").apply {
                        isDaemon = false
                    }
                }
            }
            return executorServiceOrNull!!
        }

    /**
     * Put the feedback event into the thread pool for execution
     *
     * @param requestRunnable Runnable Request task
     * @return Returns true, which means that it is currently being processed, and no other event executors need to operate it.
     */
    override fun enqueueRequestEvent(requestRunnable: Runnable): Boolean {
        Logger.info("enqueue requestRunnable to RequestEvent by MqttEventBehaviorService")
        executorService.execute(requestRunnable)
        return true
    }

    /**
     * Put feedback events into cachedThreadPool for concurrent execution
     *
     * @param callbackRunnable Runnable feedback task
     * @return Returns true, which means that it is currently being processed, and no other event executors need to operate it.
     */
    override fun enqueueCallbackEvent(callbackRunnable: Runnable): Boolean {
        Logger.info("enqueue callbackRunnable to CallbackEvent by MqttEventBehaviorService")
        executorService.execute(callbackRunnable)
        return true
    }
}