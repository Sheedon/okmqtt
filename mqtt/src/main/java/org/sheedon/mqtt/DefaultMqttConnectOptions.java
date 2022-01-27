package org.sheedon.mqtt;

/**
 * mqtt连接选项配置类
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:42 下午
 */
final class DefaultMqttConnectOptions extends org.eclipse.paho.client.mqttv3.MqttConnectOptions{
    private static DefaultMqttConnectOptions theOptions;

    // 设置基础值
    private DefaultMqttConnectOptions() {
        // 在线间隔30秒
        setKeepAliveInterval(30);
        // 连接超时10秒
        setConnectionTimeout(10);
        // 设置最大吞吐量
        setMaxInflight(30);
        // 设置清除
        setCleanSession(true);
        // 自动连接
        setAutomaticReconnect(true);
    }

    /**
     * 获取默认值
     *
     * @return MqttConnectOptions
     */
    static DefaultMqttConnectOptions getDefault() {
        synchronized (DefaultMqttConnectOptions.class) {
            if (theOptions == null) {
                theOptions = new DefaultMqttConnectOptions();
            }
        }
        return theOptions;
    }

    /**
     * 设置默认值
     *
     * @param options 配置类
     */
    static void setDefault(DefaultMqttConnectOptions options) {
        synchronized (DefaultMqttConnectOptions.class) {
            theOptions = options;
        }
    }
}
