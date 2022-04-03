package org.sheedon.mqtt.internal.binder

import org.sheedon.mqtt.ICallback
import org.sheedon.mqtt.internal.concurrent.CallbackEnum
import org.sheedon.mqtt.internal.connection.RealCall

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
     * @param call 请求或监听对象，其一为了响应时反馈请求或订阅信息，其二用于提取关键字，超时时间间隔
     * @param callback 内部Callback
     * @return String 实际订阅的结果ID,Long 超时时间
     */
    fun subscribe(
        call: RealCall,
        callback: ICallback,
        type: CallbackEnum
    ): Pair<Long, Long>

    /**
     * 取消订阅一个Callback
     * 将 subscribe 返回的id作为绑定ID去取消订阅
     */
    fun unsubscribe(id: Long)

}