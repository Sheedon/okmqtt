package org.sheedon.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;


/**
 * @Description: 网络反馈Runnable处理
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/14 23:08
 */
public class NetRunnable extends NamedRunnable {

    // 调度器
    final Dispatcher dispatcher;
    // 主题
    final String topic;
    // 内容
    final MqttMessage message;

    final String charsetName;

    NetRunnable(Dispatcher dispatcher, String topic, MqttMessage message, String charsetName) {
        super("NetRunnable");
        this.dispatcher = dispatcher;
        this.topic = topic;
        this.message = message;
        this.charsetName = charsetName;
    }

    /**
     * 执行数据处理，并且反馈到调度器结束任务处理
     */
    @Override
    protected void execute() {

        // 设置反馈名称和类型的默认值
        String backName = topic;

        String data = null;
        try {
            if (charsetName == null || charsetName.isEmpty()) {
                data = new String(message.getPayload());
            } else {
                data = new String(message.getPayload(), charsetName);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 借助具体客户端工具转化反馈名
        DataConverter<String, String> converter = dispatcher.callbackNameConverter(topic);
        if (converter != null) {
            backName = converter.convert(data);
        }

        // 获取UUID
        String id = dispatcher.findNetByBackNameToFirst(backName);

        // 结束网络调度
        dispatcher.finishedByNet(id, getResponse(data));
    }

    /**
     * 获取响应内容
     *
     * @param message 数据内容
     * @return 内容
     */
    private Response getResponse(String message) {
        return new ResponseBuilder()
                .code(200)
                .body(message)
                .build();
    }
}
