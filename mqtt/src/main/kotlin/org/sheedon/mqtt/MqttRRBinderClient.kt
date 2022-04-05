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
import org.sheedon.rr.timeout.TimeoutManager
import org.sheedon.rr.timeout.android.TimeOutHandler
import java.util.*
import kotlin.collections.ArrayList

/**
 * RequestResponseBinder Decorated client class for request-response binding
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 12:58 下午
 */
class MqttRRBinderClient constructor(
    builder: Builder
) : IDispatchManager {

    /**
     * default character set name
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

    class Builder() {
        // Character set encoding type
        internal var charsetName: String = "GBK"

        // 绑定执行调度者
        internal var dispatcher: Dispatcher? = null

        // 默认超时事件
        internal var timeout = 5

        // 职责服务执行环境
        @JvmField
        internal var behaviorService: EventBehavior = EventBehaviorService()

        // 超时处理者
        @JvmField
        internal var timeoutManager: TimeoutManager<Long> = TimeOutHandler()

        // 请求调度者
        @JvmField
        internal var requestHandler: IRequestHandler? = null

        // 绑定调度者
        @JvmField
        internal var bindHandler: IBindHandler? = null

        // 响应调度者
        @JvmField
        internal var responseHandler: IResponseHandler? = null

        // 关键字转换器
        @JvmField
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
         * Set the character set encoding type and convert it to a string of the specified format when receiving data
         *
         * @param charsetName Character set encoding type
         * @return Builder builder
         */
        fun charsetName(charsetName: String) = apply {
            this.charsetName = charsetName
        }

        /**
         * Set the information request timeout time (seconds)
         *
         * @param timeout default timeout
         */
        fun messageTimeout(timeout: Int) = apply {
            if (timeout < 0) return this
            this.timeout = timeout
        }

        /**
         * Set the behavior service environment
         *
         * @param behaviorService execution service environment
         */
        fun behaviorService(behaviorService: EventBehavior) = apply {
            this.behaviorService = behaviorService
        }

        /**
         * set timeout manager
         *
         * @param timeoutManager timeout event manager
         */
        fun timeoutManager(timeoutManager: TimeoutManager<Long>) = apply {
            this.timeoutManager = timeoutManager
        }

        /**
         * set request handler
         *
         * @param requestHandler mqtt request handler
         */
        fun requestHandler(requestHandler: IRequestHandler) = apply {
            this.requestHandler = requestHandler
        }

        /**
         * set bind handler
         *
         * @param requestHandler request and response bind handler
         */
        fun bindHandler(bindHandler: IBindHandler) = apply {
            this.bindHandler = bindHandler
        }

        /**
         * set mqtt response handler
         *
         * @param requestHandler response bind handler
         */
        fun responseHandler(responseHandler: IResponseHandler) = apply {
            this.responseHandler = responseHandler
        }

        /**
         * set keyword converter list
         *
         * @param keywordConverters Keyword converter Collection
         */
        fun keywordConverters(keywordConverters: List<DataConverter<ResponseBody, String>>) =
            apply {
                this.keywordConverters.clear()
                this.keywordConverters.addAll(keywordConverters)
            }

        /**
         * set keyword converter
         *
         * @param keywordConverters Keyword converter
         */
        fun keywordConverter(keywordConverter: DataConverter<ResponseBody, String>) =
            apply {
                this.keywordConverters.add(keywordConverter)
            }


        fun build(): MqttRRBinderClient {

            if (requestHandler == null) {
                requestHandler = MqttRequestHandler(charsetName)
            }

            if (dispatcher == null) {
                dispatcher = Dispatcher(keywordConverters, timeout * 1000L)
            }

            if (bindHandler == null) {
                bindHandler = dispatcher
            }

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