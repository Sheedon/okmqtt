package org.sheedon.app.factory

import android.content.Context
import com.google.gson.Gson
import org.sheedon.mqtt.OkMqttClient
import org.sheedon.mqtt.Topics
import org.sheedon.mqtt.Topics.Companion.build
import java.util.*

/**
 * java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/1 10:51 上午
 */
class MqttClient {

    private var mClient: OkMqttClient? = null

    companion object {
        private val instance = MqttClient()

        @JvmStatic
        fun getInstance() = instance

        // current client id
        private val clientId = UUID.randomUUID().toString()

        // It is recommended that developers change to their own mqtt-Broker
        //    private static final String serverUri = "tcp://test.mosquitto.org:1883";
        private const val serverUri = "tcp://broker-cn.emqx.io:1883"

        // Topic to subscribe to by default.
        private val topicsBodies: List<Topics> = mutableListOf(build("mq/clouds/cmd/test", 0, true))
    }

    /**
     * 创建OkMqttClient
     */
    fun loadOkMqttClient(context: Context): OkMqttClient {
        if (mClient != null) {
            return mClient!!
        }
        val clientContext = context.applicationContext
        mClient = OkMqttClient.Builder() // Configure the basic parameters of mqtt connection
            .clientInfo(clientContext, serverUri, clientId)
            // 作用于关键字关联的响应信息解析器
            .subscribeBodies(topicsBodies = topicsBodies.toTypedArray())
            .keywordConverter(CallbackNameConverter(Gson()))
            .openLog(true)
            .build()
        return mClient!!
    }


}