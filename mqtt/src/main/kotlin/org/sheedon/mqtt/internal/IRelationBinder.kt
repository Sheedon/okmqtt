package org.sheedon.mqtt.internal

import org.sheedon.mqtt.Relation

/**
 * 请求/订阅对象，必须实现获取关联者的接口
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/31 11:34 下午
 */
interface IRelationBinder {

    /**
     * 获取关联者对象
     */
    fun getRelation(): Relation
}