package org.sheedon.mqtt;

/**
 * @Description: 断开的缓冲区选项
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/14 21:12
 */
class DisconnectedBufferOptions extends org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions {

    private static DisconnectedBufferOptions theOptions;

    // 设置基础值
    private DisconnectedBufferOptions() {

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
    static DisconnectedBufferOptions getDefault() {
        synchronized (DisconnectedBufferOptions.class) {
            if (theOptions == null) {
                theOptions = new DisconnectedBufferOptions();
            }
        }
        return theOptions;
    }

    /**
     * 设置默认值
     *
     * @param options 选项
     */
    static void setDefault(DisconnectedBufferOptions options) {
        synchronized (DisconnectedBufferOptions.class) {
            theOptions = options;
        }
    }
}
