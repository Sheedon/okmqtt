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