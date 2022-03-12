package org.sheedon.mqtt

/**
 * mqtt newCall/newObservable Factory
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/13 9:56 下午
 */
interface MqttFactory : CallFactory, ObservableFactory, ListenFactory

interface CallFactory {
    fun newCall(request: Request): Call
}

interface ObservableFactory {
    fun newObservable(request: Subscribe): Observable
    fun newObservable(request: List<Subscribe>): Observable
}

interface ListenFactory {
    fun newListen(request: Request): Listener
}