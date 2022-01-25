package org.sheedon.mqtt;

import androidx.annotation.Nullable;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * 反馈构造内容
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/26 23:50
 */
public class ResponseBuilder {

    int code = -1;
    String message;
    ResponseBody body;

    public ResponseBuilder() {
        body = createResponseBody();
    }

    protected ResponseBody createResponseBody() {
        return new ResponseBody();
    }

    public ResponseBuilder(Response response) {
        this.code = response.code;
        this.message = response.message;
        this.body = response.body;
    }

    public ResponseBuilder code(int code) {
        this.code = code;
        return this;
    }

    public ResponseBuilder message(String message) {
        this.message = message;
        return this;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public ResponseBody getBody() {
        return body;
    }

    public ResponseBuilder body(@Nullable ResponseBody body) {
        this.body = body;
        return this;
    }

    public ResponseBuilder body(@Nullable MqttMessage message, String charsetName) {
        if (message == null)
            return this;

        getBody().updateMqttMessage(message, charsetName);
        return this;
    }

    public synchronized ResponseBuilder body(String data) {
        if (this.getBody() == null) {
            this.body(createResponseBody());
        }
        this.getBody().updateBody(data);

        return this;
    }

    public Response build() {
        int code = getCode();
        if (code < 0) throw new IllegalStateException("code < 0: " + code);
        ResponseBody body = getBody();
        if (body == null) throw new IllegalStateException("body == null");
        return new Response(this);
    }
}
