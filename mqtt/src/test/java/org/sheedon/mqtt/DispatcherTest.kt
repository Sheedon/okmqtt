package org.sheedon.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.junit.Assert
import org.junit.Test
import org.sheedon.mqtt.internal.DataConverter
import org.sheedon.mqtt.internal.IDispatchManager
import org.sheedon.mqtt.internal.binder.IBindHandler
import org.sheedon.mqtt.internal.binder.IRequestHandler
import org.sheedon.mqtt.internal.concurrent.EventBehavior
import org.sheedon.mqtt.internal.concurrent.EventBehaviorService
import org.sheedon.mqtt.internal.connection.RealCall
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/10 5:06 下午
 */
class DispatcherTest {

    private val dispatcher = Dispatcher(arrayListOf(KeywordConverter()), 3000)
    private val dispatcherManager = DispatcherManager()

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private var testTopic: String? = ""
    private var testMessage: MqttMessage? = null


    @Test
    fun request() {
        val request = loadRequest()
        val realCall = RealCall(dispatcherManager, request)
        val (publishId, timeout) = dispatcher.subscribe(realCall, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                println("response:$response")
            }

            override fun onFailure(e: Throwable?) {
                println("e:$e")
            }

        })

        executor.submit {
            TimeUnit.SECONDS.sleep(1)
            dispatcher.unsubscribe(publishId)
            println("dispatcher:${dispatcher}")
        }

        println("publishId:$publishId")
        println("timeout:$timeout")

        TimeUnit.SECONDS.sleep(10)
    }

    private fun loadRequest(): Request {
        val request = Request.Builder()
            .topic("test")
            .data("{\"name\":\"sheedon\"}")
            .topics("test_back", attachRecord = true)
            .build()

        testTopic = request.body.topic
        testMessage = request.body

        return request
    }

    inner class DispatcherManager : IDispatchManager {
        override fun requestHandler() = object : IRequestHandler {
            override fun checkRequestData(data: RequestBody) = data

            override fun publish(topic: String, message: MqttMessage): IMqttDeliveryToken {
                return MqttDeliveryToken()
            }

            override fun subscribe(vararg body: Topics, listener: IMqttActionListener?) {
//                Assert.assertEquals(this@DispatcherTest.testTopic, body)
                listener?.onSuccess(MqttToken())
            }

            override fun unsubscribe(vararg body: Topics, listener: IMqttActionListener?) {
                listener?.onSuccess(MqttToken())
            }

        }

        override fun eventBehavior() = EventBehaviorService()

        override fun bindHandler(): IBindHandler {
            return this@DispatcherTest.dispatcher
        }

    }


    inner class KeywordConverter : DataConverter<ResponseBody, String> {
        override fun convert(value: ResponseBody): String? {
            return value.topic
        }

    }
}