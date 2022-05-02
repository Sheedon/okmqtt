package org.sheedon.sample.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import org.sheedon.app.R;
import org.sheedon.app.databinding.FragmentPublishMessageBinding;
import org.sheedon.mqtt.Call;
import org.sheedon.mqtt.OkMqttClient;
import org.sheedon.mqtt.Request;
import org.sheedon.sample.OkMqttContributors;

import java.util.Objects;

/**
 * 提交Mqtt message 消息
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:07
 */
public class PublishMessageFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().setTitle(R.string.label_publish_message);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentPublishMessageBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_publish_message, container, false);

        // 提交mqtt消息
        binding.btnPublish.setOnClickListener(v ->
                publish(binding.etTopic.getText().toString(),
                        binding.spinnerQos.getSelectedItem().toString(),
                        binding.spinnerRetain.getSelectedItem().toString(),
                        binding.etMessage.getText().toString()));


        return binding.getRoot();
    }

    /**
     * 发送Mqtt消息请求
     *
     * @param topic   主题
     * @param qos     消息质量
     * @param retain  是否保留
     * @param message 消息
     */
    private void publish(String topic, String qos, String retain, String message) {
        int qosValue = Integer.parseInt(qos);
        boolean retainValue = Objects.equals(retain, "true");

        OkMqttClient client = OkMqttContributors.getInstance().loadOkMqttClient(getContext());

        // 构建请求类
        Request request = new Request.Builder()
                .topic(topic, qosValue, retainValue)
                .data(message)
                .build();
        // 得到Call
        Call call = client.newCall(request);
        call.publish();
    }
}
