package org.sheedon.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.sheedon.app.factory.MqttClient

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 连接mqtt
        MqttClient.getInstance().loadOkMqttClient(this)

    }
}