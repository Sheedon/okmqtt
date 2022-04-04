package org.sheedon.mqtt.internal.binder

import org.sheedon.mqtt.IBack
import org.sheedon.mqtt.internal.connection.RealCall
import org.sheedon.mqtt.internal.connection.RealObservable

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
     * @param back 内部Callback
     * @return String 实际订阅的结果ID,Long 超时时间
     */
    fun subscribe(
        call: RealCall,
        back: IBack
    ): Pair<Long, Long>


    /**
     * 订阅一个Callback
     *
     * @param observable 请求或监听对象，其一为了响应时反馈请求或订阅信息，其二用于提取关键字，超时时间间隔
     * @param back 内部Callback
     * @return String 实际订阅的结果ID集合
     */
    fun subscribe(
        observable: RealObservable,
        back: IBack
    ): List<Long>

    /**
     * 取消订阅一个Callback
     * 将 subscribe 返回的id作为绑定ID去取消订阅
     */
    fun unsubscribe(vararg id: Long)

}