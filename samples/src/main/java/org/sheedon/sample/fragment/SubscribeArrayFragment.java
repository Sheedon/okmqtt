package org.sheedon.sample.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.sheedon.app.R;
import org.sheedon.app.databinding.FragmentSubscribeArrayBinding;
import org.sheedon.mqtt.FullCallback;
import org.sheedon.mqtt.Observable;
import org.sheedon.mqtt.ObservableBack;
import org.sheedon.mqtt.OkMqttClient;
import org.sheedon.mqtt.Relation;
import org.sheedon.mqtt.Response;
import org.sheedon.mqtt.Subscribe;
import org.sheedon.mqtt.SubscribeBack;
import org.sheedon.mqtt.Topics;
import org.sheedon.sample.OkMqttContributors;
import org.sheedon.sample.viewmodel.SubscribeTopicViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 订阅一组主题
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:10
 */
public class SubscribeArrayFragment extends Fragment {

    private FragmentSubscribeArrayBinding binding;
    private SubscribeTopicViewModel viewModel = new SubscribeTopicViewModel();

    private Observable observable;

    private OkMqttClient okMqttClient = OkMqttContributors.getInstance().loadOkMqttClient(getContext());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().setTitle(R.string.label_subscribe_topic_array);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_subscribe_array, container, false);
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

        viewModel.isSubscribe.set(true);

        // 单一关联内容
        Relation relation = new Relation.Builder()
                .topics(new Topics("sheedon/open_ack"))
                .build();

        // 关联集合
        List<Relation> relations = new ArrayList<Relation>() {
            {
                add(new Relation.Builder().topics(new Topics("sheedon/alarm_ack")).build());
                add(new Relation.Builder().keyword("test_ack").build());
            }
        };

        // 订阅对象
        Subscribe subscribe = new Subscribe.Builder()
                // 标准配置
                .add("sheedon/test_ack")
                // 通过Relation配置
                .add(relation)
                // 添加Relation集合
                .addAll(relations)
                .build();

        observable = okMqttClient.newObservable(subscribe);
        // 订阅全监听，响应+订阅+错误
        observable.enqueue(new FullCallback() {
            @Override
            public void onResponse(@NonNull Observable observable, @NonNull Response response) {
                binding.tvMessage.setText(response.getBody().getData());
            }

            @Override
            public void onResponse(@Nullable MqttWireMessage response) {
                binding.tvMessage.setText(response != null ? response.toString() : "onResponse");
            }

            @Override
            public void onFailure(@Nullable Throwable e) {
                binding.tvMessage.setText(e.getMessage());
            }
        });

        // 订阅响应信息监听
//        observable.enqueue(new ObservableBack() {
//            @Override
//            public void onResponse(@NonNull Observable observable, @NonNull Response response) {
//
//            }
//
//            @Override
//            public void onFailure(@Nullable Throwable e) {
//
//            }
//        });

        // 订阅情况监听
//        observable.enqueue(new SubscribeBack() {
//            @Override
//            public void onResponse(@Nullable MqttWireMessage response) {
//
//            }
//
//            @Override
//            public void onFailure(@Nullable Throwable e) {
//
//            }
//        });

    }
}
