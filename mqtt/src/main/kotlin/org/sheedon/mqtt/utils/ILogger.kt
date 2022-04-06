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
package org.sheedon.mqtt.utils

/**
 * MQTT logger
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/3 12:02 下午
 */
interface ILogger {

    fun showLog(isShowLog: Boolean)

    fun showStackTrace(isShowStackTrace: Boolean)

    fun debug(tag: String?, message: String?)

    fun info(tag: String?, message: String?)

    fun warning(tag: String?, message: String?)

    fun error(tag: String?, message: String?)

    fun error(tag: String?, message: String?, e: Throwable?)

    fun monitor(message: String?)

    fun isMonitorMode(): Boolean

    fun getDefaultTag(): String?
}