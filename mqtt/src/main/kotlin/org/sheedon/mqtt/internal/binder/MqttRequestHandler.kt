package org.sheedon.mqtt.internal.binder

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.sheedon.mqtt.MqttWrapperClient
import org.sheedon.mqtt.RequestBody
import org.sheedon.mqtt.Topics
import org.sheedon.mqtt.utils.Logger
import java.nio.charset.Charset

/**
 * mqtt request handler
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/4 9:50 下午
 */
internal class MqttRequestHandler internal constructor(
    private val charsetName: String, // 编码类型
) : IRequestHandler {

    private var client: MqttWrapperClient? = null

    internal fun attachClient(client: MqttWrapperClient) {
        this.client = client
    }


    /**
     * Verify the request object data, splicing and converting the corresponding format in the current class,
     * and delivering it to the publish method for use
     *
     * @param data object to real request data
     * @return object to real request data
     */
    override fun checkRequestData(data: RequestBody): RequestBody {
        if (data.autoEncode && charsetName.isNotEmpty()) {
            data.payload = data.data.toByteArray(Charset.forName(charsetName))
        }

        Logger.info("checkRequestData (data is $data)")
        return data
    }

    /**
     * Publishes a message to a topic on the mqttClient.
     */
    override fun publish(topic: String, message: MqttMessage): IMqttDeliveryToken {
        return this.client?.publish(topic, message) ?: MqttDeliveryToken()
    }

    /**
     * subscribe topics group
     *
     * @param body topics group
     * @param listener mqtt action listener
     */
    override fun subscribe(vararg body: Topics, listener: IMqttActionListener?) {
        this.client?.subscribe(body.toList(), listener)
    }

    /**
     * unsubscribe topics group
     *
     * @param body topics group
     * @param listener mqtt action listener
     */
    override fun unsubscribe(vararg body: Topics, listener: IMqttActionListener?) {
        this.client?.unsubscribe(body.toList(), listener)
    }


}