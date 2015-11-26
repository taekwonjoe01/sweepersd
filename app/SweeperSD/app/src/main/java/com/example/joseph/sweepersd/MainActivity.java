package com.example.joseph.sweepersd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button mLaunchMapsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLaunchMapsButton = (Button) findViewById(R.id.launch_maps);

        mLaunchMapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activityIntent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(activityIntent);
            }
        });

        Intent serviceIntent = new Intent(this, InitializeService.class);
        startService(serviceIntent);
    }
}
