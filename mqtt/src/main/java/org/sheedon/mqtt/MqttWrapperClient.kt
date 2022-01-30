package org.sheedon.mqtt

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.eclipse.paho.android.service.MqttAndroidClient
import kotlin.Throws
import org.eclipse.paho.android.service.MqttAndroidClient.Ack
import org.eclipse.paho.client.mqttv3.*
import org.sheedon.mqtt.listener.*
import java.lang.Exception
import java.util.*

/**
 * 真实的mqtt调度客户端
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/27 10:19 上午
 */
class MqttWrapperClient private constructor(builder: Builder = Builder()) {
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

    // 主题订阅数据
    private val subscribeBodies: MutableMap<String, SubscribeBody> = builder.subscribeBodies

    // 订阅情况监听器
    private val subscribeListener: IActionListener? = builder.subscribeListener
    private val autoSubscribe: Boolean = builder.autoSubscribe

    // 数据交换中介
    internal var switchMediator: SwitchMediator? = null

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
                if (mqttClient.isConnected) IMqttListener.ACTION.CONNECT else IMqttListener.ACTION.DISCONNECT
            connectListener?.onSuccess(action)
            resetStatus()
            handler.removeCallbacksAndMessages(null)
        }

        override fun onSuccess(action: IMqttListener.ACTION) {
            connectListener?.onSuccess(action)
            resetStatus()
            handler.removeCallbacksAndMessages(null)
        }

        /**
         * 连接失败
         */
        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            val action =
                if (mqttClient.isConnected) IMqttListener.ACTION.DISCONNECT else IMqttListener.ACTION.CONNECT
            connectListener?.onFailure(action, exception)
            resetStatus()
            autoReconnect()
        }

        override fun onFailure(action: IMqttListener.ACTION, exception: Throwable?) {
            connectListener?.onFailure(action, exception)
            resetStatus()
            autoReconnect()
        }

        /**
         * 连接断开
         */
        override fun connectionLost(cause: Throwable) {
            connectListener?.onFailure(IMqttListener.ACTION.CONNECT, cause)
            resetStatus()
            autoReconnect()
        }

        /**
         * 重新连接成功
         */
        override fun connectComplete(reconnect: Boolean, serverURI: String) {
            connectListener?.onSuccess(IMqttListener.ACTION.CONNECT)
            resetStatus()
            handler.removeCallbacksAndMessages(null)
        }

        /**
         * 重置连接状态
         */
        private fun resetStatus() {
            isStartConnect = false
            isStartDisconnect = false
        }

        private fun autoReconnect() {
            if (autoReconnect) {
                handler.sendEmptyMessageDelayed(MESSAGE_WHAT, AUTO_RECONNECT_INTERVAL)
            }
        }

        /**
         * 自动订阅
         * */
        private fun autoSubscribe() {

            val topic = mutableListOf<String>()
            val qos = mutableListOf<Int>()

            subscribeBodies.forEach { (_, value) ->
                topic.add(value.topic!!)
                qos.add(value.qos)
            }
            mqttClient.subscribe(topic.toTypedArray(), qos.toIntArray())
        }

        @Throws(Exception::class)
        override fun messageArrived(topic: String, message: MqttMessage) {
            switchMediator?.callResponse(ResponseBody(topic, message))
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
     * 绑定数据交换中介者
     * */
    internal fun bindSwitchMediator(switchMediator: SwitchMediator) {
        this.switchMediator = switchMediator
    }

    /**
     * mqtt 创建连接
     */
    private fun connect(listener: IResultActionListener? = null) {
        synchronized(lock) {
            if (mqttClient.isConnected || isStartConnect) {
                return
            }
        }
        isStartConnect = true
        val realListener = createConnectListener(listener, IMqttListener.ACTION.CONNECT)
        mqttClient.connect(connectOptions, realListener)
    }

    /**
     * 重连操作
     */
    @JvmOverloads
    fun reConnect(
        listener: IResultActionListener? = null
    ) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastConnectTime < EXECUTE_INTERVAL) {
            isStartConnect = false
            val throwable = Throwable("Only reconnect once within 5 seconds")
            failureAction(listener, connectListener, IMqttListener.ACTION.CONNECT, throwable)
            return
        }

        lastConnectTime = nowTime
        try {
            connect(listener)
        } catch (e: Exception) {
            failureAction(listener, connectListener, IMqttListener.ACTION.CONNECT, e)
        }
    }

    /**
     * mqtt 断开连接
     */
    private fun disconnect(listener: IResultActionListener? = null) {
        synchronized(lock) {
            if (!mqttClient.isConnected && isStartDisconnect) {
                return
            }
        }
        isStartDisconnect = false
        val realListener = createConnectListener(listener, IMqttListener.ACTION.DISCONNECT)
        mqttClient.disconnect(connectOptions, realListener)
    }

    @JvmOverloads
    fun disConnect(listener: IResultActionListener? = null) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastDisconnectTime < EXECUTE_INTERVAL) {
            isStartDisconnect = false
            val throwable = Throwable("Only disconnect once within 5 seconds")
            failureAction(listener, connectListener, IMqttListener.ACTION.DISCONNECT, throwable)
            return
        }

        lastDisconnectTime = nowTime
        try {
            disconnect(listener)
        } catch (e: Exception) {
            failureAction(listener, connectListener, IMqttListener.ACTION.CONNECT, e)
        }
    }

    /**
     * 创建连接和断开的监听器
     */
    private fun createConnectListener(
        listener: IResultActionListener?,
        action: IMqttListener.ACTION
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
        resultActionListener: IResultActionListener? = null,
        actionCallback: IActionListener? = null,
        action: IMqttListener.ACTION,
        throwable: Throwable
    ) {
        resultActionListener?.onFailure(throwable)
        actionCallback?.onFailure(action, throwable)
    }


    /**
     * 订阅mqtt主题
     * @param body mqtt消息体
     * @param attachRecord 是否附加到缓存记录中，若false，则代表单次订阅，清空行为后，不恢复
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun subscribe(
        body: SubscribeBody,
        attachRecord: Boolean = false,
        listener: IResultActionListener? = null
    ) {
        if (attachRecord) {
            subscribeBodies
                .takeUnless { subscribeBodies.containsKey(body.convertKey()) }
                ?.let { it[body.convertKey()] = body }
        }
        mqttClient.subscribe(body.topic, body.qos, null, object : IMqttActionListener {

            override fun onSuccess(asyncActionToken: IMqttToken?) {
                listener?.onSuccess()
                subscribeListener?.onSuccess(IMqttListener.ACTION.SUBSCRIBE)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                listener?.onFailure(exception)
                subscribeListener?.onFailure(IMqttListener.ACTION.SUBSCRIBE, exception)
            }

        })
    }

    /**
     * 订阅mqtt主题
     * @param bodies mqtt消息体集合
     * @param attachRecord 是否附加到缓存记录中，若false，则代表单次订阅，清空行为后，不恢复
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun subscribe(
        bodies: List<SubscribeBody>,
        attachRecord: Boolean = false,
        listener: IResultActionListener? = null
    ) {
        val topic = mutableListOf<String>()
        val qos = mutableListOf<Int>()

        bodies.forEach { body ->
            subscribeBodies
                .also {
                    topic.add(body.topic!!)
                    qos.add(body.qos)
                }
                .takeIf { attachRecord && !subscribeBodies.containsKey(body.convertKey()) }
                ?.let {
                    it[body.convertKey()] = body
                }
        }
        mqttClient.subscribe(
            topic.toTypedArray(),
            qos.toIntArray(),
            null,
            object : IMqttActionListener {

                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    listener?.onSuccess()
                    subscribeListener?.onSuccess(IMqttListener.ACTION.SUBSCRIBE)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    listener?.onFailure(exception)
                    subscribeListener?.onFailure(IMqttListener.ACTION.SUBSCRIBE, exception)
                }

            })
    }

    /**
     * 取消订阅mqtt主题
     * @param body mqtt消息体
     * @param attachRecord 是否附加到缓存记录中，若false，则代表单次订阅，清空行为后，不恢复
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun unsubscribe(
        body: SubscribeBody,
        attachRecord: Boolean = false,
        listener: IResultActionListener? = null
    ) {
        if (attachRecord) {
            subscribeBodies
                .takeIf { subscribeBodies.containsKey(body.convertKey()) }
                ?.remove(body.convertKey())
        }
        mqttClient.unsubscribe(body.topic, null, object : IMqttActionListener {

            override fun onSuccess(asyncActionToken: IMqttToken?) {
                listener?.onSuccess()
                subscribeListener?.onSuccess(IMqttListener.ACTION.UNSUBSCRIBE)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                listener?.onFailure(exception)
                subscribeListener?.onFailure(IMqttListener.ACTION.UNSUBSCRIBE, exception)
            }

        })
    }

    /**
     * 取消订阅mqtt主题
     * @param bodies mqtt消息体集合
     * @param attachRecord 是否附加到缓存记录中，若false，则代表单次订阅，清空行为后，不恢复
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun unsubscribe(
        bodies: List<SubscribeBody>,
        attachRecord: Boolean = false,
        listener: IResultActionListener? = null
    ) {
        val topic = mutableListOf<String>()
        val qos = mutableListOf<Int>()

        bodies.forEach { body ->
            subscribeBodies
                .also {
                    topic.add(body.topic!!)
                    qos.add(body.qos)
                }
                .takeIf { attachRecord && subscribeBodies.containsKey(body.convertKey()) }
                ?.remove(body.convertKey())
        }
        mqttClient.unsubscribe(
            topic.toTypedArray(),
            null,
            object : IMqttActionListener {

                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    listener?.onSuccess()
                    subscribeListener?.onSuccess(IMqttListener.ACTION.UNSUBSCRIBE)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    listener?.onFailure(exception)
                    subscribeListener?.onFailure(IMqttListener.ACTION.UNSUBSCRIBE, exception)
                }

            })
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
        bodies: List<SubscribeBody>,
        listener: IResultActionListener? = null
    ) {
        unsubscribe(bodies, false, object : IResultActionListener {
            override fun onSuccess() {
                val copyBodies: List<SubscribeBody> = subscribeBodies.values.toMutableList()
                subscribeBodies.clear()
                subscribe(bodies, true, object : IResultActionListener {
                    override fun onSuccess() {
                        listener?.onSuccess()
                        subscribeListener?.onSuccess(IMqttListener.ACTION.SUBSCRIBE)
                    }

                    override fun onFailure(exception: Throwable?) {
                        subscribeBodies.clear()
                        // 重新订阅原来数据
                        subscribe(copyBodies, true)

                        listener?.onFailure(exception)
                        subscribeListener?.onFailure(IMqttListener.ACTION.SUBSCRIBE, exception)
                    }

                })
            }

            override fun onFailure(exception: Throwable?) {
                listener?.onFailure(exception)
                subscribeListener?.onFailure(IMqttListener.ACTION.SUBSCRIBE, exception)
            }
        })

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
        internal var subscribeBodies = mutableMapOf<String, SubscribeBody>()

        // 订阅情况监听器
        internal var subscribeListener: IActionListener? = null

        // 在重新连接后，是否自动订阅
        internal var autoSubscribe: Boolean = false

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
         * @param subscribeBodies Subscribe topic and qos
         * @param subscribeListener The subscribe action listener
         * @param autoSubscribe Whether to subscribe automatically after reconnect
         * @return Builder
         */
        @JvmOverloads
        fun subscribeBodies(
            vararg subscribeBodies: SubscribeBody,
            subscribeListener: IActionListener? = null,
            autoSubscribe: Boolean = false
        ) = apply {
            this.subscribeBodies.clear()
            subscribeBodies.forEach {
                this.subscribeBodies[it.convertKey()] = it
            }
            this.subscribeListener = subscribeListener
            this.autoSubscribe = autoSubscribe
        }

        /**
         * Build MqttWrapperClient by MqttWrapperClient.Builder
         *
         * @return MqttWrapperClient
         */
        fun build(): MqttWrapperClient {

            // If androidClient is not null,
            // it means that the developer wants to use the MqttAndroidClient created by himself
            if (androidClient != null) {
                return MqttWrapperClient(this)
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
            return MqttWrapperClient(this)
        }
    }
}