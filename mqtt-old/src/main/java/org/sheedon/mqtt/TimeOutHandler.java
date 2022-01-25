package org.sheedon.mqtt;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.concurrent.TimeoutException;

/**
 * 超时执行器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2021/10/19 10:18 下午
 */
class TimeOutHandler {

    // 处理线程
    private final HandlerThread triggerThread;
    // 事务处理器
    private final Handler workHandler;
    // 调度器
    private final Dispatcher dispatcher;

    public TimeOutHandler(Dispatcher dispatcher) {
        this(TimeOutHandler.class.getName(), dispatcher);
    }

    /**
     * 异步触发器
     *
     * @param name HandlerThread 添加的名称
     */
    public TimeOutHandler(String name, Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        // 创建一个HandlerThread 用于执行消息Loop
        triggerThread = new HandlerThread(name);
        triggerThread.start();

        // 创建绑定在triggerThread的handler
        workHandler = new Handler(triggerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (TimeOutHandler.this.dispatcher != null) {
                    TimeOutHandler.this.dispatcher.finishedByLocal(String.valueOf(msg.obj),
                            new TimeoutException("网络超时"));
                }
                return false;
            }
        });
    }

    /**
     * 新增超时事件
     * @param event 超时事件
     */
    public void addEvent(DelayEvent event) {
        Message obtain = Message.obtain();
        obtain.obj = event.getId();
        workHandler.sendMessageDelayed(obtain, event.getDelay());
    }

    /**
     * 移除超时事件
     * @param event 超时事件
     */
    public void removeEvent(DelayEvent event) {
        if (event != null) {
            workHandler.removeCallbacksAndMessages(event.getId());
        }
    }

    /**
     * 销毁
     */
    void onDestroy() {
        workHandler.removeCallbacksAndMessages(null);
        if (triggerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                triggerThread.quitSafely();
            } else {
                triggerThread.quit();
            }
        }
    }
}
