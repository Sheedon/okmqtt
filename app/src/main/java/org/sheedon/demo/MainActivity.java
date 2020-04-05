package org.sheedon.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;
import org.sheedon.demo.factory.MqttClient;
import org.sheedon.mqtt.Callback;
import org.sheedon.mqtt.Response;
import org.sheedon.mqtt.ResponseBody;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MqttClient.getInstance();

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", "get_manager_list");
                    jsonObject.put("upStartTime", "");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                MqttClient.getInstance().publish(jsonObject.toString(), "get_manager_list", new Callback() {
                    @Override
                    public void onFailure(Throwable e) {
                        System.out.println(e.getMessage());
                    }

                    @Override
                    public void onResponse(Response response) {
                        ResponseBody body = response.body();
                        System.out.println(body == null?"":body.getBody());
                    }
                });
            }
        });
    }
}
