package org.sheedon.sample.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import org.sheedon.app.R;
import org.sheedon.app.databinding.FragmentRequestResponseBinding;
import org.sheedon.mqtt.Call;
import org.sheedon.mqtt.Callback;
import org.sheedon.mqtt.Observable;
import org.sheedon.mqtt.OkMqttClient;
import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.Response;
import org.sheedon.sample.OkMqttContributors;
import org.sheedon.sample.viewmodel.SubscribeTopicViewModel;

import java.util.Objects;

/**
 * mqtt请求响应
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:09
 */
public class RequestAndResponseFragment extends Fragment {

    private FragmentRequestResponseBinding binding;

    private OkMqttClient okMqttClient = OkMqttContributors.getInstance().loadOkMqttClient(getContext());


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().setTitle(R.string.label_request_and_response);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_request_response, container, false);
        binding.btnPublish.setOnClickListener(v -> requestAndResponse());
        return binding.getRoot();
    }

    private void requestAndResponse() {
        Editable topicText = binding.etTopic.getText();
        Editable subscribeTopicText = binding.etBackTopic.getText();
        Editable keywordText = binding.etKeyword.getText();

        if (topicText == null || TextUtils.isEmpty(topicText.toString())) {
            Toast.makeText(getContext(), R.string.hint_topic, Toast.LENGTH_LONG).show();
            return;
        }

        if ((subscribeTopicText == null || TextUtils.isEmpty(subscribeTopicText.toString()))
                && (keywordText == null || TextUtils.isEmpty(keywordText.toString()))) {
            Toast.makeText(getContext(), R.string.hint_topic_keyword, Toast.LENGTH_LONG).show();
            return;
        }
        String topicStr = subscribeTopicText != null ? subscribeTopicText.toString() : "";
        String keywordStr = keywordText != null ? keywordText.toString() : "";

        int qosValue = Integer.parseInt(binding.spinnerQos.getSelectedItem().toString());
        boolean retainValue = Objects.equals(binding.spinnerRetain.getSelectedItem().toString(), "true");
        String message = binding.etMessage.getText().toString();

        OkMqttClient client = OkMqttContributors.getInstance().loadOkMqttClient(getContext());

        // 构建请求类
        Request request = new Request.Builder()
                .topic(topicText.toString(), qosValue, retainValue)
                .subscribeTopic(topicStr)
                .keyword(keywordStr)
                .data(message)
                .build();
        // 得到Call
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                binding.tvMessage.setText(response.getBody().getData());
            }

            @Override
            public void onFailure(@Nullable Throwable e) {
                binding.tvMessage.setText(e.toString());
            }
        });

    }

}
