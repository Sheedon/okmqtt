package org.sheedon.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.sheedon.sample.viewmodel.SubscribeTopicViewModel
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage
import org.sheedon.app.R
import org.sheedon.app.databinding.FragmentSubscribeArrayBinding
import org.sheedon.app.factory.MqttClient
import org.sheedon.mqtt.*

/**
 * 订阅一组主题
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:10
 */
class SubscribeArrayFragment : Fragment() {


    private lateinit var binding: FragmentSubscribeArrayBinding
    private val viewModel = SubscribeTopicViewModel()
    private var observable: Observable? = null
    private val okMqttClient = MqttClient.getInstance().loadOkMqttClient(requireContext())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.label_subscribe_topic_array)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_subscribe_array, container, false)
        binding.vm = viewModel
        binding.button.setOnClickListener { subscribeAndUnSubscribe() }
        return binding.root
    }

    private fun subscribeAndUnSubscribe() {
        val isSubscribe = viewModel.isSubscribe.get()
        if (!isSubscribe) {
            subscribe()
            return
        }
        observable?.cancel()
        viewModel.isSubscribe.set(false)
    }

    private fun subscribe() {
        viewModel.isSubscribe.set(true)

        // 单一关联内容
        val relation = Relation.Builder()
            .topics(Topics("sheedon/open_ack"))
            .build()

        // 关联集合
        val relations: List<Relation> = arrayListOf(
            Relation.Builder().topics(Topics("sheedon/alarm_ack")).build(),
            Relation.Builder().keyword("test_ack").build()
        )

        // 订阅对象
        val subscribe = Subscribe.Builder() // 标准配置
            .add("sheedon/test_ack") // 通过Relation配置
            .add(relation) // 添加Relation集合
            .addAll(relations)
            .build()


        observable = okMqttClient.newObservable(subscribe)

        // 订阅全监听，响应+订阅+错误
        observable!!.enqueue(object : FullCallback {
            override fun onResponse(observable: Observable, response: Response) {
                binding.tvMessage.text = response.body!!.data
            }

            override fun onResponse(response: MqttWireMessage?) {
                binding.tvMessage.text = response?.toString() ?: "onResponse"
            }

            override fun onFailure(e: Throwable?) {
                binding.tvMessage.text = e!!.message
            }
        })

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