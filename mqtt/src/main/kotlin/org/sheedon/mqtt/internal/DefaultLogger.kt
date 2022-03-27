package org.sheedon.mqtt.internal

import android.util.Log
import java.lang.StringBuilder


/**
 * Default Logger
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/3 9:45 上午
 */

val log = DefaultLogger.INSTANCE

class DefaultLogger private constructor() {

    companion object {
        private var defaultTag = "RR-Dispatcher"
        internal var isShowLog = false
        internal var isShowStackTrace = false

        val INSTANCE: DefaultLogger by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            DefaultLogger()
        }
    }


    fun showLog(showLog: Boolean) {
        isShowLog = showLog
    }

    fun showStackTrace(showStackTrace: Boolean) {
        isShowStackTrace = showStackTrace
    }

    fun debug(tag: String?, message: String?) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.d(
                if (tag.isNullOrEmpty()) getDefaultTag() else tag,
                message + getExtInfo(stackTraceElement)
            )
        }
    }

    fun info(tag: String?, message: String?) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.i(if (tag.isNullOrEmpty()) getDefaultTag() else tag,
                message + getExtInfo(stackTraceElement)
            )
        }
    }

    fun warning(tag: String?, message: String?) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.w(if (tag.isNullOrEmpty()) getDefaultTag() else tag,
                message + getExtInfo(stackTraceElement)
            )
        }
    }

    fun error(tag: String?, message: String?) {
        if (isShowLog) {
            val stackTraceElement = Thread.currentThread().stackTrace[3]
            Log.e(if (tag.isNullOrEmpty()) getDefaultTag() else tag,
                message + getExtInfo(stackTraceElement)
            )
        }
    }

    private fun getDefaultTag(): String {
        return defaultTag
    }

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