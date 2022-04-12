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
package org.sheedon.mqtt

import org.sheedon.mqtt.internal.DataConverter
import org.sheedon.mqtt.internal.IDispatchManager
import org.sheedon.mqtt.internal.binder.*
import org.sheedon.mqtt.internal.binder.MqttRequestHandler
import org.sheedon.mqtt.internal.binder.MqttResponseHandler
import org.sheedon.mqtt.internal.concurrent.EventBehavior
import org.sheedon.mqtt.internal.concurrent.EventBehaviorService
import org.sheedon.mqtt.internal.connection.RealCall
import org.sheedon.mqtt.internal.connection.RealObservable
import org.sheedon.rr.timeout.OnTimeOutListener
import org.sheedon.rr.timeout.TimeoutManager
import org.sheedon.rr.timeout.android.TimeOutHandler
import java.util.*
import kotlin.collections.ArrayList

/**
 * 旨在于将「请求/订阅」事件与「响应」事件关联，从而达到将mqtt消息按需分发的目的。
 * 其中，「请求事件」和「订阅事件」，以是否需要提交数据做关联而定。
 *
 * 请求事件，分为两种类型：其一，提交一个Mqtt-Message，并且需要对这条消息配置的「关联主题」做反馈监听；
 * 其二，只提交一个Mqtt-Message，无需监听反馈消息。
 *
 * 订阅事件，分为两种类型：其一，如「请求事件」一致，对一个mqtt「关联主题」做反馈监听；其二，监听一组「关联主题」，注意响应消息建议一致。
 *
 * 响应事件，也分为两种类型：其一，订阅一个mqtt-topic，对某个实现匹配的mqtt主题实现订阅；其二，订阅一个mqtt-keyword，
 * 将mqtt响应消息按「关键字转换器」得到关键字，再匹配并反馈到指定的反馈事件上。
 * mqtt-keyword在[Builder.keywordConverters]中配置，格式根据实际开发需求决定。
 * 例如：关键字为反馈主题按「/」分割后，最后一个数据，如AA/BB/CC/test，得到关键字为test，订阅/请求中存在关联关键字为test，则对该消息执行反馈。
 *
 * 内部配置项包括
 * mqtt消息格式：charsetName。
 * mqtt统一配置请求超时时长：timeout，只对于需反馈的「请求事件」有效。
 * 行为执行的环境，也就是运行的线程池：behaviorService，默认已实现。
 * 超时处理执行者：timeoutManager，默认已实现。
 * 关键字转换器集合：keywordConverters，按需配置。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 12:58 下午
 */
