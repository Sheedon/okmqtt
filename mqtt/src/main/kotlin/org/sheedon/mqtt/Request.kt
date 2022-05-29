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

import androidx.annotation.IntRange

/**
 * It is designed to construct an mqtt message sending object, subscription object, or an object
 * combined into a request response, the data consists of the response association object and the
 * request body.
 *
 * MQTT request data (topic, payload, qos, retained) can be constructed through [Builder.topic],
 * [Builder.data], [Builder.charset] or [Builder.body].
 *
 * Besides message supports both responsive request, unresponsive requests and subscription response.
 * If the message needs to be monitored by response, you can associate it by configuring [relation].
 *
 * There are two ways to associate:
 *
 * One is to configure the [Topics.topic] parameter in [Relation.topics],
 * which extends the logic of MQTT to subscribe messages through topic,
 * The subscription topic is consistent with the target topic, or the subscription topic uses wildcards,
 * and the target topic is within the coverage of the wildcard, that is, the association is completed.
 *
 * Other is the configuration parameter [Relation.keyword]. Developers can add a keyword conversion
 * strategy in [OkMqttClient.Builder.keywordConverter] as the keyword matching logic.
 *
 * For example, the developer uses the last topic level of the subscription topic as the key.
 * The subject of the response message: mq/cloud/test_ack,
 * In [DataConverter.convert] intercept the content test_ack after the last '/',
 * subscribe keywords: new Request.Builder().keyword("test_ack"),
 * then the response is associated with the request or subscription.
 *
 * And if [Relation.timeout] is not set, the global timeout is used by default.
 * Otherwise, the timeout duration is set according to the custom timeout.
 *
 * Use `new Request.Builder()` to create an object with custom settings:
 *
 * No response to request message.
 * MQTT topic is「classify/device/recyclable/data/test」.
 * MQTT data is「"{\"message\":\"EMPTY\"}"」.
 * MQTT qos is 0, qos defaults to 0, you can not configure qos(0).
 *
 * ```java
 * Request request = new Request.Builder()
 *     .topic("classify/device/recyclable/data/test")
 *     .data("{\"message\":\"EMPTY\"}")
 *     .build();
 * ```
 *
 * This is a response request message
 * MQTT topic is「classify/device/recyclable/data/test」.
 * MQTT data is「"{\"message\":\"EMPTY\"}"」.
 * MQTT qos is 0, qos defaults to 0, you can not configure qos(0).
 * MQTT response topic is 「classify/cloud/recyclable/cmd/test」.
 *
 * ```java
 * Request request = new Request.Builder()
 *     .topic("classify/device/recyclable/data/test")
 *     .data("{\"message\":\"EMPTY\"}")
 *     .subscribeTopic("classify/cloud/recyclable/cmd/test")
 *     .build();
 * ```
 *
 * This is a response request message, and response by keyword,
 * you need add DataConverter in OkMqttClient, that used to convert data into keywords.
 * Mqtt topic is「classify/device/recyclable/data/test」.
 * Mqtt data is「"{\"message\":\"EMPTY\"}"」.
 * Mqtt qos is 0, qos defaults to 0, you can not configure qos(0).
 * Mqtt response keyword is「test_ack」.
 *
 * ```java
 * Request request = new Request.Builder()
 *     .topic("classify/device/recyclable/data/test")
 *     .data("{\"message\":\"EMPTY\"}")
 *     .keyword("test_ack")
 *     .build();
 * ```
 *
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 10:58 上午
 */
