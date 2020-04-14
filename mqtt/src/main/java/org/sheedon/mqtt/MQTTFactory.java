package org.sheedon.mqtt;

/**
 * mqtt基本处理工厂
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/4/14 20:18
 */
public interface MQTTFactory {

    Call newCall(Request request);

    Observable newObservable(Request request);
}
