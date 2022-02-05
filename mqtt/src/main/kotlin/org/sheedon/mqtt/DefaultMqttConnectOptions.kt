package org.sheedon.mqtt

import org.eclipse.paho.client.mqttv3.MqttConnectOptions

/**
 * mqtt connection options configuration class
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:42 下午
 */
internal class DefaultMqttConnectOptions private constructor() : MqttConnectOptions() {
    companion object {
        /**
         * static Defaults
         *
         * @return MqttConnectOptions
         */
        private var theOptions: DefaultMqttConnectOptions? = null
        /**
         * Set and get default values
         *
         * @return DefaultMqttConnectOptions configuration class
         */
        @JvmStatic
        var default: DefaultMqttConnectOptions?
            get() {
                synchronized(DefaultMqttConnectOptions::class.java) {
                    if (theOptions == null) {
                        theOptions = DefaultMqttConnectOptions()
                    }
                }
                return theOptions
            }
            set(options) {
                synchronized(DefaultMqttConnectOptions::class.java) { theOptions = options }
            }
    }

    // set base value
    init {
        // keep online interval 30 seconds
        keepAliveInterval = 30
        // Connection timed out 10 seconds
        connectionTimeout = 10
        // Set maximum throughput
        maxInflight = 30
        // setting clear
        isCleanSession = true
        // auto connect
        isAutomaticReconnect = true
    }
}