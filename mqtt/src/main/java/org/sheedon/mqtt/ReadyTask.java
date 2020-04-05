package org.sheedon.mqtt;

/**
 * @Description: 请求任务类
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/13 12:51
 */
public class ReadyTask {
    private String id;// 任务UUID
    private String backName; // 反馈名称
    private Callback callback;// 反馈监听器
    private DelayEvent event;// 超时处理事件


    public static ReadyTask build(String id, String backName,
                                  Callback callback, DelayEvent event) {
        ReadyTask task = new ReadyTask();
        task.id = id;
        task.backName = backName;
        task.callback = callback;
        task.event = event;
        return task;
    }

    public String getId() {
        return id;
    }

    public String getBackName() {
        return backName;
    }

    public Callback getCallback() {
        return callback;
    }

    public DelayEvent getEvent() {
        return event;
    }
}
