package org.sheedon.mqtt;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;

/**
 * 断开的缓冲区选项
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:46 下午
 */
public class DefaultDisconnectedBufferOptions extends DisconnectedBufferOptions {

    private static DefaultDisconnectedBufferOptions theOptions;

    // 设置基础值
    private DefaultDisconnectedBufferOptions() {

        // 设置缓冲区启用
        setBufferEnabled(true);
        // 缓冲区大小
        setBufferSize(100);
        // 设置持久缓冲区
        setPersistBuffer(false);
        // 设置删除最早的消息
        setDeleteOldestMessages(false);

    }

    /**
     * 获取断开的缓冲区选项
     *
     * @return DisconnectedBufferOptions
     */
    static DefaultDisconnectedBufferOptions getDefault() {
        synchronized (DefaultDisconnectedBufferOptions.class) {
            if (theOptions == null) {
                theOptions = new DefaultDisconnectedBufferOptions();
            }
        }
        return theOptions;
    }

    /**
     * 设置默认值
     *
     * @param options 选项
     */
    static void setDefault(DefaultDisconnectedBufferOptions options) {
        synchronized (DefaultDisconnectedBufferOptions.class) {
            theOptions = options;
        }
    }
}