package org.sheedon.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * 请求构造内容
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/26 23:50
 */
public class RequestBuilder {

    String topic;
    RequestBody body;
    int delayMilliSecond = -1;
    String backName;
    Object tag;

    public RequestBuilder() {
        this.topic = "";
        this.body = createRequestBody();
    }

    RequestBody createRequestBody() {
        return new RequestBody();
    }

    RequestBuilder(Request request) {
        this.body = request.body;
        this.delayMilliSecond = request.delayMilliSecond;
        this.tag = request.tag;
        this.topic = request.topic;
    }

    public RequestBody getBody() {
        if (body == null)
            synchronized (this) {
                if (body == null) {
                    body = createRequestBody();
                }
            }
        return body;
    }

    /**
     * 设置请求消息
     *
     * @param body 消息内容
     * @return Builder
     */
    public RequestBuilder body(RequestBody body) {
        if (body == null) throw new NullPointerException("requestBody == null");
        this.body = body;
        return this;
    }

    /**
     * 单次请求超时额外设置
     *
     * @param delayMilliSecond 延迟时间（毫秒）
     * @return Builder
     */
    public RequestBuilder delayMilliSecond(int delayMilliSecond) {
        this.delayMilliSecond = delayMilliSecond;
        return this;
    }

    /**
     * 反馈主题名
     *
     * @param backName 反馈名
     * @return Builder
     */
    public RequestBuilder backName(String backName) {
        if (backName == null || backName.isEmpty())
            return this;

        this.backName = backName;
        return this;
    }

    /**
     * Attaches {@code tag} to the request. It can be used later to cancel the request. If the tag
     * is unspecified or null, the request is canceled by using the request itself as the tag.
     */
    public RequestBuilder tag(Object tag) {
        this.tag = tag;
        return this;
    }


    /**
     * 设置请求主题
     *
     * @param topic 主题
     * @return Builder
     */
    public RequestBuilder topic(String topic) {
        if (topic == null) throw new NullPointerException("topic == null");
        this.topic = topic;
        return this;
    }

    /**
     * 设置提交内容
     *
     * @param payload 内容
     * @return Builder
     */
    public RequestBuilder payload(String payload) {
        if (payload == null) throw new NullPointerException("payload == null");
        getBody().setPayload(payload.getBytes());
        return this;
    }

    /**
     * 设置服务质量
     *
     * @param qos 服务质量
     * @return Builder
     */
    public RequestBuilder qos(int qos) {
        getBody().setQos(qos);
        return this;
    }

    /**
     * 设置是否保留
     *
     * @param retained 保留
     * @return Builder
     */
    public RequestBuilder retained(boolean retained) {
        getBody().setRetained(retained);
        return this;
    }

    /**
     * 设置请求消息
     *
     * @param message 消息内容
     * @return Builder
     */
    public RequestBuilder message(MqttMessage message) {
        if(message == null)throw new NullPointerException("message == null");

        getBody().setPayload(message.getPayload());
        getBody().setQos(message.getQos());
        getBody().setRetained(message.isRetained());
        getBody().setId(message.getId());

        return this;
    }


    public Request build() {
        if (topic == null) throw new IllegalStateException("topic == null");
        if (getBody() == null) throw new IllegalStateException("body == null");
        return new Request(this);
    }
}
