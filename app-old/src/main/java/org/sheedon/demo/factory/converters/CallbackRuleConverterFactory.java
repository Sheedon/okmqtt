package org.sheedon.demo.factory.converters;


import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.sheedon.mqtt.DataConverter;

/**
 * @Description: java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/3/11 0:40
 */
public class CallbackRuleConverterFactory extends DataConverter.Factory {

    private Gson gson;

    public static CallbackRuleConverterFactory create() {
        return new CallbackRuleConverterFactory(new Gson());
    }

    public static CallbackRuleConverterFactory create(Gson gson) {
        return new CallbackRuleConverterFactory(gson);
    }

    private CallbackRuleConverterFactory(Gson gson) {
        this.gson = gson;
    }

    @Nullable
    @Override
    public DataConverter<String, String> callbackNameConverter(String topic) {
        return new CallbackNameConverter(topic,gson);
    }

}
