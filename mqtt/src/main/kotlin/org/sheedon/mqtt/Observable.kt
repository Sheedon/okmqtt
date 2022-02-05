package org.sheedon.mqtt

import org.sheedon.rr.core.Observable

/**
 * Observations are messages for which subscriptions are specified. The message can be cancelled.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 8:12 下午
 */
interface Observable : Observable<String, RequestBody, ResponseBody> {

    /**
     * Use the Callback in the protocol, you can also use the default subscribe (RRCallback callback),
     * but the generic type is too much
     *
     * @param callback Callback
     */
    fun subscribe(callback: Callback?)

}