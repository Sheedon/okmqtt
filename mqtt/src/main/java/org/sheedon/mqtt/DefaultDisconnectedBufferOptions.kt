package org.sheedon.mqtt

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions

/**
 * 断开的缓冲区选项
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:46 下午
 */
class DefaultDisconnectedBufferOptions private constructor() : DisconnectedBufferOptions() {

    companion object {
        private var theOptions: DefaultDisconnectedBufferOptions? = null
        /**
         * 获取断开的缓冲区选项
         *
         * @return DisconnectedBufferOptions
         */
        /**
         * 设置默认值
         *
         * @param options 选项
         */
        @JvmStatic
        var default: DefaultDisconnectedBufferOptions?
            get() {
                synchronized(DefaultDisconnectedBufferOptions::class.java) {
                    if (theOptions == null) {
                        theOptions = DefaultDisconnectedBufferOptions()
                    }
                }
                return theOptions
            }
            set(options) {
                synchronized(DefaultDisconnectedBufferOptions::class.java) { theOptions = options }
            }
    }

    // 设置基础值
    init {

        // 设置缓冲区启用
        isBufferEnabled = true
        // 缓冲区大小
        bufferSize = 100
        // 设置持久缓冲区
        isPersistBuffer = false
        // 设置删除最早的消息
        isDeleteOldestMessages = false
    }
}