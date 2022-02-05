package org.sheedon.mqtt

import org.sheedon.rr.core.IRequest
import org.sheedon.rr.core.IResponse
import org.sheedon.rr.dispatcher.RealCall
import org.sheedon.rr.core.Callback

/**
 * Real Call wrapper class for scheduling locally built enqueues
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 8:15 下午
 */
class RealCall(val realCall: RealCall<String, String, RequestBody, ResponseBody>) : Call {

    companion object {

        @JvmStatic
        fun newCall(client: MqttRRBinderClient, request: Request): Call {
            val realCall = RealCall(client, request)
            return RealCall(realCall)
        }
    }

    override fun <RRCallback : Callback<IRequest<String, RequestBody>, IResponse<String, ResponseBody>>?> enqueue(
        callback: RRCallback
    ) {
        realCall.enqueue(callback)
    }

    @Suppress("UNCHECKED_CAST")
    override fun enqueue(callback: org.sheedon.mqtt.Callback?) {
        this.enqueue(
            callback as? Callback<IRequest<String, RequestBody>,
                    IResponse<String, ResponseBody>>
        )
    }

    override fun publish() {
        realCall.publish()
    }

    override fun isCanceled(): Boolean {
        return realCall.isCanceled()
    }

    override fun cancel() {
        realCall.cancel()
    }

    override fun isExecuted(): Boolean {
        return realCall.isExecuted()
    }
}