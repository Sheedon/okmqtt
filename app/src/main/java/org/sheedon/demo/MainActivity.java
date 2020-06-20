package org.sheedon.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;
import org.sheedon.demo.factory.MqttClient;
import org.sheedon.mqtt.Call;
import org.sheedon.mqtt.Callback;
import org.sheedon.mqtt.Observable;
import org.sheedon.mqtt.OkMqttClient;
import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.RequestBuilder;
import org.sheedon.mqtt.Response;
import org.sheedon.mqtt.ResponseBody;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final OkMqttClient client = MqttClient.getInstance().getClient();

        Request request = new RequestBuilder()
                .backName("get_manager_list")
                .build();

        final Observable observable = client.newObservable(request);
        observable.subscribe(new Callback<Response>() {
            @Override
            public void onFailure(Throwable e) {
                System.out.println(e.getMessage());
            }

            @Override
            public void onResponse(Response response) {
                ResponseBody body = response.body();
                System.out.println(body == null?"":body.getBody());
                observable.cancel();
            }
        });

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

                Request request = new RequestBuilder()
                        .payload(jsonObject.toString())
                        .backName("get_manager_list")
                        .build();

                Call call = client.newCall(request);
                call.enqueue(new Callback<Response>() {
                    @Override
                    public void onFailure(Throwable e) {

                    }

                    @Override
                    public void onResponse(Response response) {

                    }
                });

//                MqttClient.getInstance().publish(jsonObject.toString(), "get_manager_list", new Callback() {
//                    @Override
//                    public void onFailure(Throwable e) {
//                        System.out.println(e.getMessage());
//                    }
//
//                    @Override
//                    public void onResponse(Response response) {
//                        ResponseBody body = response.body();
//                        System.out.println(body == null?"":body.getBody());
//                    }
//                });
            }
        });
    }
}
