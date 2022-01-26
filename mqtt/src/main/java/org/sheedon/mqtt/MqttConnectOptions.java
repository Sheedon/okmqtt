package org.sheedon.mqtt;

/**
 * mqtt连接选项配置类
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/26 4:42 下午
 */
final class MqttConnectOptions extends org.eclipse.paho.client.mqttv3.MqttConnectOptions{
    private static MqttConnectOptions theOptions;

    // 设置基础值
    private MqttConnectOptions() {
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
    static MqttConnectOptions getDefault() {
        synchronized (MqttConnectOptions.class) {
            if (theOptions == null) {
                theOptions = new MqttConnectOptions();
            }
        }
        return theOptions;
    }

    /**
     * 设置默认值
     *
     * @param options 配置类
     */
    static void setDefault(MqttConnectOptions options) {
        synchronized (MqttConnectOptions.class) {
            theOptions = options;
        }
    }
}
