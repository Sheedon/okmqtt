package org.sheedon.demo.factory;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.sheedon.demo.App;
import org.sheedon.demo.factory.converters.CallbackRuleConverterFactory;
import org.sheedon.mqtt.Call;
import org.sheedon.mqtt.Callback;
import org.sheedon.mqtt.MqttCallbackExtendedListener;
import org.sheedon.mqtt.OkMqttClient;
import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.RequestBuilder;
import org.sheedon.mqtt.Response;
import org.sheedon.mqtt.SubscribeBody;

import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @Description: java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/18 13:26
 */
public class MqttClient implements MqttCallbackExtendedListener {

    public static final MqttClient getInstance() {
        return MqttClientHolder.INSTANCE;
    }

    private OkMqttClient mClient;

    private static class MqttClientHolder {
        private static final MqttClient INSTANCE = new MqttClient();
    }

    private MqttClient() {
        createClient();
    }

    private void createClient() {
        // 创建MqttClient

        String clientId = "yhkhs2018102904611";// 设置设备编号
        String serverUri = "tcp://mqtt.yanhangtec.com";// 设置服务器地址
//        if (clientId == null || clientId.trim().equals(""))
//            return;

        Queue<SubscribeBody> subscribeBodies = new ArrayDeque<>();
        subscribeBodies.add(SubscribeBody.build("yh_classify/clouds/garbage/cmd/LJTF2020072001", 1));// 添加需要订阅主题
        subscribeBodies.add(SubscribeBody.build("yh_classify/clouds/recyclable/cmd/yhkhs20181029046", 1));// 添加需要订阅主题


        if (mClient == null) {
            mClient = new OkMqttClient.Builder()
                    .clientInfo(App.getInstance(), serverUri, clientId)
                    .subscribeBodies(subscribeBodies)
                    .baseTopic("yh_classify/device/recyclable/data/yhkhs20181029046")// 添加基础主题
                    .addConverterFactory(CallbackRuleConverterFactory.create())
                    .callback(this).build();
        }
    }

    public OkMqttClient getClient() {
        return mClient;
    }

    public void publish(String message, String backName, Callback<Response> responseCallback) {

        if (mClient == null || !mClient.mqttClient().isConnected()) {
            if (responseCallback != null)
                responseCallback.onFailure(new ConnectException("未连接"));
            return;
        }

        Request request = new RequestBuilder()
                .payload(message)
                .backName(backName)
                .build();

        Call call = mClient.newCall(request);
        call.enqueue(responseCallback);

    }

    public void publish(String message, String backName) {
        Request request = new RequestBuilder()
                .payload(message)
                .backName(backName)
                .build();

        Call call = mClient.newCall(request);
        call.publishNotCallback();

    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {

    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            Log.v("SXD", "messageArrived: " + topic + " : " + new String(message.getPayload(), "GBK"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    @Override
    public void messageArrived(String topic, String data) {
        Log.v("SXD","topic:"+topic);
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {

    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        mClient.reConnect();
    }
}
