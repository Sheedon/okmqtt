package org.sheedon.mqtt;

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 延迟处理请求超时
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/13 10:51
 */
public class DelayEvent implements Delayed {

    private String id;
    private Date timeOutDate;

    public static DelayEvent build(String id, Date timeOutDate) {
        DelayEvent event = new DelayEvent();
        event.id = id;
        event.timeOutDate = timeOutDate;
        return event;
    }

    @Override
    public int compareTo(Delayed o) {
        long result = this.getDelay(TimeUnit.MILLISECONDS)
                - o.getDelay(TimeUnit.MILLISECONDS);
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        Date now = new Date();
        long diff = timeOutDate.getTime() - now.getTime();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    public long getDelay(){
        return timeOutDate.getTime() - System.currentTimeMillis();
    }

    String getId() {
        return id;
    }
}
