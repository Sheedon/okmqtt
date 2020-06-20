package org.sheedon.mqtt;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 请求超时
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/13 16:25
 */
public class TimeOutRunnable extends NamedRunnable {

    // 延迟队列
    private final DelayQueue<DelayEvent> queue = new DelayQueue();
    // 调度器
    private final Dispatcher dispatcher;

    private final Object lock = new Object();

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean wait = new AtomicBoolean(false);

    TimeOutRunnable(Dispatcher dispatcher) {
        super("TimeOutRunnable");
        this.dispatcher = dispatcher;
    }

    boolean isRunning() {
        return running.get();
    }

    @Override
    protected void execute() {
        if (running.get())
            return;

        running.set(true);

        // 循环直至超时任务结束
        // 得到并且执行延迟事件
        do {
            delayEvent();
            DelayEvent delayEvent = queue.poll();
            if (delayEvent != null && delayEvent.getId() != null
                    && !delayEvent.getId().equals("")) {
                dispatcher.finishedByLocal(delayEvent.getId(), new TimeoutException("网络超时"));
            }
        } while (queue.size() > 0);

        running.set(false);
    }

    private void delayEvent() {
        synchronized (lock) {
            DelayEvent peek = queue.peek();

            if (peek == null)
                return;


            long time = peek.getDelay();

            if (time < 0)
                return;

            try {
                wait.set(true);
                lock.wait(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                wait.set(false);
            }
        }
    }

    public void addEvent(DelayEvent event) {
        queue.add(event);
        notifyQueue();
    }

    public void removeEvent(DelayEvent event) {
        if (event != null) {
            queue.remove(event);
        }
        notifyQueue();
    }

    private void notifyQueue() {
        synchronized (lock) {
            if (wait.get()) {
                wait.set(false);
                lock.notifyAll();
            }
        }
    }
}
