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
import org.sheedon.mqtt.internal.WildcardFilter
import org.sheedon.mqtt.internal.binder.IResponseHandler
import org.sheedon.mqtt.listener.*
import org.sheedon.mqtt.utils.Logger
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

/**
 * 旨在于维持「mqtt客户端」，执行以下行为：
 * 1.mqtt连接。
 * 2.重连。
 * 3.断开连接。
 * 4.订阅主题。
 * 5.取消订阅主题。
 * 6.发送mqtt消息。
 * 7.断开自动重连
 * 8.维持一个订阅消息过滤器。
 *
 * 订阅消息过滤器：在mqtt开发使用中，可能存在，依次订阅了存在包含的主题，
 * 从而无法分别响应得到的消息，是否是因为重复订阅而存在重复。
 * 例如：依次订阅：AA/BB/CC，AA/BB/#，当监听得到一个AA/BB/CC为主题的消息时，
 * AA/BB/CC会收到一次，AA/BB/#也会收到一次，从而产生重复。
 * 而订阅消息过滤器则是过滤包含主题造成的重复问题。
 * 若依次监听「AA/BB/CC，AA/BB/#」，在监听「AA/BB/#」之前，会先取消订阅「AA/BB/CC」，取消「AA/BB/#」监听后，恢复「AA/BB/CC」监听。
 * 若依次监听「AA/BB/#，AA/BB/CC」，不会监听「AA/BB/CC」，取消监听「AA/BB/#」后，再执行监听「AA/BB/CC」
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

    // mqtt connect or disconnect add lock
    private val lock = Any()

    // Implementation of the MQTT asynchronous client interface IMqttAsyncClient,
    // using the MQTT android service to actually interface with MQTT server.
    private val mqttClient: MqttAndroidClient = builder.androidClient!!

    // Connects to an MQTT server using the default options.
    private val connectOptions: MqttConnectOptions = builder.connectOptions!!

    // The DisconnectedBufferOptions for this client
    private val bufferOpts: DisconnectedBufferOptions = builder.bufferOpts!!

    // The global listener for the current mqtt client message
    private val messageListener: IMessageListener? = builder.callback

    // The connect listener for this client
    private val connectListener: IActionListener? = builder.connectListener

    // Auto reConnect mqtt service
    private val autoReconnect: Boolean = builder.connectOptions!!.isAutomaticReconnect

    // subscribe mqtt topic by wildcard filter
    // append default subscribeBodies to WildcardFilter
    private val wildcardFilter: WildcardFilter = WildcardFilter(
        builder.subscribeBodies
    )

    // The global listener for the subscribe mqtt topic
    private val subscribeListener: IActionListener? = builder.subscribeListener

    // auto subscribe mqtt topic
    private val autoSubscribe: Boolean = builder.autoSubscribe

    // response handler send MQTT Message to MqttRRBinderClient
    internal var responseHandler: IResponseHandler = _responseHandler

    // record last connect time, connect interval time more than EXECUTE_INTERVAL
    private var lastConnectTime: Long = 0

    // record last disconnect time, disconnect interval time more than EXECUTE_INTERVAL
    private var lastDisconnectTime: Long = 0

    // start connect flag
    private var startConnect = false

    // start disconnect flag
    private var startDisconnect = false

    // loop reconnect handler
    private val handler = Handler(Looper.myLooper()!!) {
        reConnect()
        true
    }

    /**
     * Listener for mqtt action interface.
     * it extends MqttCallbackExtended, IMqttActionListener and IActionListener
     */
    private interface MqttConnectActionListener : MqttCallbackExtended, IMqttActionListener,
        IActionListener

    /**
     * 1. Extension of [MqttCallbackExtended] to allow new callbacks
     * without breaking the API for existing applications.
     * 2. Implementors of this interface will be notified when an asynchronous action completes.
     * 3. Implementors of wrapper actionListener with IMqttActionListener
     */
    private val callbackListener: MqttConnectActionListener = object : MqttConnectActionListener {

        /**
         * This method is invoked when an action has completed successfully.
         * First dispatch onSuccess() with the [IActionListener.ACTION.CONNECT] or [IActionListener.ACTION.DISCONNECT]
         * by connectListener.
         * Second reset connect status and disconnect status.
         * Third auto subscribe MQTT Topic.
         * At last remove reconnect event
         *
         * @param asyncActionToken associated with the action that has completed
         */
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            // According to the connection status, get the action.
            val action =
                if (mqttClient.isConnected) {
                    IActionListener.ACTION.CONNECT
                } else {
                    IActionListener.ACTION.DISCONNECT
                }

            this.onSuccess(action)
        }

        /**
         * This method is invoked when an action has completed successfully.
         * First dispatch onSuccess() with the [IActionListener.ACTION.CONNECT] or [IActionListener.ACTION.DISCONNECT]
         * by connectListener.
         * Second reset connect status and disconnect status.
         * Third auto subscribe MQTT Topic.
         * At last remove reconnect event
         *
         * @param action associated action enum
         */
        override fun onSuccess(action: IActionListener.ACTION) {
            // notify result
            connectListener?.onSuccess(action)

            resetStatus()

            // if connected, auto subscribe
            if (mqttClient.isConnected) {
                autoSubscribe()
            }

            // remove reconnect event
            handler.removeCallbacksAndMessages(null)
        }

        /**
         * This method is invoked when an action fails.
         * If a client is disconnected while an action is in progress
         * onFailure will be called. For connections
         * that use cleanSession set to false, any QoS 1 and 2 messages that
         * are in the process of being delivered will be delivered to the requested
         * quality of service next time the client connects.
         *
         * @param asyncActionToken associated with the action that has failed
         * @param exception thrown by the action that has failed
         */
        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            val action =
                if (mqttClient.isConnected) {
                    IActionListener.ACTION.DISCONNECT
                } else {
                    IActionListener.ACTION.CONNECT
                }

            this.onFailure(action, exception)
        }

        override fun onFailure(action: IActionListener.ACTION, exception: Throwable?) {
            // notify result
            connectListener?.onFailure(action, exception)

            resetStatus()

            // auto reconnect
            autoReconnect()
        }

        /**
         * This method is called when the connection to the server is lost.
         * handle connect failure event to [onFailure]
         *
         * @param cause the reason behind the loss of connection.
         */
        override fun connectionLost(cause: Throwable) {
            this.onFailure(IActionListener.ACTION.CONNECT, cause)
        }

        /**
         * Called when the connection to the server is completed successfully.
         *
         * @param reconnect If true, the connection was the result of automatic reconnect.
         * @param serverURI The server URI that the connection was made to.
         */
        override fun connectComplete(reconnect: Boolean, serverURI: String) {
            this.onSuccess(IActionListener.ACTION.CONNECT)
        }

        /**
         * Reset connection state.
         * Use for connect or disconnect
         */
        private fun resetStatus() {
            startConnect = false
            startDisconnect = false
        }

        /**
         * Only when autoReconnect == true can the reconnect action be started
         */
        private fun autoReconnect() {
            if (autoReconnect) {
                handler.sendEmptyMessageDelayed(MESSAGE_WHAT, AUTO_RECONNECT_INTERVAL)
            }
        }

        /**
         * Automatically subscribe to topics with the help of wildcardFilter,
         * that gets the set of topics that need to be subscribed
         * and execute the subscription through mqttClient.
         * */
        private fun autoSubscribe() {

            val subscribeBodies = wildcardFilter.topicsBodies
            if (subscribeBodies.isEmpty()) return

            val topic = mutableListOf<String>()
            val qos = mutableListOf<Int>()

            subscribeBodies.forEach { value ->
                topic.add(value.topic)
                qos.add(value.qos)
            }
            mqttClient.subscribe(topic.toTypedArray(), qos.toIntArray())
        }

        /**
         * This method is called when a message arrives from the server.
         *
         * <p>
         * This method is invoked synchronously by the MQTT client. An
         * acknowledgment is not sent back to the server until this
         * method returns cleanly.</p>
         * <p>
         * If an implementation of this method throws an <code>Exception</code>, then the
         * client will be shut down.  When the client is next re-connected, any QoS
         * 1 or 2 messages will be redelivered by the server.</p>
         * <p>
         * Any additional messages which arrive while an
         * implementation of this method is running, will build up in memory, and
         * will then back up on the network.</p>
         * <p>
         * If an application needs to persist data, then it
         * should ensure the data is persisted prior to returning from this method, as
         * after returning from this method, the message is considered to have been
         * delivered, and will not be reproducible.</p>
         * <p>
         * It is possible to send a new message within an implementation of this callback
         * (for example, a response to this message), but the implementation must not
         * disconnect the client, as it will be impossible to send an acknowledgment for
         * the message being processed, and a deadlock will occur.</p>
         *
         * @param topic name of the topic on the message was published to
         * @param message the actual message.
         * @throws Exception if a terminal error has occurred, and the client should be
         * shut down.
         */
        @Throws(Exception::class)
        override fun messageArrived(topic: String, message: MqttMessage) {
            messageListener?.messageArrived(topic, message)
            responseHandler.callResponse(topic, message)
        }

        /**
         * Do not execute this method temporarily.
         * Called when delivery for a message has been completed, and all
         * acknowledgments have been received. For QoS 0 messages it is
         * called once the message has been handed to the network for
         * delivery. For QoS 1 it is called when PUBACK is received and
         * for QoS 2 when PUBCOMP is received. The token will be the same
         * token as that returned when the message was published.
         *
         * @param token the delivery token associated with the message.
         */
        override fun deliveryComplete(token: IMqttDeliveryToken) {}
    }

    companion object {
        // Retry interval 5 seconds
        const val EXECUTE_INTERVAL = 5000

        // Automatic reconnection interval 30 seconds
        const val AUTO_RECONNECT_INTERVAL = 30000L

        // Reconnect message ID
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
            if (mqttClient.isConnected || startConnect) {
                return
            }
        }
        startConnect = true
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
            startConnect = false
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
            if (!mqttClient.isConnected && startDisconnect) {
                Logger.error("disconnect isConnected = $mqttClient.isConnected, isStartDisconnect = $startDisconnect")
                return
            }
        }
        startDisconnect = false
        val realListener = createConnectListener(listener, IActionListener.ACTION.DISCONNECT)
        mqttClient.disconnect(connectOptions, realListener)
        Logger.info("disconnect")
    }

    @JvmOverloads
    fun disConnect(listener: IMqttActionListener? = null) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastDisconnectTime < EXECUTE_INTERVAL) {
            startDisconnect = false
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
            subscribe = wildcardFilter.subscribe(body, ::unsubscribeRealTopic)
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

        val (topic, qos) = wildcardFilter.subscribe(bodies, ::unsubscribeRealTopic)

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
            unsubscribe = wildcardFilter.unsubscribe(body, ::subscribeRealTopic)
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
        val topics = wildcardFilter.unsubscribe(bodies, ::subscribeRealTopic)
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

        val copyBodies = ArrayList(wildcardFilter.topicsBodies)
        wildcardFilter.clear()
        subscribe(bodies, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                listener?.onSuccess(asyncActionToken)
                Logger.info("subscribe onSuccess")
                subscribeListener?.onSuccess(IActionListener.ACTION.SUBSCRIBE)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                wildcardFilter.clear()
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

    class Builder internal constructor() {
        internal var androidClient: MqttAndroidClient? = null
        private var context: Context? = null
        private var serverURI: String? = null
        private var clientId: String? = null
        private var persistence: MqttClientPersistence? = null
        private var ackType: Ack? = null

        internal var connectOptions: MqttConnectOptions?
        internal var bufferOpts: DisconnectedBufferOptions?

        internal var callback: IMessageListener? = null
        internal var connectListener: IActionListener? = null

        internal var subscribeBodies = mutableListOf<Topics>()
        internal var subscribeListener: IActionListener? = null

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
         * Constructor - create an MqttAndroidClient that can be used to communicate
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
         * Sets of connection parameters that override the defaults.
         * The connection will be established using the options specified in the
         * [MqttConnectOptions] parameter.
         * The default options is [DefaultMqttConnectOptions.default]
         *
         * @param options  connection parameters
         * @param <Option> MqttConnectOptions
         */
        fun <Option : MqttConnectOptions> connectOptions(options: Option) = apply {
            this.connectOptions = options
        }

        /**
         * Sets the DisconnectedBufferOptions for this client.
         * The default bufferOpts is [DefaultDisconnectedBufferOptions.default]
         *
         * @param bufferOpts the DisconnectedBufferOptions
         * @param <Option>   DisconnectedBufferOptions
         */
        fun <Option : DisconnectedBufferOptions> disconnectedBufferOptions(bufferOpts: Option) =
            apply {
                this.bufferOpts = bufferOpts
            }

        /**
         * Sets the global listener for the current mqtt client message.
         * This listener receives MQTT Message, optional items.
         *
         * @param callback the MessageListener
         */
        fun callback(callback: IMessageListener) = apply {
            this.callback = callback
        }

        /**
         * Sets the connect listener for this client.
         * Listen MQTT connect status.
         *
         * @param connectListener the connect action listener
         */
        @JvmOverloads
        fun connectListener(
            connectListener: IActionListener? = null
        ) = apply {
            this.connectListener = connectListener
        }

        /**
         * Subscribe to multiple topics, each of which may include wildcards,
         * and add a global listener for the subscription result.
         * autoSubscribe: After reconnecting, whether to subscribe automatically,
         * MqttConnectOptions isCleanSession == true and autoSubscribe == true,
         * only supports automatic subscription
         *
         * @param topicsBodies Subscribe topic and qos
         * @param subscribeListener The subscribe action listener
         * @param autoSubscribe Whether to subscribe automatically after reconnect
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
         * @param responseHandler it send MQTT Message to MqttRRBinderClient
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

            // create MqttAndroidClient
            androidClient = MqttAndroidClient(context, serverURI, clientId, persistence, ackType)

            return MqttWrapperClient(this, responseHandler)
        }
    }
}