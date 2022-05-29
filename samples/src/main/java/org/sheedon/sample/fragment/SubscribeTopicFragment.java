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
import org.sheedon.app.databinding.FragmentSubscribeTopicBinding;
import org.sheedon.mqtt.Observable;
import org.sheedon.mqtt.ObservableBack;
import org.sheedon.mqtt.OkMqttClient;
import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.Response;
import org.sheedon.sample.OkMqttContributors;
import org.sheedon.sample.viewmodel.SubscribeTopicViewModel;

/**
 * 订阅一个主题
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:09
 */
public class SubscribeTopicFragment extends Fragment {

    private FragmentSubscribeTopicBinding binding;
    private SubscribeTopicViewModel viewModel = new SubscribeTopicViewModel();

    private Observable observable;

    private OkMqttClient okMqttClient = OkMqttContributors.getInstance().loadOkMqttClient(getContext());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().setTitle(R.string.label_subscribe_topic);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_subscribe_topic, container, false);
        binding.setVm(viewModel);
        binding.button.setOnClickListener(v -> subscribeAndUnSubscribe());
        return binding.getRoot();
    }

    private void subscribeAndUnSubscribe() {
        boolean isSubscribe = viewModel.isSubscribe.get();
        if (!isSubscribe) {
            subscribe();
            return;
        }

        observable.cancel();
        viewModel.isSubscribe.set(false);
    }

    private void subscribe() {

        Editable topicText = binding.etTopic.getText();
        Editable keywordText = binding.etKeyword.getText();
        if ((topicText == null || TextUtils.isEmpty(topicText.toString()))
                && (keywordText == null || TextUtils.isEmpty(keywordText.toString()))) {
            Toast.makeText(getContext(), R.string.hint_topic_keyword, Toast.LENGTH_LONG).show();
            return;
        }

        viewModel.isSubscribe.set(true);

        String topicStr = topicText != null ? topicText.toString() : "";
        String keywordStr = keywordText != null ? keywordText.toString() : "";

        Request request = new Request.Builder()
                .subscribeTopic(topicStr)
                .keyword(keywordStr)
                .build();

        observable = okMqttClient.newObservable(request);
        observable.enqueue(new ObservableBack() {
            @Override
            public void onResponse(@NonNull Observable observable, @NonNull Response response) {
                binding.tvMessage.setText(response.getBody().getData());
            }

            @Override
            public void onFailure(@Nullable Throwable e) {

            }
        });
    }
}
