package org.sheedon.mqtt.internal.concurrent

import java.util.*

/**
 * 设置其线程名称的 Runnable 实现。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/8 4:09 下午
 */
abstract class NamedRunnable protected constructor(format: String, vararg args: Any?) : Runnable {
    private val name: String = String.format(Locale.US, format, *args)

    override fun run() {
        val oldName = Thread.currentThread().name
        Thread.currentThread().name = name
        try {
            execute()
        } finally {
            Thread.currentThread().name = oldName
        }
    }

    protected abstract fun execute()

}