class MqttRRBinderClient constructor(
    builder: Builder
) : IDispatchManager {

    /**
     * character set name
     */
    private val charsetName: String = builder.charsetName

    /**
     * default timeout method
     */
    internal val timeout: Int = builder.timeout

    /**
     * default dispatcher
     */
    private val dispatcher: Dispatcher = builder.dispatcher!!

    /**
     * Responsibilities Service Execution Environment
     */
    private val behaviorService: EventBehavior = builder.behaviorService

    /**
     * Responsibilities Service Execution Environment
     */
    private val timeoutManager: TimeoutManager<Long> = builder.timeoutManager

    /**
     * default request handler
     */
    private val requestHandler: IRequestHandler = builder.requestHandler!!

    /**
     * default bind handler
     */
    private val bindHandler: IBindHandler = builder.bindHandler!!

    /**
     * default response handler
     */
    internal val responseHandler: IResponseHandler = builder.responseHandler!!

    /**
     * default keyword converter
     */
    private val keywordConverters: ArrayList<DataConverter<ResponseBody, String>> =
        builder.keywordConverters


    /**
     * load request handler
     * */
    override fun requestHandler(): IRequestHandler = requestHandler

    /**
     * load event behavior, Acts on asynchronous execution of tasks
     * */
    override fun eventBehavior(): EventBehavior = behaviorService

    /**
     * load request handler
     * If there are no additional settings, it is dispatcher
     * */
    override fun bindHandler(): IBindHandler = bindHandler

    /**
     * Create a call for a request-response
     *
     * @param request request object
     * @return Call The action used to perform the enqueue submit request
     */
    internal fun newCall(request: Request): Call {
        return RealCall(this, request)
    }

    /**
     * An observer Observable that creates information
     * Single topic subscription
     *
     * @param request request object
     * @return Observable Subscribe to a topic and listen for messages from that topic
     */
    internal fun newObservable(request: Request): Observable {
        return RealObservable.newObservable(this, request)
    }

    /**
     * An observer Observable that creates information
     * Multi-topic, subscription to the same result
     *
     * @param subscribe subscribe object
     * @return Observable Subscribe to a topic and listen for messages from that topic
     */
    internal fun newObservable(subscribe: Subscribe): Observable {
        return RealObservable.newObservable(this, subscribe)
    }

    constructor() : this(Builder())

    class Builder internal constructor() {

        internal var charsetName: String = Charsets.UTF_8.displayName()
        internal var dispatcher: Dispatcher? = null
        internal var timeout = 5
        internal var timeoutManager: TimeoutManager<Long> = TimeOutHandler()
        internal var behaviorService: EventBehavior = EventBehaviorService()
        internal var requestHandler: IRequestHandler? = null
        internal var bindHandler: IBindHandler? = null
        internal var responseHandler: IResponseHandler? = null
        internal var keywordConverters: ArrayList<DataConverter<ResponseBody, String>> =
            ArrayList()


        internal constructor(rrClient: MqttRRBinderClient) : this() {
            this.charsetName = rrClient.charsetName
            this.dispatcher = rrClient.dispatcher
            this.timeout = rrClient.timeout
            this.behaviorService = rrClient.behaviorService
            this.timeoutManager = rrClient.timeoutManager
            this.requestHandler = rrClient.requestHandler
            this.bindHandler = rrClient.bindHandler
            this.responseHandler = rrClient.responseHandler
            this.keywordConverters = rrClient.keywordConverters
        }


        /**
         * Unified sets the MQTTMessage's payload encoding type.
         * If [RequestBody.autoEncode] is true and [RequestBody.charset] is null,
         * set MQTTMessage's payload encoding type by this value.
         * The default charsetName is `UTF-8`.
         *
         * @param charsetName Character set encoding type
         */
        fun charsetName(charsetName: String) = apply {
            this.charsetName = charsetName
        }

        /**
         * Sets the request timeout value.
         * This value, measured in seconds,defines the maximum time interval
         * the request will wait for the network callback to the MQTT Message response to be established.
         * The default timeout is 5 seconds.
         *
         * @param timeout the timeout value, measured in seconds. It must be &gt;0
         */
        fun messageTimeout(timeout: Int) = apply {
            if (timeout <= 0) return this
            this.timeout = timeout
        }


        /**
         * Sets operating environment of the request and the response to use.
         * Use [behaviorService] to provide Call and Observable with a thread pool for execution.
         * The default behaviorService is [EventBehaviorService]
         *
         * @param behaviorService the execution service environment to use
         */
        fun behaviorService(behaviorService: EventBehavior) = apply {
            this.behaviorService = behaviorService
        }

        /**
         * Sets timeout event manager.
         * Use [timeoutManager] to hold and execute timeout events by timeout time, when the timeout condition is reached,
         * execute the callback of the event with the help of [OnTimeOutListener].
         * Use [TimeoutManager.removeEvent] to cancel and remove timeout events with message ID.
         * The default timeoutManager is [TimeOutHandler].
         *
         * @param timeoutManager timeout event manager
         */
        fun timeoutManager(timeoutManager: TimeoutManager<Long>) = apply {
            this.timeoutManager = timeoutManager
        }

        /**
         * Sets MQTT event handler.
         * Use [requestHandler] to execute publish MQTT Message,subscribe and unsubscribe MQTT Topic.
         * The default requestHandler is [MqttRequestHandler], it agent [MqttWrapperClient] executes
         * the events of publish MQTT Message,subscribe and unsubscribe MQTT Topic.
         *
         * @param requestHandler MQTT event request handler
         */
        fun requestHandler(requestHandler: IRequestHandler) = apply {
            this.requestHandler = requestHandler
        }

        /**
         * Sets event bind handler to bind MQTT response and [request MQTT or subscribe MQTT Topic].
         * Use [bindHandler] to associate RealCall or RealObservable with IBack, after getting the message,
         * feedback through IBack, and also support to remove the binding relationship through the message ID.
         *
         * @param bindHandler request and response bind handler
         */
        fun bindHandler(bindHandler: IBindHandler) = apply {
            this.bindHandler = bindHandler
        }

        /**
         * Sets MQTT Message response handler.
         * Use [responseHandler] callResponse MQTT topic and MQTT Message.
         * The default responseHandler is [MqttResponseHandler],it agent [Dispatcher] executes
         * the events of callResponse.
         *
         * @param responseHandler response handler
         */
        fun responseHandler(responseHandler: IResponseHandler) = apply {
            this.responseHandler = responseHandler
        }

        /**
         * Sets keyword converter collection.
         * Used to convert topics or messages into corresponding keywords
         * to match distribution messages in real business.
         * The default value is empty collection.
         *
         * @param keywordConverters Keyword converter Collection
         */
        fun keywordConverters(keywordConverters: List<DataConverter<ResponseBody, String>>) =
            apply {
                this.keywordConverters.clear()
                this.keywordConverters.addAll(keywordConverters)
            }

        /**
         * Sets keyword converter
         * Used to convert topics or messages into corresponding keywords
         * to match distribution messages in real business.
         *
         * @param keywordConverters keyword converter cannot null
         */
        fun keywordConverter(keywordConverter: DataConverter<ResponseBody, String>) =
            apply {
                this.keywordConverters.add(keywordConverter)
            }


        fun build(): MqttRRBinderClient {

            // sets default request wrapper handler
            if (requestHandler == null) {
                requestHandler = MqttRequestHandler(charsetName)
            }

            // create dispatcher
            if (dispatcher == null) {
                dispatcher = Dispatcher(keywordConverters, timeout * 1000L)
            }

            // dispatcher is bindHandler
            if (bindHandler == null) {
                bindHandler = dispatcher
            }

            // sets default response wrapper handler
            if (responseHandler == null) {
                responseHandler = MqttResponseHandler(
                    dispatcher!!,
                    behaviorService
                )
            }

            return MqttRRBinderClient(this)
        }

    }
}