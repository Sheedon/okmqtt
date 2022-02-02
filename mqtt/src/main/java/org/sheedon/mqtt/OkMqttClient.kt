package org.sheedon.mqtt

import android.content.Context
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.sheedon.mqtt.listener.*
import org.sheedon.rr.core.*
import org.sheedon.rr.dispatcher.Dispatcher
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
) {

    private val mqttRRBinderClient: MqttRRBinderClient = builder.mqttRRBinderClient!!
    val mqttClient: MqttWrapperClient = builder.mqttClient!!

    init {
        val requestAdapter = mqttRRBinderClient.dispatchManager.requestAdapter()
        requestAdapter!!.bindSender(mqttClient)

        mqttClient.bindDispatchAdapter(mqttRRBinderClient.switchMediator)

    }

    /**
     * 创建请求响应的Call
     *
     * @param request 请求对象
     * @return Call 用于执行入队/提交请求的动作
     */
    fun newCall(request: IRequest<String, RequestBody>): Call {
        return mqttRRBinderClient.newCall(request)
    }

    /**
     * 创建信息的观察者 Observable
     *
     * @param request 请求对象
     * @return Observable 订阅某个主题，监听该主题的消息
     */
    fun newObservable(request: IRequest<String, RequestBody>): Observable {
        return mqttRRBinderClient.newObservable(request)
    }

    /**
     * 重连操作
     */
    @JvmOverloads
    fun reConnect(
        listener: IResultActionListener? = null
    ) {
        this.mqttClient.reConnect(listener)
    }

    @JvmOverloads
    fun disConnect(listener: IResultActionListener? = null) {
        this.mqttClient.disConnect(listener)
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
        this.mqttClient.subscribe(body, attachRecord, listener)
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
        this.mqttClient.subscribe(bodies, attachRecord, listener)
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
        this.mqttClient.unsubscribe(body, attachRecord, listener)
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
        this.mqttClient.unsubscribe(bodies, attachRecord, listener)
    }

    /**
     * 重新订阅mqtt主题
     *
     * @param bodies mqtt消息体集合
     * @param listener 操作监听器
     */
    @JvmOverloads
    fun reSubscribe(
        bodies: List<SubscribeBody>,
        listener: IResultActionListener? = null
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
         * 设置基础主题，后续添加的topic 则在此基础上拼接
         *
         * @param baseTopic 基础主题
         * @return Builder 构建者
         */
        fun baseTopic(baseTopic: String) = apply {
            this.mqttRRBinderBuilder.baseTopic(baseTopic)
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
         * 设置用于设置策略和执行异步请求的调度程序。不能为null。
         *
         * @param dispatcher 请求响应执行者
         */
        fun dispatcher(dispatcher: Dispatcher<String, String, RequestBody, ResponseBody>) = apply {
            this.mqttRRBinderBuilder.dispatcher(dispatcher)
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
         * 设置行为服务环境集合
         *
         * @param behaviorServices 执行服务环境集合
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun behaviorServices(behaviorServices: MutableList<EventBehavior>) = apply {
            this.mqttRRBinderBuilder.behaviorServices(behaviorServices)
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
         * 设置事件管理集合
         *
         * @param eventManagerPool 事件管理集合
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun eventManagerPool(eventManagerPool: MutableList<EventManager<String, String, RequestBody, ResponseBody>>) =
            apply {
                this.mqttRRBinderBuilder.eventManagerPool(eventManagerPool)
            }

        /**
         * 设置事件管理
         *
         * @param eventManager 事件管理
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun eventManager(eventManager: EventManager<String, String, RequestBody, ResponseBody>) =
            apply {
                this.mqttRRBinderBuilder.eventManager(eventManager)
            }

        /**
         * 设置超时处理者
         *
         * @param timeoutManager 事件管理
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun timeoutManager(timeoutManager: TimeoutManager<String>) = apply {
            this.mqttRRBinderBuilder.timeoutManager(timeoutManager)
        }

        /**
         * 设置调度调度适配者
         *
         * @param dispatchAdapter 调度适配者
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun dispatchAdapter(dispatchAdapter: DispatchAdapter<RequestBody, ResponseBody>) = apply {
            this.mqttRRBinderBuilder.dispatchAdapter(dispatchAdapter)
        }

        /**
         * 设置请求适配者
         *
         * @param requestAdapter 请求适配者
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun requestAdapter(requestAdapter: RequestAdapter<RequestBody>) = apply {
            this.mqttRRBinderBuilder.requestAdapter(requestAdapter)
        }

        /**
         * 设置反馈主题转换器集合
         *
         * @param backTopicConverters 反馈主题转换器集合
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun backTopicConverters(backTopicConverters: MutableList<DataConverter<ResponseBody, String>>) =
            apply {
                this.mqttRRBinderBuilder.backTopicConverters(backTopicConverters)
            }

        /**
         * 设置反馈主题转换器
         *
         * @param backTopicConverter 反馈主题转换器
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun addBackTopicConverter(backTopicConverter: DataConverter<ResponseBody, String>) =
            apply {
                this.mqttRRBinderBuilder.addBackTopicConverter(backTopicConverter)
            }

        /**
         * 设置响应调度适配者
         *
         * @param responseAdapter 响应调度适配者
         * @return Builder<BackTopic></BackTopic>, ID>
         */
        fun responseAdapter(responseAdapter: ResponseAdapter<String, ResponseBody>) = apply {
            this.mqttRRBinderBuilder.responseAdapter(responseAdapter)
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
         * @param subscribeBodies Subscribe topic and qos
         * @param subscribeListener The subscribe action listener
         * @param autoSubscribe Whether to subscribe automatically after reconnect
         * @return Builder
         */
        @JvmOverloads
        fun subscribeBodies(
            subscribeListener: IActionListener? = null,
            autoSubscribe: Boolean = false,
            vararg subscribeBodies: SubscribeBody
        ) = apply {
            this.mqttBuilder.subscribeBodies(subscribeListener, autoSubscribe, *subscribeBodies)
        }

        fun build(): OkMqttClient {
            if (mqttRRBinderClient == null) {
                mqttRRBinderClient = mqttRRBinderBuilder.build()
            }
            if (mqttClient == null) {
                mqttClient = mqttBuilder.build()
            }

            return OkMqttClient(this)
        }
    }

}