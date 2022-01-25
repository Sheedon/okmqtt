package org.sheedon.mqtt;

/**
 * 调度封装的基本接口
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/11 12:46
 */
public interface Call extends Cloneable {
    /**
     * Returns the original request that initiated this call.
     */
    Request request();


    void publishNotCallback();


    <R extends Response> void enqueue(Callback<R> callback);

    /**
     * Cancels the request, if possible. Requests that are already complete cannot be canceled.
     */
    void cancel();


    boolean isExecuted();

    boolean isCanceled();


    Call clone();
}
