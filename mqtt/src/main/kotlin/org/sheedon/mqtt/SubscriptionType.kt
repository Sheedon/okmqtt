package org.sheedon.mqtt

/**
 * The scope of the current [Topics].
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/3 4:46 下午
 */
enum class SubscriptionType {

    // that means that the current [Topics] performs MQTT Topic subscription without subscribing,
    // and associates the current [Topics] with the callback implementation.
    REMOTE,

    // that just associated with callback, no need to subscribe to mqtt topic.
    LOCAL
}