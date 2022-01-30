package org.sheedon.mqtt

import org.sheedon.rr.dispatcher.AbstractClient
import org.sheedon.rr.dispatcher.DefaultEventBehaviorService
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




    class Builder : AbstractClient.Builder<String, String, RequestBody, ResponseBody>() {

        // 基础主题 用于主题拼接
        private var baseTopic: String = ""

        // 字符集编码类型
        private var charsetName: String = "GBK"

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
                behaviorServices.add(DefaultEventBehaviorService())
            }
            if (eventManagerPool.isEmpty()) {
                eventManagerPool.add(DefaultEventManager())
            }
            if (timeoutManager == null) {
                timeoutManager = TimeOutHandler()
            }
            if (dispatchAdapter == null) {
                dispatchAdapter = SwitchMediator(baseTopic, requestAdapter)
            }
            if(backTopicConverter == null){
                throw IllegalStateException("backTopicConverter is null.")
            }
            if (responseAdapter == null) {
                responseAdapter = MqttResponseAdapter(charsetName)
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <Client : AbstractClient<String, String, RequestBody, ResponseBody>> builder(): Client {
            return MqttRRBinderClient(this) as Client
        }

    }
}