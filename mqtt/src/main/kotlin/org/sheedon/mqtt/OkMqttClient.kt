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
import org.sheedon.mqtt.MqttWrapperClient.Companion.EXECUTE_INTERVAL
import org.sheedon.mqtt.internal.DataConverter
import org.sheedon.mqtt.internal.binder.*
import org.sheedon.mqtt.internal.binder.MqttRequestHandler
import org.sheedon.mqtt.internal.concurrent.EventBehavior
import org.sheedon.mqtt.internal.concurrent.EventBehaviorService
import org.sheedon.mqtt.listener.*
import org.sheedon.mqtt.utils.ILogger
import org.sheedon.mqtt.utils.Logger
import org.sheedon.rr.timeout.OnTimeOutListener
import org.sheedon.rr.timeout.TimeoutManager
import org.sheedon.rr.timeout.android.TimeOutHandler

/**
 * Factory for [calls][Call], which can be used to send MQTT requests and read their responses,
 * that is facade client class, including "request response client" and "mqtt dispatch client".
 *
 * ## OkMqttClient Should Be Shared
 *
 * Use `new OkMqttClient.Builder()` to create a shared instance with custom settings:
 *
 * ```java
 * // The singleton MQTT client.
 * public final OkMqttClient client = new OkMqttClient.Builder()
 *     .clientInfo(context, serverUri, clientId)
 *     .setLogger(new MqttLoggingLogger())
 *     .build();
 * ```
 *
 * ## Customize Your Client With newBuilder()
 *
 * You can customize a shared OkMqttClient instance with [newBuilder]. This builds a client that
 * shares the same connection pool, thread pools, and configuration. Use the builder methods to
 * add configuration to the derived client for a specific purpose.
 *
 * This example shows a call with submit mqtt message's payload by charsetName and 5 seconds request
 * timeout. Original configuration is kept, but can be overridden.
 *
 * ```java
 * OkHttpClient eagerClient = client.newBuilder()
 *     .charsetName(Charsets.UTF_8.displayName())
 *     .messageTimeout(5)
 *     .build();
 *
 * Request request = new Request.Builder()
 *     .subscribeTopic("get_manager_list")
 *     .topic("classify/device/recyclable/data/test")
 *     .data(message)
 *     .build();
 *
 * // request
 * Call call = client.newCall(request);
 * call.enqueue(new Callback() {
 *     @Override
 *     public void onResponse(@NonNull Call call, @NonNull Response response) {
 *          // call response
 *     }
 *
 *     @Override
 *     public void onFailure(@Nullable Throwable e) {
 *          // call mqtt fail response
 *     }
 * });
 *
 * // subscription
 * Observable observable = client.newObservable(request);
 * observable.enqueue(new Callback() {
 *     @Override
 *     public void onResponse(@NonNull Call call, @NonNull Response response) {
 *          // call response
 *     }
 *
 *     @Override
 *     public void onFailure(@Nullable Throwable e) {
 *          // call mqtt fail response
 *     }
 * });
 * ```
 *
 * ## Shutdown Isn't Necessary by request Call,Shutdown is Necessary by request Observable.
 *
 * The threads and connections that are held will be released automatically if they remain idle. But
 * if you are writing a application that needs to aggressively release unused resources you may do
 * so.
 *
 * ```java
 * observable.cancel();
 * ```
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 10:40 上午
 */
