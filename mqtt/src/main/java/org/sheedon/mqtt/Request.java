package org.sheedon.mqtt;

/**
 * 数据请求类
 *
 * 泛型内容 + 延迟毫秒 + 反馈名 + tag
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/10 13:26
 */
public class Request {

    final String topic;
    final RequestBody body;
    final int delayMilliSecond;
    final String backName;
    final Object tag;

    public Request(RequestBuilder builder) {
        this.body = builder.body;
        this.topic = builder.topic;
        this.delayMilliSecond = builder.delayMilliSecond;
        this.backName = builder.backName;

        this.tag = builder.tag != null ? builder.tag : this;
    }

    public RequestBody getBody() {
        return body;
    }

    public String backName() {
        return backName;
    }

    public int delayMilliSecond() {
        return delayMilliSecond;
    }

    public String topic() {
        return topic;
    }
}
