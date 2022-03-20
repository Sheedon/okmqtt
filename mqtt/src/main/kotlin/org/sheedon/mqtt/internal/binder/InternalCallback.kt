package org.sheedon.mqtt.internal.binder

import org.sheedon.mqtt.ICallback

/**
 * 内部反馈Callback
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/20 6:32 下午
 */
data class InternalCallback(
    val callback: ICallback,
    val flag: CallbackEnum = CallbackEnum.SINGLE
)

enum class CallbackEnum {
    RETAIN,// 保留类型，作为订阅一个主题或关键字
    SINGLE// 单次类型，作为请求行为单次反馈
}