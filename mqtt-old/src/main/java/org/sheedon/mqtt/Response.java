package org.sheedon.mqtt;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 反馈响应类
 *
 * code + 描述 + 内容
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/11 12:48
 */
public class Response{
    final int code;
    final String message;
    final @Nullable ResponseBody body;

    protected Response(ResponseBuilder builder) {
        this.code = builder.code;
        this.message = builder.message;
        this.body = builder.body;
    }

    public int code() {
        return code;
    }

    public boolean isSuccessful() {
        return code >= 200 && code < 300;
    }

    public String message() {
        return message;
    }

    public ResponseBody body() {
        return body;
    }


    public ResponseBuilder newBuilder() {
        return new ResponseBuilder(this);
    }

    @NonNull
    @Override
    public String toString() {
        return "Response{code="
                + code
                + ", message="
                + message
                + ", body="
                + (body != null ? body.getBody() : "null")
                + '}';
    }
}
