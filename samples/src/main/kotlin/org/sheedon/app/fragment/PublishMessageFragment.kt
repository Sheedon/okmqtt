package org.sheedon.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.sheedon.app.R
import org.sheedon.app.databinding.FragmentPublishMessageBinding
import org.sheedon.app.factory.MqttClient
import org.sheedon.mqtt.*

/**
 * 提交Mqtt message 消息
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:07
 */
class PublishMessageFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.label_publish_message)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentPublishMessageBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_publish_message, container, false)

        // 提交mqtt消息
        binding.btnPublish.setOnClickListener {
            publish(
                binding.etTopic.text.toString(),
                binding.spinnerQos.selectedItem.toString(),
                binding.spinnerRetain.selectedItem.toString(),
                binding.etMessage.text.toString()
            )
        }
        return binding.root
    }

    /**
     * 发送Mqtt消息请求
     *
     * @param topic   主题
     * @param qos     消息质量
     * @param retain  是否保留
     * @param message 消息
     */
    private fun publish(topic: String, qos: String, retain: String, message: String) {
        val qosValue = qos.toInt()
        val retainValue = retain == "true"
        val client = MqttClient.getInstance().loadOkMqttClient(requireContext())

        // 构建请求类
        val request = Request.Builder()
            .topic(topic, qosValue, retainValue)
            .data(message)
            .build()
        // 得到Call
        val call = client.newCall(request)
        call.publish()
    }
}