package org.sheedon.mqtt

import org.sheedon.rr.core.Call

/**
 * A call is a request that is ready to execute.
 * Since the object represents a single request-response pair (stream), it cannot be executed twice.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 7:43 下午
 */
interface Call : Call<String, RequestBody, ResponseBody> {

    /**
     * Use the Callback in the protocol, you can also use the default enqueue (RRCallback callback),
     * but too many generics are displayed
     *
     * @param callback Callback with request
     */
    fun enqueue(callback: Callback?)

}