package org.sheedon.mqtt;

/**
 * Runnable implementation which always sets its thread name.
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/11 20:59
 */
public abstract class NamedRunnable implements Runnable {
    private final String name;

    NamedRunnable(String format, Object... args) {
        this.name = Util.format(format, args);
    }

    @Override public final void run() {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName(name);
        try {
            execute();
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }

    protected abstract void execute();
}

