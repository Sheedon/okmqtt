package org.sheedon.mqtt;

/**
 * 观察者
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/4/14 12:58
 */
public interface Observable extends Cloneable{
    /**
     * Returns the original request that initiated this call.
     */
    Request request();

    <R extends Response> void subscribe(Callback<R> callback);

    /**
     * Cancels the request, if possible. Requests that are already complete cannot be canceled.
     */
    void cancel();


    boolean isExecuted();

    boolean isCanceled();


    Observable clone();
}
