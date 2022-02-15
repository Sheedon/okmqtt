package org.sheedon.mqtt

/**
 * mqtt newCall/newObservable Factory
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/13 9:56 下午
 */
interface MqttFactory : CallFactory, ObservableFactory

interface CallFactory {
    fun newCall(request: Request): org.sheedon.mqtt.Call
}

interface ObservableFactory {
    fun newObservable(request: Request): org.sheedon.mqtt.Observable
}