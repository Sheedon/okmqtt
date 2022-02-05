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

import org.sheedon.mqtt.utils.Logger
import org.sheedon.rr.core.RequestAdapter
import java.nio.charset.Charset

/**
 * mqtt request adapter
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 2:03 下午
 */
open class MqttRequestAdapter(
    val baseTopic: String, // 反馈主题
    val charsetName: String // 编码类型
) : RequestAdapter.AbstractRequestImpl<RequestBody>() {

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

        if (baseTopic.isNotEmpty()) {
            data.topic = baseTopic + data.topic
        }
        Logger.info("checkRequestData (data is $data)")
        return data
    }

    /**
     * Publishes a message to a topic on the mqttClient.
     * and wait up to 3 seconds to process if the request is successful
     *
     * @param data object to real request data
     * @return Whether the request is successful
     */
    override fun publish(data: RequestBody): Boolean {
        if (sender == null) return false

        try {
            val token = (sender!! as MqttWrapperClient).publish(data.topic, data)
            token.waitForCompletion(3000)
        } catch (e: Exception) {
            Logger.error("publish mqtt message fail", e)
            return false
        }
        return true
    }
}