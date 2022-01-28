package org.sheedon.mqtt.listener;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

/**
 * MqttActionListener Implementation class，
 * Internally holds IMqttActionListener and IResultActionListener,
 * corresponding to the feedback message according to the listener passed in by the developer
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/28 8:19 下午
 */
public class MqttActionListenerImpl implements IMqttActionListener {

    private IMqttActionListener mqttActionListener;
    private IResultActionListener resultActionListener;

    public MqttActionListenerImpl(IMqttActionListener mqttActionListener) {
        this.mqttActionListener = mqttActionListener;
    }

    public MqttActionListenerImpl(IResultActionListener resultActionListener) {
        this.resultActionListener = resultActionListener;
    }

    /**
     * This method is invoked when an action has completed successfully.
     *
     * @param asyncActionToken associated with the action that has completed
     */
    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        if (mqttActionListener != null) {
            mqttActionListener.onSuccess(asyncActionToken);
        }

        if (resultActionListener != null) {
            resultActionListener.onSuccess();
        }
    }

    /**
     * This method is invoked when an action fails.
     * If a client is disconnected while an action is in progress
     * onFailure will be called. For connections
     * that use cleanSession set to false, any QoS 1 and 2 messages that
     * are in the process of being delivered will be delivered to the requested
     * quality of service next time the client connects.
     *
     * @param asyncActionToken associated with the action that has failed
     * @param exception        thrown by the action that has failed
     */
    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        if (mqttActionListener != null) {
            mqttActionListener.onFailure(asyncActionToken, exception);
        }

        if (resultActionListener != null) {
            resultActionListener.onFailure(exception);
        }
    }
}
