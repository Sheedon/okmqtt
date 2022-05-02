package org.sheedon.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import org.sheedon.app.R
import org.sheedon.app.databinding.FragmentOkMqttBinding

/**
 * okMqtt使用
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:12
 */
class OkMqttFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentOkMqttBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_ok_mqtt, container, false)

        // 到发送mqtt消息页面
        binding.btnPublishMessage.setOnClickListener {
            nav().navigate(R.id.action_to_publish_message)
        }
        // 到订阅消息页面
        binding.btnSubscribeTopic.setOnClickListener {
            nav().navigate(R.id.action_to_subscribe_topic)
        }
        // 到请求响应页面
        binding.btnRr.setOnClickListener {
            nav().navigate(R.id.action_to_request_and_response)
        }
        // 到订阅一个组页面
        binding.btnSubscribeArray.setOnClickListener {
            nav().navigate(R.id.action_to_subscribe_array)
        }

        return binding.root
    }

    private fun nav(): NavController {
        return NavHostFragment.findNavController(this)
    }
}