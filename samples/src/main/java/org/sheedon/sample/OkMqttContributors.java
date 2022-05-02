package org.sheedon.sample;

import android.content.Context;

import com.google.gson.Gson;

import org.sheedon.app.factory.CallbackNameConverter;
import org.sheedon.mqtt.OkMqttClient;
import org.sheedon.mqtt.Topics;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * OkMqttClient java code Contributor
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/25 22:35
 */
public class OkMqttContributors {

    // current client id
    private static final String clientId = UUID.randomUUID().toString();
    // It is recommended that developers change to their own mqtt-Broker
//    private static final String serverUri = "tcp://test.mosquitto.org:1883";
    private static final String serverUri = "tcp://broker-cn.emqx.io:1883";
    // Topic to subscribe to by default.
    private static final List<Topics> topicsBodies = new ArrayList<Topics>() {
        {
            // Contents in order: topic, message quality, attachRecord
            // attachRecord: whether additional records are required,
            // if necessary, the subscription will be automatically added after reconnection
            add(Topics.build("mq/clouds/cmd/test", 0, true));
        }
    };

    private static final OkMqttContributors instance = new OkMqttContributors();

    private volatile OkMqttClient okMqttClient;

    public static OkMqttContributors getInstance() {
        return instance;
    }

    private OkMqttContributors() {

    }

    /**
     * 创建OkMqttClient
     */
    public OkMqttClient loadOkMqttClient(Context context) {
        if (okMqttClient != null) {
            return okMqttClient;
        }

        Context clientContext = context.getApplicationContext();
        okMqttClient = new OkMqttClient.Builder()
                // Configure the basic parameters of mqtt connection
                .clientInfo(clientContext, serverUri, clientId)
                .subscribeBodies(topicsBodies.toArray(new Topics[0]))
                // 作用于关键字关联的响应信息解析器
                .keywordConverter(new CallbackNameConverter(new Gson()))
                .build();

        return okMqttClient;
    }

}
