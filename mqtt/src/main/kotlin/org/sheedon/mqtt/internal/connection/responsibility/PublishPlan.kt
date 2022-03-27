package org.sheedon.mqtt.internal.connection.responsibility

import org.sheedon.mqtt.Request
import org.sheedon.mqtt.RequestBody
import org.sheedon.mqtt.internal.IDispatchManager
import org.sheedon.mqtt.internal.connection.Plan
import org.sheedon.mqtt.internal.connection.RealCall
import org.sheedon.mqtt.internal.connection.RealPlan
import org.sheedon.mqtt.internal.log

/**
 * 发送职责环节 plan
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/27 9:48 下午
 */
class PublishPlan(
    call: RealCall,
    nextPlan: Plan?,
    request: Request,
    dispatcher: IDispatchManager
) : RealPlan(call, nextPlan, request, dispatcher) {

    private var executed = false
    private var publishId: String? = null

    override fun proceed() {
        if (call.isCanceled()) {
            log.info("Dispatcher", "publishPlan to cancel proceed by $request")
            return
        }
        log.info("Dispatcher", "publishPlan to proceed by $request")

        // 请求body 不能为空
        // 执行请求对象的核实/转换格式动作
        var body: RequestBody = request.body
            ?: throw IllegalArgumentException(
                "request's body is null by $request"
            )

        val requestHandler = dispatcher.requestHandler()
        body = requestHandler.checkRequestData(body)

        val relation = request.relation
        // 需要有绑定内容
        if (relation.keyword.isNullOrEmpty() && relation.subscribe?.topic.isNullOrEmpty()) {
            throw IllegalArgumentException(
                "request's relation bind topic or keyword is null by $request"
            )
        }


        // 添加绑定
        val callback = call.callback
        if (callback != null) {
            val bindHandler = dispatcher.bindHandler()
            publishId = bindHandler.subscribe(request, callback)
        }

        // 1. 执行提交，最多等待结果3秒，若超时，则认为请求失败，到达catch中
        // 2. 执行结束后，执行成功，并且当前状态为取消，则执行解除绑定的行为
        try {
            val token = requestHandler.publish(body.topic, body)
            token.waitForCompletion(3000)
            executed = true
        } catch (e: Exception) {
            log.error("Dispatcher", "$request publish mqtt message fail:$e")
            unBind()
            callback?.onFailure(e)
        } finally {
            if (call.isCanceled() && executed) {
                unBind()
            }
        }


    }

    /**
     * 取消提交
     */
    override fun cancel() {
        if (executed) {
            unBind()
        }
    }

    /**
     * 解除绑定
     */
    private fun unBind() {
        if (call.callback == null) {
            return
        }

        val relation = request.relation
        val bindHandler = dispatcher.bindHandler()
        val keyword = relation.keyword ?: relation.subscribe?.topic ?: ""
        bindHandler.unsubscribe(publishId ?: keyword)
    }

}