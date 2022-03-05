package org.sheedon.mqtt

import androidx.annotation.IntDef

/**
 * 节点状态
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/5 10:15 下午
 */
@MustBeDocumented
@IntDef(NoteStatus.EMPTY, NoteStatus.ENABLE, NoteStatus.DISABLE)
@Retention(AnnotationRetention.SOURCE)
annotation class NoteStatus {
    companion object {
        const val EMPTY = 0x0
        const val ENABLE = 0x1
        const val DISABLE = 0x2
    }
}
