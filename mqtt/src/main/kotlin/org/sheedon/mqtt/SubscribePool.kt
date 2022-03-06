package org.sheedon.mqtt

import java.lang.StringBuilder

/**
 * 订阅池，旨在于位置订阅信息，以及新增节点和移除节点。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/4 10:02 下午
 */
const val ROOT_NAME = "org.sheedon.mqtt.root"
const val REGEX = "/"

class SubscribePool {

    // 根节点
    val rootNote: ObservableNote = ObservableNote(ROOT_NAME)

    /**
     * 添加一个订阅路径
     * @param path 需要订阅的路径
     * @return 包含实际需要订阅的路径，和取消订阅的路径集合
     */
    fun push(path: String): Pair<String?, Set<String>> {
        val removeArray = mutableSetOf<String>()

        // 若路径为空，则直接返回
        if (path.isEmpty()) {
            return Pair(null, removeArray)
        }

        // 按/分割字符串
        val noteArray = path.split(REGEX)
        // 当前路径
        var currentNote = rootNote
        // 用于记录多级#，是否需要新增订阅
        var needAddSubscribe = true
        // 完整路径名
        val pathName = StringBuilder()

        var size = noteArray.size
        val parentArray = mutableSetOf<ObservableNote>()
        noteArray.forEachIndexed { index, noteName ->
            // 路径中存在连续//的路径过滤无效/
            if (noteName.isEmpty()) {
                size -= 1
                return@forEachIndexed
            }
            currentNote.incrementFlag()

            pathName.append(noteName).append("/")
            if (needAddSubscribe) {
                when {
                    currentNote.hasPoundSign() -> needAddSubscribe = false
                    currentNote.hasAddSign()
                            && index == size - 1
                            && noteName != "#"
                    -> needAddSubscribe = false
                }
            }

            currentNote = currentNote.createNextByName(noteName)
        }

        // 订阅当前主题的个数+1
        currentNote.increment()
        // 当前为通配符，且不是空状态，空状态代表新增，则说明这个通配符之前已经配置，无需在此配置
        if (currentNote.checkWildcardAndNotEmpty()) {
            recoverFlag(parentArray)
            return Pair(null, removeArray)
        }

        // 更新状态，返回当前节点是否为新增启用项，不是新增启用节点，则直接返回当前结果
        val isAdd = currentNote.updateStatus(needAddSubscribe)
        if (!isAdd) {
            recoverFlag(parentArray)
            return Pair(null, removeArray)
        }

        // 去除最后一个/
        if (pathName.isNotEmpty()) {
            pathName.deleteCharAt(pathName.length - 1)
        }
        val addPath = pathName.toString()
        val parent = currentNote.parent

        if (currentNote.isPoundSign()) {
            // 当前新增项为#，则需要移除#所能通配的层级资源，从而添加到removeArray中
            pathName.deleteCharAt(pathName.length - 1)
            currentNote.disablePoundSign(parent, pathName, removeArray)
        } else if (currentNote.isPlusSign()) {
            // 当前新增项为+，则需要移除+所能通配的层级资源，从而添加到removeArray中
            pathName.deleteCharAt(pathName.length - 1)
            currentNote.disablePlusSign(parent, pathName, removeArray)
        }
        recoverFlag(parentArray)
        return Pair(addPath, removeArray)
    }

    /**
     * 恢复父节点flag
     * @param parentArray 父节点集合
     */
    private fun recoverFlag(parentArray: Set<ObservableNote>) {
        parentArray.forEach {
            it.decrementFlag()
        }
    }

    /**
     * 移除一个订阅路径
     * @param path 需要订阅的路径
     * @return 包含实际需要订阅的路径，和取消订阅的路径集合
     */
    fun pop(path: String): Pair<Set<String>, String?> {
        val addArray = mutableSetOf<String>()

        // 若路径为空，则直接返回
        if (path.isEmpty()) {
            return Pair(addArray, null)
        }

        // 按/分割字符串
        val noteArray = path.split(REGEX)
        // 当前路径
        var currentNote = rootNote
        // 完整路径名
        val pathName = StringBuilder()

        var size = noteArray.size
        noteArray.forEachIndexed { _, noteName ->
            // 路径中存在连续//的路径过滤无效/
            if (noteName.isEmpty()) {
                size -= 1
                return@forEachIndexed
            }

            pathName.append(noteName).append("/")
            val note = currentNote.getChildNote(noteName)
            if (note == null) {
                currentNote.tryDeleteSelf()
                return Pair(addArray, null)
            }
            currentNote = note
        }

        // 订阅当前主题的个数-1
        val count = currentNote.decrement()

        // 当前未启动，则结束后续行为
        // 当前主题订阅个数递减为0，如果是#/+则删除，或者其他节点，不存在子节点，则启动删除动作
        if (!currentNote.isEnable()) {
            currentNote.tryDeleteSelf()
            return Pair(addArray, null)
        }

        // 大于0，代表还有其他对象进行订阅，无需取消
        if (count > 0) {
            return Pair(addArray, null)
        }

        // 去除最后一个/
        if (pathName.isNotEmpty()) {
            pathName.deleteCharAt(pathName.length - 1)
        }

        val removePath = pathName.toString()
        val parent = currentNote.parent

        // 启动状态下，需要取消这个节点
        currentNote.disableStatus()
        if (currentNote.isPoundSign()) {
            // 当前删除项为#，则需要恢复#所能通配的层级资源，从而添加到addArray中
            pathName.deleteCharAt(pathName.length - 1)
            currentNote.enablePoundSign(parent, pathName, addArray)
        } else if (currentNote.isPlusSign()) {
            // 当前删除项为+，则需要恢复+所能通配的层级资源，从而添加到addArray中
            pathName.deleteCharAt(pathName.length - 1)
            currentNote.enablePlusSign(parent, pathName, addArray)
        }

        // 尝试删除当前节点，并且返回添加集合和移除的路径
        currentNote.tryDeleteSelf()
        return Pair(addArray, removePath)
    }


}