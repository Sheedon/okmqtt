package org.sheedon.mqtt

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 订阅观察的节点信息
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/5 10:06 下午
 */
class ObservableNote @JvmOverloads constructor(
    val name: String, // 当前节点名称
    val parent: ObservableNote? = null // 关联父节点
) {

    // 节点状态，空/启用/停用
    @NoteStatus
    var status = NoteStatus.EMPTY

    // 订阅个数，归零后，才能移除当前节点
    var count = AtomicInteger()

    // 子节点集合
    val childNotes = ConcurrentHashMap<String, ObservableNote>()

}