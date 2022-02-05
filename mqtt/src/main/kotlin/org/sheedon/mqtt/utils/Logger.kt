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

import org.sheedon.mqtt.template.ILogger

/**
 * Logger wrapper class
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/3 12:40 下午
 */
object Logger : ILogger {

    private var logger: ILogger? = null

    override fun isMonitorMode() = logger?.isMonitorMode() ?: false

    override fun getDefaultTag() = logger?.getDefaultTag()

    fun setLogger(logger: ILogger) {
        this.logger = logger
    }

    override fun showLog(isShowLog: Boolean) {
        if (logger == null) {
            setLogger(MqttLogger())
        }
        logger?.showLog(isShowLog)
    }

    override fun showStackTrace(isShowStackTrace: Boolean) {
        if (logger == null) {
            setLogger(MqttLogger())
        }
        logger?.showStackTrace(isShowStackTrace)
    }

    fun debug(message: String?) {
        debug(logger?.getDefaultTag(), message)
    }

    override fun debug(tag: String?, message: String?) {
        logger?.debug(tag, message)
    }

    fun info(message: String?) {
        info(logger?.getDefaultTag(), message)
    }

    override fun info(tag: String?, message: String?) {
        logger?.info(tag, message)
    }

    fun warning(message: String?) {
        warning(logger?.getDefaultTag(), message)
    }

    override fun warning(tag: String?, message: String?) {
        logger?.warning(tag, message)
    }

    fun error(message: String?) {
        error(logger?.getDefaultTag(), message)
    }

    override fun error(tag: String?, message: String?) {
        logger?.error(tag, message)
    }

    fun error(message: String?, e: Throwable?) {
        error(logger?.getDefaultTag(), message, e)
    }

    override fun error(tag: String?, message: String?, e: Throwable?) {
        logger?.error(tag, message, e)
    }

    override fun monitor(message: String?) {
        logger?.monitor(message)
    }
}