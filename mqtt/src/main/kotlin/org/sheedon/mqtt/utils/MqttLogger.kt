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

import android.util.Log
import java.lang.StringBuilder

/**
 * mqtt logger
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/3 12:04 下午
 */
class MqttLogger : ILogger {

    private val defaultTag = "MqttDispatcher"

    companion object {
        private var isShowLog = false
        private var isShowStackTrace = false
        private var isMonitorMode = false

        fun getExtInfo(stackTraceElement: StackTraceElement): String {
            if (isShowStackTrace) {
                val separator = " & "
                val sb = StringBuilder("[")
                val threadName = Thread.currentThread().name
                val fileName = stackTraceElement.fileName
                val className = stackTraceElement.className
                val methodName = stackTraceElement.methodName
                val threadID = Thread.currentThread().id
                val lineNumber = stackTraceElement.lineNumber
                sb.append("ThreadId=").append(threadID).append(separator)
                sb.append("ThreadName=").append(threadName).append(separator)
                sb.append("FileName=").append(fileName).append(separator)
                sb.append("ClassName=").append(className).append(separator)
                sb.append("MethodName=").append(methodName).append(separator)
                sb.append("LineNumber=").append(lineNumber)
                sb.append(" ] ")
                return sb.toString()
            }
            return ""
        }
    }

    override fun showLog(showLog: Boolean) {
        isShowLog = showLog
    }

    override fun showStackTrace(showStackTrace: Boolean) {
        isShowStackTrace = showStackTrace
    }

    override fun debug(tag: String?, message: String?) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.d(
                if (tag.isNullOrEmpty()) getDefaultTag() else tag,
                message + getExtInfo(stackTraceElement)
            )
        }
    }

    override fun info(tag: String?, message: String?) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.i(
                if (tag.isNullOrEmpty()) getDefaultTag() else tag,
                message + getExtInfo(stackTraceElement)
            )
        }
    }

    override fun warning(tag: String?, message: String?) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.w(
                if (tag.isNullOrEmpty()) getDefaultTag() else tag,
                message + getExtInfo(stackTraceElement)
            )
        }
    }

    override fun error(tag: String?, message: String?) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.e(
                if (tag.isNullOrEmpty()) getDefaultTag() else tag,
                message + getExtInfo(stackTraceElement)
            )
        }
    }

    override fun error(tag: String?, message: String?, e: Throwable?) {
        if (isShowLog) {
            Log.e(
                if (tag.isNullOrEmpty()) getDefaultTag() else tag,
                message, e
            )
        }
    }

    override fun monitor(message: String?) {
        if (isShowLog && isMonitorMode()) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.d("$defaultTag::monitor", message + getExtInfo(stackTraceElement))
        }
    }

    override fun isMonitorMode(): Boolean {
        return isMonitorMode
    }

    override fun getDefaultTag(): String {
        return defaultTag
    }
}