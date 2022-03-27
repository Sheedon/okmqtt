package org.sheedon.mqtt.internal.connection

/**
 * 请求调度链,按照配置的流程，依次执行每个流程
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/27 9:05 下午
 */
interface Plan {

    /**
     * 继续执行下一个调度行为
     */
    fun proceed()

    /**
     * 取消操作
     */
    fun cancel()
}