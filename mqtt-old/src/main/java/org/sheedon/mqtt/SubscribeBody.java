package org.sheedon.mqtt;

/**
 * @Description: mqtt订阅主题内容
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/13 17:36
 */
public class SubscribeBody {
    private String topic;
    private int qos;

    public static SubscribeBody build(String topic, int qos) {
        SubscribeBody body = new SubscribeBody();
        body.topic = topic;
        body.qos = qos;
        return body;
    }

    public String getTopic() {
        return topic;
    }

    public int getQos() {
        return qos;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }
}
