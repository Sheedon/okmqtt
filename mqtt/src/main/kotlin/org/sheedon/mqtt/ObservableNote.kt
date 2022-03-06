package org.sheedon.mqtt

import java.lang.StringBuilder
import java.util.concurrent.ConcurrentHashMap
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
    val count = AtomicInteger()

    // add新增处理时的节点标识位
    @Volatile
    var flag = AtomicInteger()

    // 子节点集合
    val childNotes = ConcurrentHashMap<String, ObservableNote>()

    /**
     * 是否存在#号
     */
    fun hasPoundSign(): Boolean {
        return childNotes["#"] != null
    }

    /**
     * 是否存在+号
     */
    fun hasAddSign(): Boolean {
        return childNotes["+"] != null
    }

    /**
     * 通过下一个节点名，查找或创建目标节点
     * @param noteName 节点名称
     * @return ObservableNote 可观察的节点
     */
    fun createNextByName(noteName: String): ObservableNote {
        if (childNotes[noteName] == null) {
            synchronized(this) {
                if (childNotes[noteName] == null) {
                    childNotes[noteName] = ObservableNote(noteName, this)
                    childNotes[noteName]?.flag?.incrementAndGet()
                }
            }
        } else {
            childNotes[noteName]?.flag?.incrementAndGet()
        }
        return childNotes[noteName]!!
    }

    /**
     * 订阅递增 +1
     */
    fun increment() {
        count.incrementAndGet()
    }

    /**
     * 订阅递减 -1
     */
    fun decrement(): Int {
        val countNum = count.decrementAndGet()
        return if (countNum < 0) return 0 else countNum
    }

    /**
     * 获取订阅个数
     */
    fun getCount(): Int {
        return count.get()
    }

    /**
     * 当前为通配符，且不是空状态，空状态代表新增，则说明这个通配符之前已经配置，无需在此配置
     * @return true:已存在的通配符，false:新增的节点
     */
    fun checkWildcardAndNotEmpty(): Boolean {
        return (name == "#" || name == "+") && status != NoteStatus.EMPTY
    }

    /**
     * 当前节点是否启用
     */
    fun isEnable(): Boolean {
        return status == NoteStatus.ENABLE
    }

    /**
     * 当前节点是否停用
     */
    fun isDisable(): Boolean {
        return status == NoteStatus.DISABLE
    }

    /**
     * 是否存在关系子节点
     * 如果是#，不存在下级节点
     * 如果是+，不存在下级节点
     * 其他，则存在子节点
     */
    fun hasChildNotes(): Boolean {
        if (name == "#" || name == "+") {
            return false
        }
        return childNotes.isEmpty()
    }

    /**
     * 更新节点状态
     * 节点状态更改有一下两种情况
     * 1. 当前节点为新增节点，则状态为NoteStatus.EMPTY，
     * 那么根据 needAddSubscribe 来配置设置为启用([NoteStatus.ENABLE])，还是停用([NoteStatus.DISABLE])
     * 2. 当前节点已存在，则无需改变原本状态
     *
     * @param needAddSubscribe 是否需要新订阅
     * @return 是否作为新增项，需要重新订阅，true:为新增节点，且状态为启用，
     * false:不是新增节点，或者为新增节点且停用状态
     */
    fun updateStatus(needAddSubscribe: Boolean): Boolean {

        if (this.status != NoteStatus.EMPTY) {
            return false
        }

        return if (needAddSubscribe) {
            status = NoteStatus.ENABLE
            true
        } else {
            status = NoteStatus.DISABLE
            false
        }

    }

    /**
     * 停用节点
     */
    fun disableStatus() {
        status = NoteStatus.DISABLE
    }

    /**
     * 当前节点是否为 #
     */
    fun isPoundSign(): Boolean {
        return name == "#"
    }

    /**
     * 当前节点是否为 +
     */
    fun isPlusSign(): Boolean {
        return name == "+"
    }

    /**
     * 停用 # 所在同级和下级的订阅节点信息
     * 当前方法停用同级，下级采用[disableNextPoundSign]
     */
    @Synchronized
    fun disablePoundSign(
        parent: ObservableNote?,
        pathName: StringBuilder,
        removeArray: MutableSet<String>
    ) {
        val currentPathName = pathName.toString()
        parent?.childNotes?.forEach { entry ->
            entry.takeIf { it.key != "#" }
                ?.also {
                    val value = it.value
                    if (value.status == NoteStatus.ENABLE) {
                        removeArray.add(currentPathName + value.name)
                        value.status = NoteStatus.DISABLE
                    }
                    val current = StringBuilder(pathName).append(value.name).append(REGEX)
                    disableNextPoundSign(value, current, removeArray)
                }
        }
    }

    /**
     * 停用 # 所在下级的订阅节点信息
     */
    fun disableNextPoundSign(
        parent: ObservableNote?,
        pathName: StringBuilder,
        removeArray: MutableSet<String>
    ) {
        val currentPathName = pathName.toString()
        parent?.childNotes?.forEach {
            val value = it.value
            if (value.status == NoteStatus.ENABLE) {
                value.status = NoteStatus.DISABLE
                removeArray.add(currentPathName + value.name)
            }
            val current = StringBuilder(pathName).append(value.name).append(REGEX)
            disableNextPoundSign(value, current, removeArray)
        }
    }

    /**
     * 停用 + 所在同级
     */
    @Synchronized
    fun disablePlusSign(
        parent: ObservableNote?,
        pathName: StringBuilder,
        removeArray: MutableSet<String>
    ) {
        val currentPathName = pathName.toString()
        parent?.childNotes?.forEach { entry ->
            entry.takeIf { it.key != "+" }
                ?.also {
                    val value = it.value
                    if (value.status == NoteStatus.ENABLE) {
                        removeArray.add(currentPathName + value.name)
                        value.status = NoteStatus.DISABLE
                    }
                }
        }
    }

    /**
     * 自增flag
     */
    fun incrementFlag() {
        flag.incrementAndGet()
    }

    /**
     * 自减flag
     */
    fun decrementFlag() {
        flag.decrementAndGet()
    }

    /**
     * 通过节点名称获取子节点
     * @param noteName 节点名
     * @return ObservableNote 子节点
     */
    fun getChildNote(noteName: String): ObservableNote? {
        return childNotes[noteName]
    }

    /**
     * 子节点是否为空
     */
    fun isEmptyByChildNotes(): Boolean {
        return childNotes.isEmpty()
    }

    /**
     * 删除当前节点，并且回溯，依次判断条件并且删除链上节点
     * 条件：当前节点flag为0，未启用，不存在子节点
     */
    @Synchronized
    fun deleteSelf() {
        if (flag.get() > 0) {
            return
        }
        parent?.also {
            it.childNotes.remove(name)
        }?.takeIf {
            // 父节点上订阅数量为0，且没有子节点，且父节点不是根节点
            it.count.get() == 0 && !it.hasChildNotes()
                    && it.name != ROOT_NAME
        }?.also {
            it.deleteSelf()
        }
    }

    /**
     * 尝试删除自己这个节点
     */
    fun tryDeleteSelf() {
        if (getCount() <= 0 && !hasChildNotes()) {
            deleteSelf()
        }
    }

    /**
     * 因为取消 #，而启动的所在同级和下级的订阅节点信息
     * 当前方法启动同级，下级采用[disableNextPoundSign]
     */
    @Synchronized
    fun enablePoundSign(
        parent: ObservableNote?,
        pathName: StringBuilder,
        addArray: MutableSet<String>
    ) {
        parent?.childNotes?.also { childNotes ->
            // 不存在「+」，启动同级和下级
            if (childNotes["+"] == null) {
                val currentPathName = pathName.toString()
                childNotes.values.forEach {
                    if (it.name == "#") {
                        return@forEach
                    }
                    if (it.isDisable()) {
                        it.status = NoteStatus.ENABLE
                        addArray.add(currentPathName + it.name)
                    }
                    val current = StringBuilder(pathName).append(it.name).append(REGEX)
                    enableNextPoundSign(it, current, addArray)
                }
                return
            }

            childNotes["+"]?.status = NoteStatus.ENABLE
        }

        // 检索下级
        parent?.childNotes?.forEach { entry ->
            if (entry.key != "#" && entry.key != "+") {
                val current = StringBuilder(pathName).append(entry.key).append(REGEX)
                enableNextPoundSign(entry.value, current, addArray)
            }
        }
    }


    /**
     * 因为取消 +，而启动的所在同级
     */
    @Synchronized
    fun enablePlusSign(
        parent: ObservableNote?,
        pathName: StringBuilder,
        addArray: MutableSet<String>
    ) {
        val currentPathName = pathName.toString()
        parent?.childNotes?.forEach {
            if (it.key != "+" && it.value.isDisable()) {
                it.value.status = NoteStatus.ENABLE
                addArray.add(currentPathName + it.key)
            }
        }
    }

    /**
     * 启动 # 所在下级的订阅节点信息
     * 当前层级存在#，启动，且无需往下检索
     * 当前层级存在+，启动，同级无需检索，下级继续检索
     */
    fun enableNextPoundSign(
        parent: ObservableNote,
        pathName: StringBuilder,
        addArray: MutableSet<String>
    ) {
        val child = parent.childNotes
        // # 启动
        if (child["#"] != null) {
            child["#"]?.status = NoteStatus.ENABLE
            addArray.add("$pathName#")
            return
        }

        // + 启动
        if (child["+"] != null) {
            child["+"]!!.status = NoteStatus.ENABLE
            addArray.add("$pathName+")
            child.forEach {
                if (it.key != "+") {
                    val current = StringBuilder(pathName).append(it.value.name).append(REGEX)
                    enableNextPoundSign(it.value, current, addArray)
                }
            }
            return
        }

        // 其他启动
        val currentPathName = pathName.toString()
        child.forEach {
            if (it.value.status == NoteStatus.DISABLE) {
                addArray.add(currentPathName + it.value.name)
                it.value.status = 1
            }
            val current = StringBuilder(pathName).append(it.value.name).append(REGEX)
            enableNextPoundSign(it.value, current, addArray)
        }
    }


}