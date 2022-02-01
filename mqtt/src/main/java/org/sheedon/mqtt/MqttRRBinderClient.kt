package org.sheedon.mqtt

import org.sheedon.rr.core.DispatchAdapter
import org.sheedon.rr.core.IRequest
import org.sheedon.rr.dispatcher.AbstractClient
import org.sheedon.rr.dispatcher.DefaultEventManager
import org.sheedon.rr.timeout.android.TimeOutHandler
import java.lang.IllegalStateException

/**
 * RequestResponseBinder 请求响应绑定的装饰客户端类
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 12:58 下午
 */
class MqttRRBinderClient constructor(
    builder: Builder
) : AbstractClient<String/*反馈主题*/, String/*消息ID*/, RequestBody/*请求格式*/, ResponseBody/*反馈格式*/>(
    builder
) {

    internal val switchMediator: SwitchMediator =
        if (builder.loadDispatchAdapter() is SwitchMediator)
            builder.loadDispatchAdapter() as SwitchMediator
        else
            SwitchMediator(
                builder.baseTopic,
                builder.charsetName,
                builder.loadDispatchAdapter().loadRequestAdapter()
            )


    /**
     * 创建请求响应的Call
     *
     * @param request 请求对象
     * @return Call 用于执行入队/提交请求的动作
     */
    override fun newCall(request: IRequest<String, RequestBody>): Call {
        return RealCall.newCall(this, request as Request)
    }

    /**
     * 创建信息的观察者 Observable
     *
     * @param request 请求对象
     * @return Observable 订阅某个主题，监听该主题的消息
     */
    override fun newObservable(request: IRequest<String, RequestBody>): Observable {
        return RealObserver.newObservable(this, request as Request)
    }


    class Builder :
        AbstractClient.Builder<MqttRRBinderClient, String, String, RequestBody, ResponseBody>() {

        // 基础主题 用于主题拼接
        internal var baseTopic: String = ""

        // 字符集编码类型
        internal var charsetName: String = "GBK"


        internal fun loadDispatchAdapter(): DispatchAdapter<RequestBody, ResponseBody> {
            return dispatchAdapter!!
        }

        /**
         * 设置基础主题，后续添加的topic 则在此基础上拼接
         *
         * @param baseTopic 基础主题
         * @return Builder 构建者
         */
        fun baseTopic(baseTopic: String) = apply {
            this.baseTopic = baseTopic
        }

        /**
         * 设置字符集编码类型，在接收数据时转化为指定格式的字符串
         *
         * @param charsetName 字符集编码类型
         * @return Builder 构建者
         */
        fun charsetName(charsetName: String) = apply {
            this.charsetName = charsetName
        }

        override fun checkAndBind() {
            if (behaviorServices.isEmpty()) {
                behaviorServices.add(MqttEventBehaviorService())
            }
            if (eventManagerPool.isEmpty()) {
                eventManagerPool.add(DefaultEventManager())
            }
            if (timeoutManager == null) {
                timeoutManager = TimeOutHandler()
            }
            if (dispatchAdapter == null) {
                dispatchAdapter = SwitchMediator(baseTopic, charsetName, requestAdapter)
            }
            if (backTopicConverters.isEmpty()) {
                throw IllegalStateException("backTopicConverter is null.")
            }
            if (responseAdapter == null) {
                responseAdapter = MqttResponseAdapter(charsetName)
            }
        }

        override fun builder(): MqttRRBinderClient {
            return MqttRRBinderClient(this)
        }


    }
}