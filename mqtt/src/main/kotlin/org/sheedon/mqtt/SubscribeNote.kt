package org.sheedon.mqtt

/**
 * 订阅主题节点
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/26 8:16 下午
 */
class SubscribeNote(val noteName: String) {
    internal var enable: Boolean = false
    internal val child: MutableMap<String, SubscribeNote> = mutableMapOf()

}