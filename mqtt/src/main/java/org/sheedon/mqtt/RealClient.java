package org.sheedon.mqtt;

/**
 * 客户端基础实现内容
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/27 8:41
 */
public interface RealClient {
    Dispatcher dispatcher();

    long timeOutMilliSecond();

}
