package org.sheedon.mqtt.internal.binder

import org.sheedon.mqtt.ICallback
import org.sheedon.mqtt.Request

/**
 * 绑定者处理者职责
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/20 5:44 下午
 */
interface IBindHandler {

    /**
     * 订阅一个Callback
     *
     * @param request 请求对象，主要用于提取关键字，超时时间间隔
     * @param callback 内部Callback
     * @return String 实际订阅的结果ID
     */
    fun subscribe(request: Request, callback: ICallback): String

    /**
     * 取消订阅一个Callback
     * 将 subscribe 返回的id作为绑定ID去取消订阅
     */
    fun unsubscribe(bindId: String)

}