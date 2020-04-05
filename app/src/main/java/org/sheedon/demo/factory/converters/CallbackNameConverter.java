package org.sheedon.demo.factory.converters;

import com.google.gson.Gson;

import org.sheedon.demo.RspModel;
import org.sheedon.mqtt.DataConverter;

/**
 * @Description: java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/3/11 0:45
 */
public class CallbackNameConverter implements DataConverter<String, String> {
    String topic;
    Gson gson;

    public CallbackNameConverter(String topic,Gson gson) {
        this.topic = topic;
        this.gson = gson;
    }

    @Override
    public String convert(String value) {
        if (value == null || value.isEmpty())
            return "";

        RspModel rspModel = null;
        try {
            rspModel = gson.fromJson(value, RspModel.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (rspModel == null || rspModel.getType() == null || rspModel.getType().equals(""))
            return "";

        return rspModel.getType();
    }
}
