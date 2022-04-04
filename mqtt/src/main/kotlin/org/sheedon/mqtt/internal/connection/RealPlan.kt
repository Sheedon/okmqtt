package org.sheedon.mqtt.internal.connection

/**
 * 承载整个请求调度链：所有应用程序调度包括：订阅调度、请求调度。
 * 若执行中执行取消操作，则停止执行接下来的操作，若已执行，则调度取消动作
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/27 9:18 下午
 */
open class RealPlan(
    internal val call: Listen,
    private val nextPlan: Plan?
) : Plan {


    override fun proceed() {
        nextPlan?.proceed()
    }

    override fun cancel() {
        nextPlan?.cancel()
    }

}