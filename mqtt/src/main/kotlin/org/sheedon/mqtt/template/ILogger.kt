package org.sheedon.mqtt.template

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