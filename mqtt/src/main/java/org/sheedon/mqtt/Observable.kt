package org.sheedon.mqtt

import org.sheedon.rr.core.Observable

/**
 * 观察是指定订阅的消息。该消息可取消。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 8:12 下午
 */
interface Observable : Observable<String, RequestBody, ResponseBody> {

    /**
     * 使用协议中的Callback ，使用默认subscribe(RRCallback callback)也可，只是泛型显示过多
     *
     * @param callback Callback
     */
    fun subscribe(callback: Callback?)

}