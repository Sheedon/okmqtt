package org.sheedon.sample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;
import android.view.View;

import org.sheedon.app.R;

public class OkMqttActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ok_mqtt);

        // 连接mqtt
        OkMqttContributors.getInstance().loadOkMqttClient(this);
    }
}