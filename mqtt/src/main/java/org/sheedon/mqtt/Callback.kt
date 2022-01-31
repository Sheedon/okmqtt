package org.sheedon.mqtt

import org.sheedon.rr.core.Callback

/**
 * 回调信息
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 7:57 下午
 */
interface Callback : Callback<Request, Response> {
}