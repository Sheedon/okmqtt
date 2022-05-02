package org.sheedon.app.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.sheedon.sample.viewmodel.SubscribeTopicViewModel
import org.sheedon.app.R
import org.sheedon.app.databinding.FragmentSubscribeTopicBinding
import org.sheedon.app.factory.MqttClient
import org.sheedon.mqtt.*

/**
 * 订阅一个主题
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:09
 */
class SubscribeTopicFragment : Fragment() {

    private lateinit var binding: FragmentSubscribeTopicBinding
    private val viewModel = SubscribeTopicViewModel()
    private var observable: Observable? = null
    private val okMqttClient = MqttClient.getInstance().loadOkMqttClient(requireContext())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.label_subscribe_topic)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_subscribe_topic, container, false)
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
        observable!!.cancel()
        viewModel.isSubscribe.set(false)
    }

    private fun subscribe() {
        val topicText = binding.etTopic.text
        val keywordText = binding.etKeyword.text
        if ((topicText == null || TextUtils.isEmpty(topicText.toString()))
            && (keywordText == null || TextUtils.isEmpty(keywordText.toString()))
        ) {
            Toast.makeText(context, R.string.hint_topic_keyword, Toast.LENGTH_LONG).show()
            return
        }

        viewModel.isSubscribe.set(true)
        val topicStr = topicText?.toString() ?: ""
        val keywordStr = keywordText?.toString() ?: ""
        val request = Request.Builder()
            .backTopic(topicStr)
            .keyword(keywordStr)
            .build()


        observable = okMqttClient.newObservable(request)

        observable?.enqueue(object : ObservableBack {
            override fun onResponse(observable: Observable, response: Response) {
                binding.tvMessage.text = response.body!!.data
            }

            override fun onFailure(e: Throwable?) {}
        })
    }
}