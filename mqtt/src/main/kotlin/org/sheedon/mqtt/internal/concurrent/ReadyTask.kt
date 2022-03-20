package org.sheedon.mqtt.internal.concurrent

import org.sheedon.mqtt.Callback
import org.sheedon.mqtt.Request

/**
 * 准备好的任务，包含的内容请求数据和反馈Callback
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/9 2:58 下午
 */
data class ReadyTask(
    var request: Request? = null, // 请求数据
    var id: String? = null, // 请求记录ID
    var callback: Callback? = null // 反馈Callback
)