package org.sheedon.mqtt;

import java.util.Date;

/**
 * @Description: 异步处理类基本需要实现的接口
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/21 12:10
 */
public interface AsyncCallImpl {

    // 任务UUID
    String id();

    // 反馈器
    Callback callback();

    // 延迟时间
    Date delayDate();

    // 反馈名称
    String backName();
}
