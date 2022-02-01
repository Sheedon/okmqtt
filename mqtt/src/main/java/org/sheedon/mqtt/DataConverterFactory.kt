package org.sheedon.mqtt

import org.sheedon.rr.core.DataConverter

/**
 * 数据转化器工厂
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/1 9:53 上午
 */
abstract class DataConverterFactory {

    open fun callbackNameConverter(topic: String?): DataConverter<String, String>? {
        return null
    }
}