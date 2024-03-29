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

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage

/**
 * Callback information
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 7:57 下午
 */

/* 请求响应结果反馈 */
interface Callback : IBack {

    /**
     * 当请求成功返回，从而响应时调用。
     */
    fun onResponse(call: Call, response: Response)
}

interface ObservableBack : IBack {

    /**
     * 当请求成功返回，从而响应时调用。
     */
    fun onResponse(observable: Observable, response: Response)
}

/* 订阅动作的结果反馈 */
interface SubscribeBack : IBack {

    /**
     * 当请求成功返回，从而响应时调用。
     */
    fun onResponse(response: MqttWireMessage?)
}

/* 订阅+响应 */
interface FullCallback : ObservableBack, SubscribeBack

interface IBack {
    /**
     * 当请求由于取消、连接问题或超时而无法执行时调用。
     * 由于网络可能在交换期间发生故障，因此远程服务器可能在故障之前接受了请求。
     */
    fun onFailure(e: Throwable?)
}