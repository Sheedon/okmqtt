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
import android.os.Handler
import android.os.Looper
import org.eclipse.paho.android.service.MqttAndroidClient
import kotlin.Throws
import org.eclipse.paho.android.service.MqttAndroidClient.Ack
import org.eclipse.paho.client.mqttv3.*
import org.sheedon.mqtt.internal.binder.IResponseHandler
import org.sheedon.mqtt.listener.*
import org.sheedon.mqtt.utils.Logger
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

/**
 * Real mqtt wrapper scheduling client
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/27 10:19 上午
 */
class MqttWrapperClient private constructor(
    builder: Builder = Builder(),
    _responseHandler: IResponseHandler
) {
    // 锁
    private val lock = Any()

    // mqttAndroid客户端
    private val mqttClient: MqttAndroidClient = builder.androidClient!!

    // 连接配置选项
    private val connectOptions: MqttConnectOptions = builder.connectOptions!!

    // 断开连接缓冲配置
    private val bufferOpts: DisconnectedBufferOptions = builder.bufferOpts!!

    // 消息监听者
    private val messageListener: IMessageListener? = builder.callback

    // 连接情况监听器
    private val connectListener: IActionListener? = builder.connectListener
    private val autoReconnect: Boolean = builder.connectOptions!!.isAutomaticReconnect

    // 主题通配符过滤器
    private val wildcardFiller: WildcardFiller = WildcardFiller(
        builder.subscribeBodies
    )

    // 订阅情况监听器
    private val subscribeListener: IActionListener? = builder.subscribeListener
    private val autoSubscribe: Boolean = builder.autoSubscribe

    // 响应结果处理者
    internal var responseHandler: IResponseHandler = _responseHandler

    // 上一次重连时间
    private var lastConnectTime: Long = 0
    private var lastDisconnectTime: Long = 0

    // 是否开始连接
    private var isStartConnect = false
    private var isStartDisconnect = false

    private val handler = Handler(Looper.myLooper()!!) {
        reConnect()
        true
    }

    /**
     * mqtt连接动作的监听器
     * 包括连接成功与否 和 重连
     */
    private interface MqttConnectActionListener : MqttCallbackExtended, IMqttActionListener,
        IActionListener

    private val callbackListener: MqttConnectActionListener = object : MqttConnectActionListener {

        /**
         * 连接成功
         */
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            val action =
                if (mqttClient.isConnected) {
                    IActionListener.ACTION.CONNECT
                } else {
                    IActionListener.ACTION.DISCONNECT
                }
            connectListener?.onSuccess(action)
            resetStatus()
            autoSubscribe()
            handler.removeCallbacksAndMessages(null)
        }

        override fun onSuccess(action: IActionListener.ACTION) {
            connectListener?.onSuccess(action)
            resetStatus()
            autoSubscribe()
            handler.removeCallbacksAndMessages(null)
        }

        /**
         * 连接失败
         */
        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            val action =
                if (mqttClient.isConnected) IActionListener.ACTION.DISCONNECT else IActionListener.ACTION.CONNECT
            connectListener?.onFailure(action, exception)
            resetStatus()
            autoReconnect()
        }

        override fun onFailure(action: IActionListener.ACTION, exception: Throwable?) {
            connectListener?.onFailure(action, exception)
            resetStatus()
            autoReconnect()
        }

        /**
         * 连接断开
         */
        override fun connectionLost(cause: Throwable) {
            connectListener?.onFailure(IActionListener.ACTION.CONNECT, cause)
            resetStatus()
            autoReconnect()
        }

        /**
         * 重新连接成功
         */
        override fun connectComplete(reconnect: Boolean, serverURI: String) {
            connectListener?.onSuccess(IActionListener.ACTION.CONNECT)
            resetStatus()
            autoSubscribe()
            handler.removeCallbacksAndMessages(null)
        }

        /**
         * 重置连接状态
         */
        private fun resetStatus() {
            isStartConnect = false
            isStartDisconnect = false
        }

        /**
         * 自动重连
         */
        private fun autoReconnect() {
            if (autoReconnect) {
                handler.sendEmptyMessageDelayed(MESSAGE_WHAT, AUTO_RECONNECT_INTERVAL)
            }
        }

        /**
         * 自动订阅
         * */
        private fun autoSubscribe() {

            val subscribeBodies = wildcardFiller.topicsBodies
            if (subscribeBodies.isEmpty()) return

            val topic = mutableListOf<String>()
            val qos = mutableListOf<Int>()

            subscribeBodies.forEach { value ->
                topic.add(value.topic)
                qos.add(value.qos)
            }
            mqttClient.subscribe(topic.toTypedArray(), qos.toIntArray())
        }

        @Throws(Exception::class)
        override fun messageArrived(topic: String, message: MqttMessage) {
            messageListener?.messageArrived(topic, message)
            responseHandler.callResponse(topic, message)
        }

        override fun deliveryComplete(token: IMqttDeliveryToken) {
            // 暂时不执行该方法
        }
    }

    companion object {
        // 重试间隔 5秒
        const val EXECUTE_INTERVAL = 5000

        // 自动重连间隔 30秒
        const val AUTO_RECONNECT_INTERVAL = 30000L

        // 重连消息ID
        const val MESSAGE_WHAT = 0x0101
    }

    init {

        mqttClient.setCallback(callbackListener)
        reConnect()
    }

    /**
     * mqtt 创建连接
     */
    private fun connect(listener: IMqttActionListener? = null) {
        synchronized(lock) {
            if (mqttClient.isConnected || isStartConnect) {
                return
            }
        }
        isStartConnect = true
        val realListener = createConnectListener(listener, IActionListener.ACTION.CONNECT)
        mqttClient.connect(connectOptions, realListener)
    }

    /**
     * 重连操作
     */
    @JvmOverloads
    fun reConnect(
        listener: IMqttActionListener? = null
    ) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastConnectTime < EXECUTE_INTERVAL) {
            isStartConnect = false
            val throwable = Throwable("Only reconnect once within 5 seconds")
            failureAction(listener, connectListener, IActionListener.ACTION.CONNECT, throwable)
            return
        }

        lastConnectTime = nowTime
        try {
            connect(listener)
            Logger.info("connect mqtt server")
        } catch (e: Exception) {
            failureAction(listener, connectListener, IActionListener.ACTION.CONNECT, e)
        }
    }

    /**
     * mqtt 断开连接
     */
    private fun disconnect(listener: IMqttActionListener? = null) {
        synchronized(lock) {
            if (!mqttClient.isConnected && isStartDisconnect) {
                Logger.error("disconnect isConnected = $mqttClient.isConnected, isStartDisconnect = $isStartDisconnect")
                return
            }
        }
        isStartDisconnect = false
        val realListener = createConnectListener(listener, IActionListener.ACTION.DISCONNECT)
        mqttClient.disconnect(connectOptions, realListener)
        Logger.info("disconnect")
    }

    @JvmOverloads
    fun disConnect(listener: IMqttActionListener? = null) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastDisconnectTime < EXECUTE_INTERVAL) {
            isStartDisconnect = false
            val throwable = Throwable("Only disconnect once within 5 seconds")
            failureAction(listener, connectListener, IActionListener.ACTION.DISCONNECT, throwable)
            return
        }

        lastDisconnectTime = nowTime
        try {
            disconnect(listener)
            Logger.info("disConnect mqtt server")
        } catch (e: Exception) {
            failureAction(listener, connectListener, IActionListener.ACTION.CONNECT, e)
        }
    }

    /**
     * 创建连接和断开的监听器
     */
    private fun createConnectListener(
        listener: IMqttActionListener?,
        action: IActionListener.ACTION
    ): IMqttActionListener {
        return if (listener == null) {
            callbackListener
        } else {
            RealCallbackListener(callbackListener, listener, action)
        }
    }


    /**
     * 失败动作的消息反馈
     *
     * @param resultActionListener 方法调度时添加的反馈动作监听器
     * @param actionCallback 全局监听的调度动作监听器
     * @param action 动作类型
     * @param throwable 错误信息
     * */
    private fun failureAction(
        resultActionListener: IMqttActionListener? = null,
        actionCallback: IActionListener? = null,
        action: IActionListener.ACTION,
        throwable: Throwable
    ) {
        resultActionListener?.onFailure(null, throwable)
        actionCallback?.onFailure(action, throwable)
        Logger.error("failure action", throwable)
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
        // 是否附加到缓存记录中，若false，则代表单次订阅，清空行为后，不恢复
        var subscribe: Topics? = null
        if (body.headers.attachRecord) {
            subscribe = wildcardFiller.subscribe(body, this)
        }

        subscribe?.let {
            subscribeRealTopic(it, listener)
        }
    }

    /**
     * 真实的订阅方法
     *
     * @param body mqtt消息体
     * @param listener 操作监听器
     */
    internal fun subscribeRealTopic(
        body: Topics,
        listener: IMqttActionListener? = null
    ) {
        mqttClient.subscribe(body.topic, body.qos, null, SubscribeListener(listener))
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

        val (topic, qos) = wildcardFiller.subscribe(bodies, this)

        if (topic.isEmpty()) {
            Logger.info("not topic need subscribe!")
            listener?.onSuccess(null)
            return
        }

        // 执行真实的订阅一个主题集合
        subscribeRealTopic(topic, qos, listener)
    }

    /**
     * 真实的订阅方法
     *
     * @param body mqtt消息体
     * @param listener 操作监听器
     */
    internal fun subscribeRealTopic(
        topicArray: Collection<String>,
        qosArray: Collection<Int>,
        listener: IMqttActionListener? = null
    ) {
        mqttClient.subscribe(
            topicArray.toTypedArray(),
            qosArray.toIntArray(),
            null,
            SubscribeListener(listener)
        )
    }

    /**
     * 订阅监听者实现类
     */
    private inner class SubscribeListener(val listener: IMqttActionListener? = null) :
        IMqttActionListener {

        override fun onSuccess(asyncActionToken: IMqttToken?) {
            listener?.onSuccess(asyncActionToken)
            Logger.info("subscribe onSuccess")
            subscribeListener?.onSuccess(IActionListener.ACTION.SUBSCRIBE)
        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            listener?.onFailure(asyncActionToken, exception)
            Logger.error("subscribe onFailure", exception)
            subscribeListener?.onFailure(IActionListener.ACTION.SUBSCRIBE, exception)
        }
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

        // 是否附加到缓存记录中，若false，则代表单次订阅，清空行为后，不恢复
        var unsubscribe: Topics? = null
        if (body.headers.attachRecord) {
            //是否附加到缓存记录中，若false，则代表单次订阅，清空行为后，不恢复
            unsubscribe = wildcardFiller.unsubscribe(body, this)
        }

        unsubscribe?.let {
            unsubscribeRealTopic(it, listener)
        }
    }

    /**
     * 真实的取消订阅方法
     *
     * @param body mqtt消息体
     * @param listener 操作监听器
     */
    internal fun unsubscribeRealTopic(
        body: Topics,
        listener: IMqttActionListener? = null
    ) {
        mqttClient.unsubscribe(body.topic, null, UnSubscribeListener(listener))
    }

    /**
     * 取消订阅mqtt主题
     * @param bodies mqtt消息体集合
     *  @param listener 操作监听器
     */
    @JvmOverloads
    fun unsubscribe(
        bodies: List<Topics>,
        listener: IMqttActionListener? = null
    ) {
        val topics = wildcardFiller.unsubscribe(bodies, this)
        if (topics.isEmpty()) {
            Logger.info("not topic need unsubscribe!")
            listener?.onSuccess(null)
            return
        }

        // 执行真实的订阅一个主题集合
        unsubscribeRealTopic(topics, listener)
    }

    /**
     * 真实的取消订阅方法
     *
     * @param body mqtt消息体
     * @param listener 操作监听器
     */
    internal fun unsubscribeRealTopic(
        topics: Collection<String>,
        listener: IMqttActionListener? = null
    ) {
        mqttClient.unsubscribe(topics.toTypedArray(), null, UnSubscribeListener(listener))
    }

    /**
     * 订阅监听者实现类
     */
    internal inner class UnSubscribeListener(val listener: IMqttActionListener? = null) :
        IMqttActionListener {

        override fun onSuccess(asyncActionToken: IMqttToken?) {
            listener?.onSuccess(asyncActionToken)
            Logger.info("unsubscribe onSuccess")
            subscribeListener?.onSuccess(IActionListener.ACTION.UNSUBSCRIBE)
        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            listener?.onFailure(asyncActionToken, exception)
            Logger.error("unsubscribe onFailure", exception)
            subscribeListener?.onFailure(IActionListener.ACTION.UNSUBSCRIBE, exception)
        }
    }

    /**
     * 重新订阅mqtt主题
     * 1. 取消原来订阅
     * 2. 记录原来订阅信息，以备订阅失败做恢复处理，订阅当前配置信息
     * 3. 第一步取消失败，则反馈订阅失败
     * 4. 第二部订阅失败，则反馈订阅失败，恢复当前数据
     * 否则订阅成功
     *
     * @param bodies mqtt消息体集合
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun reSubscribe(
        bodies: List<Topics>,
        listener: IMqttActionListener? = null
    ) {
        unsubscribe(bodies, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Logger.info("unsubscribe onSuccess")
                reSubscribeBySuccess(bodies, listener)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                listener?.onFailure(asyncActionToken, exception)
                Logger.error("unsubscribe onFailure", exception)
                subscribeListener?.onFailure(IActionListener.ACTION.SUBSCRIBE, exception)
            }
        })

    }

    /**
     * 重新订阅mqtt主题
     * 在取消订阅成功后，进行重新订阅新的主题
     *
     * @param bodies mqtt消息体集合
     * @param listener 操作监听器
     */
    private fun reSubscribeBySuccess(
        bodies: List<Topics>,
        listener: IMqttActionListener? = null
    ) {

        val copyBodies = ArrayList(wildcardFiller.topicsBodies)
        wildcardFiller.clear()
        subscribe(bodies, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                listener?.onSuccess(asyncActionToken)
                Logger.info("subscribe onSuccess")
                subscribeListener?.onSuccess(IActionListener.ACTION.SUBSCRIBE)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                wildcardFiller.clear()
                // 重新订阅原来数据
                subscribe(copyBodies)
                Logger.error("subscribe onFailure", exception)
                listener?.onFailure(asyncActionToken, exception)
                subscribeListener?.onFailure(IActionListener.ACTION.SUBSCRIBE, exception)
            }

        })
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
        // mqtt的Android客户端
        internal var androidClient: MqttAndroidClient? = null

        // 上下文，用于创建MqttAndroidClient
        private var context: Context? = null

        // 服务地址
        private var serverURI: String? = null

        // 设备ID
        private var clientId: String? = null

        // Mqtt 客户端持久化
        private var persistence: MqttClientPersistence? = null

        // Ack 反馈处理类型
        private var ackType: Ack? = null

        // 连接配置选项
        internal var connectOptions: MqttConnectOptions?

        // 断开连接缓冲选项
        internal var bufferOpts: DisconnectedBufferOptions?

        // mqtt 反馈监听器
        internal var callback: IMessageListener? = null

        // 连接情况监听器
        internal var connectListener: IActionListener? = null

        // 订阅信息
        internal var subscribeBodies = mutableListOf<Topics>()

        // 订阅情况监听器
        internal var subscribeListener: IActionListener? = null

        // 在重新连接后，是否自动订阅,MqttConnectOptions isCleanSession == true and autoSubscribe == true,
        // 才支持自动订阅
        internal var autoSubscribe: Boolean = true

        init {
            connectOptions = DefaultMqttConnectOptions.default
            bufferOpts = DefaultDisconnectedBufferOptions.default
        }

        /**
         * Enables an android application to communicate with an MQTT server using non-blocking methods.
         *
         * @param androidClient MqttAndroidClient
         * @return Builder
         */
        fun androidClient(androidClient: MqttAndroidClient) = apply {
            this.androidClient = androidClient
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
            ackType: Ack? = Ack.AUTO_ACK
        ) = apply {
            this.context = context
            this.serverURI = serverURI
            this.clientId = if (clientId.isNullOrEmpty()) UUID.randomUUID().toString() else clientId
            this.persistence = persistence
            this.ackType = ackType ?: Ack.AUTO_ACK
        }

        /**
         * set of connection parameters that override the defaults
         *
         * @param options  connection parameters
         * @param <Option> MqttConnectOptions
         * @return Builder
        </Option> */
        fun <Option : MqttConnectOptions> connectOptions(options: Option) = apply {
            this.connectOptions = options
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
                this.bufferOpts = bufferOpts
            }

        /**
         * Sets the MessageListener for this client
         *
         * @param callback the MessageListener
         * @return Builder
         */
        fun callback(callback: IMessageListener) = apply {
            this.callback = callback
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
            this.connectListener = connectListener
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
            autoSubscribe: Boolean = true,
            vararg topicsBodies: Topics
        ) = apply {
            this.subscribeBodies.clear()
            this.subscribeBodies.addAll(topicsBodies)
            this.subscribeListener = subscribeListener
            this.autoSubscribe = autoSubscribe
        }

        /**
         * Build MqttWrapperClient by MqttWrapperClient.Builder
         *
         * @return MqttWrapperClient
         */
        fun build(responseHandler: IResponseHandler): MqttWrapperClient {

            if (this.connectOptions?.isCleanSession == false) {
                this.autoSubscribe = false
            }

            // If androidClient is not null,
            // it means that the developer wants to use the MqttAndroidClient created by himself
            if (androidClient != null) {
                return MqttWrapperClient(this, responseHandler)
            }

            // Otherwise, create it according to the configuration content
            requireNotNull(context) {
                "The current context is empty, please set the context"
            }
            requireNotNull(serverURI) {
                "The current serverURI is empty, please set the serverURI"
            }
            clientId = if (clientId.isNullOrEmpty()) UUID.randomUUID().toString() else clientId
            this.ackType = ackType ?: Ack.AUTO_ACK
            androidClient = MqttAndroidClient(context, serverURI, clientId, persistence, ackType)
            return MqttWrapperClient(this, responseHandler)
        }
    }
}