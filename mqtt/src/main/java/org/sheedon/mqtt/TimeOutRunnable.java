package org.sheedon.mqtt;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeoutException;

/**
 * 请求超时
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/13 16:25
 */
public class TimeOutRunnable extends NamedRunnable {

    // 延迟队列
    private final DelayQueue<DelayEvent> queue;
    // 调度器
    private final Dispatcher dispatcher;

    private boolean running;

    TimeOutRunnable(Dispatcher dispatcher, DelayQueue<DelayEvent> queue) {
        super("TimeOutRunnable");
        this.queue = queue;
        this.dispatcher = dispatcher;
    }

    boolean isRunning() {
        return running;
    }

    @Override
    protected void execute() {
        running = true;

        if (queue == null) {
            running = false;
            return;
        }

        // 循环直至超时任务结束
        // 得到并且执行延迟事件
        do {
            DelayEvent delayEvent;
            do {
                delayEvent = queue.poll();
                if (delayEvent != null
                        && delayEvent.getId() != null
                        && !delayEvent.getId().equals("")) {

                    dispatcher.finishedByLocal(delayEvent.getId(), new TimeoutException("网络超时"));
                }
            } while (delayEvent != null);
        } while (queue.size() > 0);

        running = false;
    }
}
