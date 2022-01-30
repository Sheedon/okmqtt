package org.sheedon.mqtt

import org.eclipse.paho.client.mqttv3.MqttConnectOptions

/**
 * mqtt连接选项配置类
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:42 下午
 */
internal class DefaultMqttConnectOptions private constructor() : MqttConnectOptions() {
    companion object {
        private var theOptions: DefaultMqttConnectOptions? = null
        /**
         * 获取默认值
         *
         * @return MqttConnectOptions
         */
        /**
         * 设置默认值
         *
         * @param options 配置类
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

    // 设置基础值
    init {
        // 保持在线间隔30秒
        keepAliveInterval = 30
        // 连接超时10秒
        connectionTimeout = 10
        // 设置最大吞吐量
        maxInflight = 30
        // 设置清除
        isCleanSession = true
        // 自动连接
        isAutomaticReconnect = true
    }
}