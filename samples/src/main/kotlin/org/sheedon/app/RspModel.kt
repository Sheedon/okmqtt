package org.sheedon.app

/**
 * 反馈消息Model
 *
 * @author sheedon
 * @version 1.0
 */
class RspModel<T> {
    var type: String? = null
    var code: String? = null
    var errMessage: String? = null
    var data: T? = null
        private set

    fun setData(data: T) {
        this.data = data
    }
}