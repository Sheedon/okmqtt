package org.sheedon.demo;

/**
 * 反馈消息Model
 *
 * @author sheedon
 * @version 1.0
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class RspModel<T> {

    private String type;
    private String code;
    private String errMessage;
    private T data;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }
}