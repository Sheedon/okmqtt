package org.sheedon.mqtt

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions

/**
 * Default broken buffer option
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:46 下午
 */
class DefaultDisconnectedBufferOptions private constructor() : DisconnectedBufferOptions() {

    companion object {
        /**
         * Broken buffer option
         *
         * @return DisconnectedBufferOptions
         */
        private var theOptions: DefaultDisconnectedBufferOptions? = null

        /**
         * Set broken buffer option defaults
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

    // set base value
    init {

        // set buffer enabled
        isBufferEnabled = true
        // buffer size
        bufferSize = 100
        // set persistent buffer
        isPersistBuffer = false
        // set to delete oldest messages
        isDeleteOldestMessages = false
    }
}