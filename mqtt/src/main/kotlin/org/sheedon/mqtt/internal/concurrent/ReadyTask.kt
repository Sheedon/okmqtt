package org.sheedon.mqtt.internal.concurrent

import org.sheedon.mqtt.IBack
import org.sheedon.mqtt.internal.connection.Listen

/**
 * 准备好的任务，包含的内容请求数据和反馈Callback
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/9 2:58 下午
 */
data class ReadyTask(
    var listen: Listen, // Call/Subscribe 的引用
    var id: Long, // 请求记录ID
    val type: CallbackEnum = CallbackEnum.SINGLE,//单次请求
    var back: IBack? = null // 反馈Callback
)