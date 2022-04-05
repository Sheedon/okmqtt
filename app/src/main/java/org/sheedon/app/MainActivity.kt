package org.sheedon.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import org.json.JSONException
import org.json.JSONObject
import org.sheedon.app.factory.MqttClient
import org.sheedon.mqtt.*

class MainActivity : AppCompatActivity() {

    val client: OkMqttClient = MqttClient.getInstance().getClient()!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val request: Request = Request.Builder()
            .backTopic("get_manager_list")
            .topic("yh_classify/device/recyclable/data/yhkhs20181029046")
            .delaySecond(10)
            .build()

        val observable = client.newObservable(request)
        observable.enqueue(object :Callback{
            override fun onResponse(call: Call, response: Response) {
                Log.v("TAG", "response:${response.body}")
            }

            override fun onFailure(e: Throwable?) {
                Log.v("TAG", "e:$e")
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
            .delaySecond(100)
            .topic("yh_classify/device/recyclable/data/yhkhs20181029046")
            .backTopic("yh_classify/clouds/recyclable/cmd/yhkhs20181029046")
//            .keyword("get_manager_list")
            .data(jsonObject.toString())
            .build()

        val call = client.newCall(request)
        call.enqueue(object :Callback{

            override fun onResponse(call: Call, response: Response) {
                Log.v("TAG", "response:${response.body}")
            }

            override fun onFailure(e: Throwable?) {
                Log.v("TAG", "e:$e")
            }
        })

//        MqttClient.getInstance().getClient()?.subscribe(Topics.build(
//            "yh_classify/clouds/recyclable/cmd/yhkhs20181029046",
//            1
//        ))

    }
}