class Request internal constructor(
    @get:JvmName("relation") val relation: Relation,
    @get:JvmName("body") val body: RequestBody,
) {

    override fun toString(): String = buildString {
        append("Request{body=")
        append(body)
        append(",relation=")
        append(relation)
        append('}')
    }

    open class Builder {
        internal var topic: String = ""
        internal var data: String = ""
        internal var qos: Int = 0
        internal var retained: Boolean = false
        internal var charset: String? = null
        internal var autoEncode: Boolean = true
        internal var relation: Relation.Builder = Relation.Builder()
        internal var body: RequestBody? = null

        /**
         * Sets MQTT topic and MQTT message basic configuration.
         *
         * TOPIC:
         * The mqtt client must have a topic for subscription and publication.
         * Only after subscribing to a topic can it receive the payload of
         * the corresponding topic and communicate.
         *
         * QOS:
         * The quality of service for this message, either 0, 1, or 2.
         * see [org.eclipse.paho.client.mqttv3.MqttMessage.setQos]
         *
         * RETAINED:
         * Whether or not the publish message should be retained by the messaging engine.
         * Sending a message with retained set to <code>true</code> and with an empty
         * byte array as the payload e.g. <code>new byte[0]</code> will clear the
         * retained message from the server.  The default value is <code>false</code>
         * see [org.eclipse.paho.client.mqttv3.MqttMessage.setRetained]
         *
         * @param topic the topic to subscribe to, which can include wildcards.
         * @param qos the maximum quality of service at which to subscribe. Default value is 0.
         * @param retained whether or not the messaging engine should retain the message.
         */
        @JvmOverloads
        open fun topic(
            topic: String,
            @IntRange(from = 0, to = 2) qos: Int = 0,
            retained: Boolean = false
        ) = apply {
            this.topic = topic
        }

        /**
         * Sets the payload of this message to be the request data.
         *
         * @param data the data for this message
         */
        open fun data(data: String) = apply {
            this.data = data
        }

        /**
         * Sets the payload of this message to be the specified byte array.
         *
         * @param data the data for this message.
         */
        open fun data(data: ByteArray) = apply {
            this.data = data.toString()
        }

        /**
         * Sets the payload to the specified charset.
         *
         * When sending the payload of message, the receiver may only receive the data set in the
         * specified character format, so the sender needs to change the character format so that
         * the receiver can process the readable content.
         * if the charset is empty, the byte array of data is set to the payload, and if autoEncode == true,
         * the globally set charset is set to the payload before the request is sent.
         * Otherwise, set the current charset directly, and set auto Encode to false.
         * Before submitting, No longer reset the charset of the payload by global charset.
         *
         * @param charset charset of payload
         * @param autoEncode whether to use automatic encoding
         */
        @JvmOverloads
        open fun charset(charset: String, autoEncode: Boolean = true) = apply {
            this.charset = charset
            this.autoEncode = autoEncode
        }

        /**
         * Sets request body for constructing MQTT-Topic and MqttMessage.
         * see [RequestBody].
         *
         * @param body Contains Mqtt-topic and MqttMessage
         */
        open fun body(body: RequestBody) = apply {
            this.body = body
        }

        /**
         * Sets the topic to subscribe to to receive response messages.
         *
         * Only after subscribing to a topic can it receive the payload of
         * the corresponding topic and communicate.
         * So subscribeTopic is used to subscribe to the topic, and qos is the message quality of
         * the subscribed topic.
         * When attachRecord is true, the topic will be recorded in the subscription message pool.
         * If mqtt is re-subscribed, automatic subscription can be realized unless the developer
         * has canceled the subscription.
         *
         * subscriptionType is used to define whether this topic is a remote subscription or
         * a local subscription. If it is a remote subscription, you need to implement the mqtt
         * subscription behavior to complete the local subscription. If it is a local subscription,
         * there is no need for mqtt subscription, but the topic is kept locally for association.
         * When the response message is returned from the server to the client, it only tries to
         * match the message and distributes it internally.
         *
         * @param subscribeTopic Topic for subscribing to mqtt messages
         * @param qos Quality of subscription mqtt messages
         * @param userContext optional object used to pass context to the callback.
         *                  Use null if not required. Default value is null.
         * @param attachRecord Whether to keep subscription records in the cache pool
         * @param subscriptionType The request type, which limits the scope of subscription messages
         */
        @JvmOverloads
        open fun subscribeTopic(
            subscribeTopic: String,
            @IntRange(from = 0, to = 2) qos: Int = 0,
            userContext: Any? = null,
            attachRecord: Boolean = false,
            subscriptionType: SubscriptionType = SubscriptionType.REMOTE
        ) = apply {
            relation.topics(
                Topics(
                    subscribeTopic,
                    qos,
                    userContext,
                    Headers(attachRecord, subscriptionType)
                )
            )
        }

        /**
         * Sets the keyword of the response message
         *
         * If the field is not "", it means that the correlation field is used as the matching field
         * of the response message for correlation; otherwise, the subscription topic in the relation is taken
         *
         * Note: It is hoped that in the case of subscribing to the same topic, the short-term
         * request will use the topic uniformly, or use the keyword instead of mixing
         * (sometimes there is no keyword). In this case, there is a response error.
         *
         * @param keyword the keyword of the response message
         */
        open fun keyword(keyword: String) = apply {
            relation.keyword(keyword)
        }

        /**
         * Sets the request timeout value.
         * This value, measured in millisecond,defines the maximum time interval
         * the request will wait for the network callback to the MQTT Message response to be established.
         *
         * @param delayMilliSecond request timeout value (millisecond)
         */
        open fun delayMilliSecond(delayMilliSecond: Long) = apply {
            relation.delayMilliSecond(delayMilliSecond)
        }

        /**
         * Sets the request timeout value.
         * This value, measured in seconds,defines the maximum time interval
         * the request will wait for the network callback to the MQTT Message response to be established.
         *
         * @param delayMilliSecond request timeout value (seconds)
         */
        open fun delaySecond(delaySecond: Int) = apply {
            relation.delaySecond(delaySecond)
        }

        /**
         * Sets configuration relation.
         */
        open fun relation(relationBuilder: Relation.Builder) = apply {
            relation = relationBuilder
        }

        open fun build(): Request {
            if (body == null) {
                body(RequestBody(topic, data, qos, retained, charset))
            }

            return Request(relation.build(), body!!)
        }


    }
}