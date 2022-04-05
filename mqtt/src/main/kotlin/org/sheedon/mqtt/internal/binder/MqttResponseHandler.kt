package org.sheedon.mqtt.internal.binder

import org.eclipse.paho.client.mqttv3.MqttMessage
import org.sheedon.mqtt.Dispatcher
import org.sheedon.mqtt.internal.concurrent.EventBehavior

/**
 * mqtt响应执行者
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/5 2:27 下午
 */
internal class MqttResponseHandler internal constructor(
    private val dispatcher: Dispatcher,
    private val eventBehavior: EventBehavior
) : IResponseHandler {


    /**
     * 异步执行回调响应结果
     *
     * @param topic mqtt主题
     * @param message mqtt消息
     */
    override fun callResponse(topic: String, message: MqttMessage) {
        eventBehavior.enqueueCallbackEvent {
            dispatcher.callResponse(topic, message)
        }
    }
}