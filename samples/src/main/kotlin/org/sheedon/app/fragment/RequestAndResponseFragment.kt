package org.sheedon.app.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.sheedon.app.R
import org.sheedon.app.databinding.FragmentRequestResponseBinding
import org.sheedon.app.factory.MqttClient
import org.sheedon.mqtt.*

/**
 * mqtt请求响应
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:09
 */
class RequestAndResponseFragment : Fragment() {

    private lateinit var binding: FragmentRequestResponseBinding
    private val okMqttClient = MqttClient.getInstance().loadOkMqttClient(requireContext())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.label_request_and_response)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_request_response, container, false)
        binding.btnPublish.setOnClickListener { requestAndResponse() }

        return binding.root
    }

    private fun requestAndResponse() {
        val topicText = binding.etTopic.text
        val backTopicText = binding.etBackTopic.text
        val keywordText = binding.etKeyword.text
        if (topicText == null || TextUtils.isEmpty(topicText.toString())) {
            Toast.makeText(context, R.string.hint_topic, Toast.LENGTH_LONG).show()
            return
        }
        if ((backTopicText == null || TextUtils.isEmpty(backTopicText.toString()))
            && (keywordText == null || TextUtils.isEmpty(keywordText.toString()))
        ) {
            Toast.makeText(context, R.string.hint_topic_keyword, Toast.LENGTH_LONG).show()
            return
        }

        val topicStr = backTopicText?.toString() ?: ""
        val keywordStr = keywordText?.toString() ?: ""
        val qosValue = binding.spinnerQos.selectedItem.toString().toInt()
        val retainValue = binding.spinnerRetain.selectedItem.toString() == "true"
        val message = binding.etMessage.text.toString()


        val client = MqttClient.getInstance().loadOkMqttClient(requireContext())

        // 构建请求类
        val request = Request.Builder()
            .topic(topicText.toString(), qosValue, retainValue)
            .backTopic(topicStr)
            .keyword(keywordStr)
            .data(message)
            .build()

        // 得到Call
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                binding.tvMessage.text = response.body!!.data
            }

            override fun onFailure(e: Throwable?) {
                binding.tvMessage.text = e.toString()
            }
        })
    }
}