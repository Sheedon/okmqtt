package org.sheedon.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * 反馈内容
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/26 16:31
 */
public class ResponseBody {


    private String data;
    private int qos = 1;
    private boolean retained = false;
    private boolean dup = false;
    private int messageId;

    public void updateMqttMessage(MqttMessage message, String charset) {
        this.qos = message.getQos();
        this.retained = message.isRetained();
        this.dup = message.isDuplicate();
        this.messageId = message.getId();
        this.data = CharsetUtils.changeCharset(message.getPayload(), charset);
    }

    public String getBody() {
        return data;
    }

    public void updateBody(String data) {
        this.data = data;
    }

    public void close() {

    }
}
