package org.sheedon.mqtt

import org.sheedon.mqtt.utils.Logger
import org.sheedon.rr.core.EventBehavior
import java.util.concurrent.Executors

/**
 * The event behavior service of mqtt puts the event into the thread pool for execution
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/31 1:35 下午
 */
class MqttEventBehaviorService : EventBehavior {

    // Thread pool processing tasks submit feedback data,
    // which may be high concurrency, use cache thread pool to share a unified thread pool
    private val service = Executors.newCachedThreadPool()

    /**
     * Put the feedback event into the thread pool for execution
     *
     * @param requestRunnable Runnable Request task
     * @return Returns true, which means that it is currently being processed, and no other event executors need to operate it.
     */
    override fun enqueueRequestEvent(requestRunnable: Runnable): Boolean {
        Logger.info("enqueue requestRunnable to RequestEvent by MqttEventBehaviorService")
        service.execute(requestRunnable)
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
        service.execute(callbackRunnable)
        return true
    }
}