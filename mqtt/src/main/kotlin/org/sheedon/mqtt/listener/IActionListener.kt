package org.sheedon.mqtt.listener

/**
 * Implementers of this interface will be notified when the asynchronous operation is complete.
 * Mainly used for "connect", "disconnect", "subscribe", "unsubscribe".
 * After receiving the feedback result, the action field indicates the processing type of the request,
 * and onSuccess/onFailure methods indicate the content of the success or failure
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/29 3:42 下午
 */
interface IActionListener {

    /**
     * The enumeration object of mqtt action listener includes "subscribe", "unsubscribe", "connect", "unconnect".
     */
    enum class ACTION {
        CONNECT,
        DISCONNECT,
        SUBSCRIBE,
        UNSUBSCRIBE
    }

    /**
     * This method is invoked when an action has completed successfully.
     */
    fun onSuccess(action: ACTION)

    /**
     * This method is invoked when an action fails.
     */
    fun onFailure(action: ACTION, exception: Throwable?)
}