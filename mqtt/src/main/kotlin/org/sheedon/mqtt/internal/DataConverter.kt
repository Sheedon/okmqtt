package org.sheedon.mqtt.internal

/**
 * 内容转换器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/8 4:55 下午
 */
interface DataConverter<T, F> {
    fun convert(value: T): F?
}