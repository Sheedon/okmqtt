package org.sheedon.mqtt;


/**
 * 消息反馈
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/11 12:49
 */
public interface Callback<R extends Response> {

    void onFailure(Throwable e);


    void onResponse(R response);
}
