package org.sheedon.mqtt

import org.sheedon.rr.core.DispatchAdapter
import org.sheedon.rr.core.RequestAdapter

/**
 * The data exchange intermediary is used to send the request message under the request-response model to the mqttClient,
 * and the mqttClient "subscribed message" is fed back to the "request-response module"
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 10:27 上午
 */
internal class SwitchMediator internal constructor(
    _baseTopic: String = "",
    _charsetName: String = "UTF-8",
    _requestAdapter: RequestAdapter<RequestBody>? = null
) : DispatchAdapter.AbstractDispatchImpl<RequestBody, ResponseBody>() {

    private val charsetName: String = if (_charsetName.isEmpty()) {
        "UTF-8"
    } else {
        _charsetName
    }
    private val requestAdapter: RequestAdapter<RequestBody> =
        _requestAdapter ?: MqttRequestAdapter(_baseTopic, charsetName)

    override fun loadRequestAdapter(): RequestAdapter<RequestBody> {
        return requestAdapter
    }
}