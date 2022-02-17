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

/**
 * Basic feedback class, the content to be included includes "feedback subject" and "feedback message body"
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 11:01 上午
 */
class Response @JvmOverloads constructor(
    private val backTopic: String? = "",
    private val message: String? = "",
    private val body: ResponseBody? = null
) : IResponse<String, ResponseBody> {

    override fun backTopic(): String {
        return backTopic ?: ""
    }

    override fun message(): String {
        return message ?: ""
    }

    override fun body(): ResponseBody {
        return body ?: ResponseBody()
    }

}