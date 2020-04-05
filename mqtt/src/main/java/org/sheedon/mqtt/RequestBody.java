package org.sheedon.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

/**
 * 请求数据包
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/26 16:30
 */
public class RequestBody extends MqttMessage {
    private String data;

    /**
     * 创建请求数据包
     *
     * @param charset 字符集
     * @param data    内容
     * @return 请求数据包
     */
    public RequestBody updateData(String charset, String data) {
        if (charset == null || charset.isEmpty()) {
            return updateData(data);
        }

        try {
            data = CharsetUtils.changeCharset(data, charset);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return updateData(data);
    }

    public RequestBody updateData(String data) {
        this.data = data;
        setPayload(data.getBytes());
        return this;
    }

    public String getData() {
        return data;
    }
}
