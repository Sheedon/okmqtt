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

class TopicsPool {

    // 根节点
    val rootNote: ObservableNote = ObservableNote(ROOT_NAME)
    val systemRootNote: ObservableNote = ObservableNote(ROOT_NAME)

    // 优先存在#，都存在按/个数由少到多排序，否则根据存在+，按/个数由少到多排序
    val sortCondition = Comparator<String> { condition1, condition2 ->
        val o1End = condition1.endsWith("#")
        val o2End = condition2.endsWith("#")
        compareProcess(
            o1End, o2End,
            compareCount(condition1, condition2),
            sortPlusSign(condition1, condition2)
        )
    }

    /**
     * 添加一个订阅路径
     * @param paths 需要订阅的路径集合
     * @return 包含实际需要订阅的路径集合，和取消订阅的路径集合
     */
    fun push(paths: List<String>): Pair<Set<String>, Set<String>> {
        val addArray = mutableSetOf<String>()
        val removeArray = mutableSetOf<String>()
        val targetList = paths.sortedWith(sortCondition)
        targetList.forEach { path ->
            if (path.isEmpty()) {
                return@forEach
            }
            push(path).also { pair ->
                pair.first?.let {
                    addArray.add(it)
                }
                removeArray.addAll(pair.second)
            }
        }

        // 一般不存在重复
        addArray.removeAll(removeArray)
        return Pair(addArray, removeArray)
    }

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
        var currentNote = loadRootNote(path)
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
     * 移除一个订阅路径集合
     * @param paths 需要取消订阅的路径集合
     * @return 包含实际需要订阅的路径集合，和取消订阅的路径集合
     */
    fun pop(paths: List<String>): Pair<Set<String>, Set<String>> {
        val addArray = mutableSetOf<String>()
        val removeArray = mutableSetOf<String>()
        val targetList = paths.sortedWith(sortCondition)
        // 排序翻转
        targetList.reversed()

        targetList.forEach { path ->
            if (path.isEmpty()) {
                return@forEach
            }
            pop(path).also { pair ->
                addArray.addAll(pair.first)
                pair.second?.let {
                    removeArray.add(it)
                }
            }
        }

        // 一般不存在重复
        removeArray.removeAll(addArray)
        return Pair(addArray, removeArray)
    }

    /**
     * 移除一个订阅路径
     * @param path 需要取消订阅的路径
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
        var currentNote = loadRootNote(path)
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

    /**
     * 获取根节点
     */
    private fun loadRootNote(path: String): ObservableNote {
        return if (path.startsWith("\$SYS")) {
            systemRootNote
        } else {
            rootNote
        }
    }

    /**
     * 比较+号
     */
    private fun sortPlusSign(str1: String, str2: String): () -> Int {
        val o1End = str1.endsWith("+")
        val o2End = str2.endsWith("+")

        return {
            compareProcess(
                o1End, o2End,
                compareCount(str1, str2)
            ) { 0 }
        }

    }

    /**
     * 比较流程规范
     * @param condition1 比较条件1
     * @param condition2 比较条件2
     * @param conformityFun 比较条件1/比较条件2 都符合情况下执行
     * @param nonConformityFun 比较条件1/比较条件2 都不符合条件下执行
     *
     */
    private fun compareProcess(
        condition1: Boolean,
        condition2: Boolean,
        conformityFun: () -> Int,
        nonConformityFun: () -> Int
    ): Int {
        return if (condition1 && condition2) {
            conformityFun()
        } else if (condition1) {
            -1
        } else if (condition2) {
            1
        } else {
            nonConformityFun()
        }
    }

    /**
     * 比较个数
     */
    private fun compareCount(str1: String, str2: String): () -> Int {
        return {
            str1.count {
                it == '/'
            } - str2.count {
                it == '/'
            }
        }
    }

}