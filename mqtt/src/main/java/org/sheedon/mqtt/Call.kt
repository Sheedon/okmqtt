package org.sheedon.mqtt

import org.sheedon.rr.core.Call

/**
 * 调用是已准备好执行的请求。由于该对象表示单个请求响应对（流），因此不能执行两次。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 7:43 下午
 */
interface Call : Call<String, RequestBody, ResponseBody> {

    /**
     * 使用协议中的Callback ，使用默认enqueue(RRCallback callback)也可，只是泛型显示过多
     *
     * @param callback Callback
     */
    fun enqueue(callback: Callback?)

}