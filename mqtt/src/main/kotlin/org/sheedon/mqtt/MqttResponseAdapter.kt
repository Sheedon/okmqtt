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

import org.sheedon.rr.core.IResponse
import org.sheedon.rr.core.ResponseAdapter
import java.nio.charset.Charset

/**
 * mqtt response adapter
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 2:09 下午
 */
internal class MqttResponseAdapter(val charsetName: String?) :
    ResponseAdapter<String, ResponseBody> {


    /**
     * Build failed response objects from topic and message
     *
     * @param topic Feedback Topics
     * @param message error message
     * @return Response Response data
     */
    @Suppress("UNCHECKED_CAST")
    override fun <Response : IResponse<String, ResponseBody>> buildFailure(
        topic: String,
        message: String
    ): Response {
        return Response(topic, message) as Response
    }

    /**
     * Build success response objects from topic and message
     *
     * @param topic Feedback Topics
     * @param body Response data
     * @return Response Response data
     */
    @Suppress("UNCHECKED_CAST")
    override fun <Response : IResponse<String, ResponseBody>> buildResponse(
        topic: String,
        body: ResponseBody
    ): Response {
        if (charsetName.isNullOrEmpty()) {
            body.data = String(body.payload)
        } else {
            val charset = Charset.forName(charsetName)
            body.data = String(body.payload, charset)
        }
        return Response(topic, body = body) as Response
    }
}