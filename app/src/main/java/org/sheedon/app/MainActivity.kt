package org.sheedon.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import org.json.JSONException
import org.json.JSONObject
import org.sheedon.app.factory.MqttClient
import org.sheedon.mqtt.Callback
import org.sheedon.mqtt.OkMqttClient
import org.sheedon.mqtt.Request
import org.sheedon.mqtt.Response

class MainActivity : AppCompatActivity() {

    val client: OkMqttClient = MqttClient.getInstance().getClient()!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val request: Request = Request.Builder()
            .backTopic("get_manager_list")
            .delaySecond(10)
            .build()

        val observable = client.newObservable(request)
        observable.subscribe(object : Callback {
            override fun onFailure(e: Throwable) {
                Log.v("TAG", "e:$e")
            }

            override fun onResponse(request: Request, response: Response) {
                Log.v("TAG", "response:${response.body()}")
            }

        })
    }

    fun onTouchClick(view: View) {

        val jsonObject = JSONObject()
        try {
            jsonObject.put("type", "get_manager_list")
            jsonObject.put("upStartTime", "")
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val request = Request.Builder()
            .backTopic("get_manager_list")
            .data(jsonObject.toString())
            .build()

        val call = client.newCall(request)
        call.enqueue(object :Callback{
            override fun onFailure(e: Throwable) {
                Log.v("TAG", "e:$e")
            }

            override fun onResponse(request: Request, response: Response) {
                Log.v("TAG", "response:${response.body()}")
            }
        })

    }
}