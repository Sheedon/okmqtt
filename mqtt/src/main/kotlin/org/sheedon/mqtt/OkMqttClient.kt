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

import android.content.Context
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.sheedon.mqtt.internal.DataConverter
import org.sheedon.mqtt.internal.binder.IBindHandler
import org.sheedon.mqtt.internal.binder.IRequestHandler
import org.sheedon.mqtt.internal.binder.IResponseHandler
import org.sheedon.mqtt.internal.binder.MqttRequestHandler
import org.sheedon.mqtt.internal.concurrent.EventBehavior
import org.sheedon.mqtt.internal.log
import org.sheedon.mqtt.listener.*
import org.sheedon.mqtt.template.ILogger
import org.sheedon.mqtt.utils.Logger
import org.sheedon.rr.timeout.TimeoutManager

/**
 * Mqtt 门面客户端类，包含「请求响应客户端」和「mqtt调度客户端」
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 10:40 上午
 */
class OkMqttClient internal constructor(
    builder: Builder
) : MqttFactory {

    private val mqttRRBinderClient: MqttRRBinderClient = builder.mqttRRBinderClient!!
    private val mqttClient: MqttWrapperClient = builder.mqttClient!!

    constructor() : this(Builder())

    init {
        val requestHandler = mqttRRBinderClient.requestHandler()
        if (requestHandler is MqttRequestHandler) {
            requestHandler.attachClient(mqttClient)
        }
    }

    /**
     * 默认超时时间单位（秒）
     */
    fun getDefaultTimeout(): Int {
        return mqttRRBinderClient.timeout
    }

    /**
     * 创建请求响应的Call
     *
     * @param request 请求对象
     * @return Call 用于执行入队/提交请求的动作
     */
    override fun newCall(request: Request): Call {
        return mqttRRBinderClient.newCall(request)
    }

    /**
     * 创建信息的观察者 Listener
     *
     * @param request 请求对象
     * @return Listener 订阅某个主题，监听该主题的消息
     */
    override fun newObservable(request: Request): Observable {
        return mqttRRBinderClient.newObservable(request)
    }

    /**
     * 创建信息的观察者 Listener
     *
     * @param subscribe 订阅对象
     * @return Listener 订阅某个主题，监听该主题的消息
     */
    override fun newObservable(subscribe: Subscribe): Observable {
        return mqttRRBinderClient.newObservable(subscribe)
    }

    /**
     * 重连操作
     */
    @JvmOverloads
    fun reConnect(
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.reConnect(listener)
    }

    @JvmOverloads
    fun disConnect(listener: IMqttActionListener? = null) {
        this.mqttClient.disConnect(listener)
    }


    /**
     * 订阅mqtt主题
     * @param body mqtt消息体
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun subscribe(
        body: Topics,
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.subscribe(body, listener)
    }

    /**
     * 订阅mqtt主题
     * @param bodies mqtt消息体集合
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun subscribe(
        bodies: List<Topics>,
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.subscribe(bodies, listener)
    }

    /**
     * 取消订阅mqtt主题
     * @param body mqtt消息体
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun unsubscribe(
        body: Topics,
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.unsubscribe(body, listener)
    }

    /**
     * 取消订阅mqtt主题
     * @param bodies mqtt消息体集合
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun unsubscribe(
        bodies: List<Topics>,
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.unsubscribe(bodies, listener)
    }

    /**
     * 重新订阅mqtt主题
     *
     * @param bodies mqtt消息体集合
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun reSubscribe(
        bodies: List<Topics>,
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.reSubscribe(bodies, listener)
    }

    /**
     * 发送mqtt消息
     *
     * @param topic 主题
     * @param message mqtt消息内容
     */
    fun publish(topic: String, message: MqttMessage): IMqttDeliveryToken {
        return mqttClient.publish(topic, message)
    }


    class Builder {
        internal var mqttRRBinderClient: MqttRRBinderClient? = null
        internal var mqttClient: MqttWrapperClient? = null

        internal val mqttRRBinderBuilder: MqttRRBinderClient.Builder = MqttRRBinderClient.Builder()
        internal val mqttBuilder: MqttWrapperClient.Builder = MqttWrapperClient.Builder()

        /**
         * 设置 mqtt 请求响应绑定客户端
         * @param mqttRRBinderClient 请求响应绑定客户端
         */
        fun mqttRRBinderClient(mqttRRBinderClient: MqttRRBinderClient) = apply {
            this.mqttRRBinderClient = mqttRRBinderClient
        }

        /**
         * 设置 mqtt调度客户端
         * @param mqttClient mqtt调度客户端
         */
        fun mqttClient(mqttClient: MqttWrapperClient) = apply {
            this.mqttClient = mqttClient
        }

        /**
         * 设置字符集编码类型，在接收数据时转化为指定格式的字符串
         *
         * @param charsetName 字符集编码类型
         * @return Builder 构建者
         */
        fun charsetName(charsetName: String) = apply {
            this.mqttRRBinderBuilder.charsetName(charsetName)
        }

        /**
         * 设置信息请求超时时间（单位秒）
         *
         * @param timeout 超时时间
         * @return Builder<BackTopic></BackTopic>, ID> 构建者
         */
        fun messageTimeout(timeout: Int) = apply {
            if (timeout < 0) return this
            this.mqttRRBinderBuilder.messageTimeout(timeout)
        }

        /**
         * 设置行为线程池，后加的靠前
         *
         * @param behaviorService 执行服务环境
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun behaviorService(behaviorService: EventBehavior) = apply {
            this.mqttRRBinderBuilder.behaviorService(behaviorService)
        }

        /**
         * 设置超时处理者
         *
         * @param timeoutManager 事件管理
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun timeoutManager(timeoutManager: TimeoutManager<Long>) = apply {
            this.mqttRRBinderBuilder.timeoutManager(timeoutManager)
        }

        /**
         * 设置请求调度者
         *
         * @param requestHandler 请求调度者
         */
        fun requestAdapter(requestHandler: IRequestHandler) = apply {
            this.mqttRRBinderBuilder.requestHandler(requestHandler)
        }

        /**
         * 设置绑定调度者
         *
         * @param requestHandler 请求和响应绑定处理程序
         */
        fun bindHandler(bindHandler: IBindHandler) = apply {
            this.mqttRRBinderBuilder.bindHandler(bindHandler)
        }

        /**
         * 设置 mqtt 响应处理程序
         *
         * @param requestHandler 响应绑定处理程序
         */
        fun responseHandler(responseHandler: IResponseHandler) = apply {
            this.mqttRRBinderBuilder.responseHandler(responseHandler)
        }

        /**
         * 设置反馈关键字转换器集合
         *
         * @param keywordConverters 反馈主题转换器集合
         */
        fun keywordConverters(keywordConverters: List<DataConverter<ResponseBody, String>>) =
            apply {
                this.mqttRRBinderBuilder.keywordConverters(keywordConverters)
            }

        /**
         * 设置反馈关键字转换器
         *
         * @param backTopicConverter 反馈主题转换器
         */
        fun keywordConverter(keywordConverter: DataConverter<ResponseBody, String>) =
            apply {
                this.mqttRRBinderBuilder.keywordConverter(keywordConverter)
            }

        /**
         * Enables an android application to communicate with an MQTT server using non-blocking methods.
         *
         * @param androidClient MqttAndroidClient
         * @return Builder
         */
        fun androidClient(androidClient: MqttAndroidClient) = apply {
            this.mqttBuilder.androidClient(androidClient)
        }

        /**
         * Constructor- create an MqttAndroidClient that can be used to communicate
         * with an MQTT server on android
         *
         * @param context     used to pass context to the callback.
         * @param serverURI   specifies the protocol, host name and port to be used to
         * connect to an MQTT server
         * @param clientId    specifies the name by which this connection should be
         * identified to the server
         * @param persistence the persistence class to use to store in-flight message. If
         * null then the default persistence mechanism is used
         * @param ackType     how the application wishes to acknowledge a message has been
         * processed.
         * @return Builder
         */
        @JvmOverloads
        fun clientInfo(
            context: Context,
            serverURI: String,
            clientId: String?,
            persistence: MqttClientPersistence? = null,
            ackType: MqttAndroidClient.Ack? = MqttAndroidClient.Ack.AUTO_ACK
        ) = apply {
            this.mqttBuilder.clientInfo(context, serverURI, clientId, persistence, ackType)
        }

        /**
         * set of connection parameters that override the defaults
         *
         * @param options  connection parameters
         * @param <Option> MqttConnectOptions
         * @return Builder
        </Option> */
        fun <Option : MqttConnectOptions> connectOptions(options: Option) = apply {
            this.mqttBuilder.connectOptions(options)
        }

        /**
         * Sets the DisconnectedBufferOptions for this client
         *
         * @param bufferOpts the DisconnectedBufferOptions
         * @param <Option>   DisconnectedBufferOptions
         * @return Builder
         */
        fun <Option : DisconnectedBufferOptions> disconnectedBufferOptions(bufferOpts: Option) =
            apply {
                this.mqttBuilder.disconnectedBufferOptions(bufferOpts)
            }

        /**
         * Sets the MessageListener for this client
         *
         * @param callback the MessageListener
         * @return Builder
         */
        fun callback(callback: IMessageListener) = apply {
            this.mqttBuilder.callback(callback)
        }

        /**
         * Sets the MessageListener for this client
         *
         * @param connectListener the connect action listener
         * @return Builder
         */
        @JvmOverloads
        fun connectListener(
            connectListener: IActionListener? = null
        ) = apply {
            this.mqttBuilder.connectListener(connectListener)
        }

        /**
         * Sets the default subscribe info for this client and bind listener
         *
         * @param topicsBodies Subscribe topic and qos
         * @param subscribeListener The subscribe action listener
         * @param autoSubscribe Whether to subscribe automatically after reconnect
         * @return Builder
         */
        @JvmOverloads
        fun subscribeBodies(
            subscribeListener: IActionListener? = null,
            autoSubscribe: Boolean = false,
            vararg topicsBodies: Topics
        ) = apply {
            this.mqttBuilder.subscribeBodies(subscribeListener, autoSubscribe, *topicsBodies)
        }

        /**
         * 是否开启log
         *
         * @param showMqttLog 是否开启 Mqtt 调度者的log
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        @JvmOverloads
        fun openLog(showMqttLog: Boolean, openRRBindLog: Boolean = false) = apply {
            Logger.showLog(showMqttLog)
            log.showLog(openRRBindLog)
        }

        /**
         * 是否开启显示堆栈跟踪
         *
         * @param isShowStackTrace 是否显示堆栈跟踪
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        @JvmOverloads
        fun openStackTrace(isShowStackTrace: Boolean, openRRBindTrace: Boolean = false) = apply {
            Logger.showStackTrace(isShowStackTrace)
            log.showStackTrace(openRRBindTrace)
        }

        /**
         * Whether to activate subscription optimization.
         * After subscription optimization is activated,
         * inclusive subscriptions will no longer be repeated subscriptions
         * For example：A/B/C/# and A/B/C/+ and A/B/C/D，
         * it will subscribe only A/B/C/# ，However,
         * the linked list of records will store the above three subscription information
         */
        fun subscribeOptimize(subscribeOptimize: Boolean) = apply {
            this.mqttBuilder.subscribeOptimize(subscribeOptimize)
        }

        /**
         * Automatic registration requires subscription topics,requests or subscription messages.
         * If the message is not in the subscription collection,
         * the subscription will be automatically added
         * Note: backTopic must be a fully subscribed topic
         * and cannot be part of the message body "mqttMessage"
         */
        fun autoRegister(autoRegister: Boolean) = apply {
            this.mqttBuilder.autoRegister(autoRegister)
        }

        /**
         * 配置自定义的Logger
         *
         * @param logger Logger
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun setLogger(logger: ILogger) = apply {
            Logger.setLogger(logger)
        }

        fun build(): OkMqttClient {
            if (mqttRRBinderClient == null) {
                mqttRRBinderClient = mqttRRBinderBuilder.build()
            }
            if (mqttClient == null) {
                mqttClient = mqttBuilder.build(mqttRRBinderClient!!.responseHandler)
            }

            return OkMqttClient(this)
        }
    }

}