class OkMqttClient internal constructor(
    builder: Builder
) : MqttFactory {

    // MQTT request and response bind client
    private val mqttRRBinderClient: MqttRRBinderClient = builder.mqttRRBinderClient!!

    // MQTT client wrapper class
    private val mqttClient: MqttWrapperClient = builder.mqttClient!!

    constructor() : this(Builder())

    init {
        // attach MqttWrapperClient to requestHandler
        val requestHandler = mqttRRBinderClient.requestHandler()
        if (requestHandler is MqttRequestHandler) {
            requestHandler.attachClient(mqttClient)
        }
    }

    /**
     * Gets the request timeout value.
     * This value, measured in seconds,defines the maximum time interval
     * the request will wait for the network callback to the MQTT Message response to be established.
     */
    fun getDefaultTimeout(): Int {
        return mqttRRBinderClient.timeout
    }

    /**
     * Create a call for a request-response
     *
     * @param request request object
     * @return Call The action used to perform the enqueue submit request
     */
    override fun newCall(request: Request): Call {
        return mqttRRBinderClient.newCall(request)
    }

    /**
     * An observer Observable that creates information
     * Single topic subscription
     *
     * @param request request object
     * @return Observable Subscribe to a topic and listen for messages from that topic
     */
    override fun newObservable(request: Request): Observable {
        return mqttRRBinderClient.newObservable(request)
    }

    /**
     * An observer Observable that creates information
     * Multi-topic, subscription to the same result
     *
     * @param subscribe subscribe object
     * @return Observable Subscribe to a topic and listen for messages from that topic
     */
    override fun newObservable(subscribe: Subscribe): Observable {
        return mqttRRBinderClient.newObservable(subscribe)
    }

    /**
     * Reconnects to an MQTT server.
     * The frequency of connecting mqtt is [EXECUTE_INTERVAL].
     *
     * @param listener
     *            optional listener that will be notified when the connect
     *            completes. Use null if not required.
     * @throws MqttException
     *             for any connected problems
     * @see [mqttClient.reConnect]
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
     * Subscribe to a topic, which may include wildcards.
     *
     * @param body  wrapper mqtt topic body,
     *              include topic,qos,userContext
     * @param listener subscribe listener,
     *                 optional listener that will be notified when the subscribe result.
     */
    @JvmOverloads
    fun subscribe(
        body: Topics,
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.subscribe(body, listener)
    }

    /**
     * Subscribes to multiple topics, each topic may include wildcards.
     * <p>
     * Provides an optimized way to subscribe to multiple topics compared to
     * subscribing to each one individually.
     * </p>
     * @param bodies
     *            one or more topics to subscribe to, which can include
     *            wildcards
     * @param listener
     *            optional listener that will be notified when subscribe has
     *            completed
     */
    @JvmOverloads
    fun subscribe(
        bodies: List<Topics>,
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.subscribe(bodies, listener)
    }

    /**
     * Requests the server to unsubscribe the client from a topics.
     *
     * @param bodies
     *            one or more topics to subscribe to, which can include
     *            wildcards
     * @param listener
     *            optional listener that will be notified when unsubscribe has
     *            completed
     */
    @JvmOverloads
    fun unsubscribe(
        body: Topics,
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.unsubscribe(body, listener)
    }

    /**
     * Requests the server to unsubscribe the client from one or more topics.
     * <p>
     * Unsubcribing is the opposite of subscribing. When the server receives the
     * unsubscribe request it looks to see if it can find a matching
     * subscription for the client and then removes it. After this point the
     * server will send no more messages to the client for this subscription.
     * </p>
     * <p>
     * The topic(s) specified on the unsubscribe must match the topic(s)
     * specified in the original subscribe request for the unsubscribe to
     * succeed
     * </p>
     * @param bodies
     *            one or more topics to subscribe to, which can include
     *            wildcards
     * @param listener
     *            optional listener that will be notified when subscribe has
     *            completed
     */
    @JvmOverloads
    fun unsubscribe(
        bodies: List<Topics>,
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.unsubscribe(bodies, listener)
    }

    /**
     * Resubscribe to mqtt topic
     * 1. Cancel original subscription
     * 2. Record the original subscription information for recovery processing in case of subscription failure,
     * and subscribe to the current configuration information.
     * 3. If the cancellation in the first step fails, the feedback subscription fails.
     * 4. If the second subscription fails, the feedback subscription fails and the current data is restored.
     * Otherwise the subscription is successful
     *
     * @param bodies mqtt topic body collection
     * @param listener Action listener
     */
    @JvmOverloads
    fun reSubscribe(
        bodies: List<Topics>,
        listener: IMqttActionListener? = null
    ) {
        this.mqttClient.reSubscribe(bodies, listener)
    }

    /**
     * Publishes a message to a topic on the server. Takes an
     * [MqttMessage] message and delivers it to the server at the
     * requested quality of service.
     *
     * @param topic
     *           to deliver the message to, for example "finance/stock/ibm".
     * @param message
     *           to deliver to the server
     * @return token used to track and wait for the publish to complete. The
     *         token will be passed to any callback that has been set.
     * @see [mqttClient.publish]
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
         * Unified sets the MQTTMessage's payload encoding type.
         * If [RequestBody.autoEncode] is true and [RequestBody.charset] is null,
         * set MQTTMessage's payload encoding type by this value.
         * The default charsetName is `UTF-8`.
         *
         * @param charsetName Character set encoding type
         */
        fun charsetName(charsetName: String) = apply {
            this.mqttRRBinderBuilder.charsetName(charsetName)
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
            if (timeout < 0) return this
            this.mqttRRBinderBuilder.messageTimeout(timeout)
        }

        /**
         * Sets operating environment of the request and the response to use.
         * Use [behaviorService] to provide Call and Observable with a thread pool for execution.
         * The default behaviorService is [EventBehaviorService]
         *
         * @param behaviorService the execution service environment to use
         */
        fun behaviorService(behaviorService: EventBehavior) = apply {
            this.mqttRRBinderBuilder.behaviorService(behaviorService)
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
            this.mqttRRBinderBuilder.timeoutManager(timeoutManager)
        }

        /**
         * Sets MQTT event handler.
         * Use [requestHandler] to execute publish MQTT Message,subscribe and unsubscribe MQTT Topic.
         * The default requestHandler is [MqttRequestHandler], it agent [MqttWrapperClient] executes
         * the events of publish MQTT Message,subscribe and unsubscribe MQTT Topic.
         *
         * @param requestHandler MQTT event request handler
         */
        fun requestAdapter(requestHandler: IRequestHandler) = apply {
            this.mqttRRBinderBuilder.requestHandler(requestHandler)
        }

        /**
         * Sets event bind handler to bind MQTT response and [request MQTT or subscribe MQTT Topic].
         * Use [bindHandler] to associate RealCall or RealObservable with IBack, after getting the message,
         * feedback through IBack, and also support to remove the binding relationship through the message ID.
         *
         * @param bindHandler request and response bind handler
         */
        fun bindHandler(bindHandler: IBindHandler) = apply {
            this.mqttRRBinderBuilder.bindHandler(bindHandler)
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
            this.mqttRRBinderBuilder.responseHandler(responseHandler)
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
                this.mqttRRBinderBuilder.keywordConverters(keywordConverters)
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
         * Sets whether to start the log
         *
         * @param showLog Whether to enable the log of the Mqtt scheduler
         */
        @JvmOverloads
        fun openLog(showLog: Boolean = false) = apply {
            Logger.showLog(showLog)
        }

        /**
         * Sets whether to enable display stack trace
         *
         * @param isShowStackTrace whether to show stack trace
         */
        @JvmOverloads
        fun openStackTrace(showStackTrace: Boolean = false) = apply {
            Logger.showStackTrace(showStackTrace)
        }

        /**
         * Configure a custom Logger
         *
         * @param logger Logger
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