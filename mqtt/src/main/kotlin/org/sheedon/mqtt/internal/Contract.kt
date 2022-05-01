package org.sheedon.mqtt.internal

/**
 * 公共参数
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/1 11:50
 */
internal object Contract {
    // 根订阅主题名，若只订阅关键字，不订阅MQTT主题，则统一存入该主题下的消息中
    const val ROOT_OBSERVER = "##"

    // 斜杠
    const val SLASH = "/"
    const val PLUS = "+"
    const val SIGN = "#"
}