package org.sheedon.app.factory

import com.google.gson.Gson
import org.sheedon.app.App
import org.sheedon.mqtt.*
import kotlin.collections.ArrayList

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
    }

    init {
        createClient()
    }

    fun getClient() = mClient

    private fun createClient() {

        // 创建MqttClient
        val clientId = "" // 设置设备编号

        val serverUri = "" // 设置服务器地址
        val subscribeBodies: MutableList<SubscribeBody> = ArrayList()
        subscribeBodies.add(
            SubscribeBody.build(
                "yh_classify/clouds/garbage/cmd/LJTF2020072001",
                1
            )
        ) // 添加需要订阅主题
        subscribeBodies.add(
            SubscribeBody.build(
                "yh_classify/clouds/recyclable/cmd/yhkhs20181029046",
                1
            )
        ) // 添加需要订阅主题


        if (mClient == null) {
            mClient = OkMqttClient.Builder()
                .clientInfo(App.instance, serverUri, clientId)
                .subscribeBodies(subscribeBodies = subscribeBodies.toTypedArray())
                .baseTopic("yh_classify/device/recyclable/data/yhkhs20181029046") // 添加基础主题
                .addBackTopicConverter(CallbackNameConverter(Gson()))
                .build()
        }
    }


}