package org.sheedon.mqtt

/**
 * 订阅类型
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/3 4:46 下午
 */
enum class SubscriptionType {

    // mqtt subscription + local subscription
    REMOTE,

    // Single local subscription
    LOCAL
}