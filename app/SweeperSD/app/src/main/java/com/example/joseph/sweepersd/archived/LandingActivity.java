package com.example.joseph.sweepersd.archived;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.archived.presentation.manualalarms.WatchZoneViewActivity;

/**
 * Opening Activity for SweeperSD.
 */
public class LandingActivity extends AppCompatActivity {
    private Button mManualAlarmsButton;
    private Button mAutomatedAlarmsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_advanced_settings, true);

        setContentView(R.layout.activity_landing);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mManualAlarmsButton = (Button) findViewById(R.id.button_alarms);
        mAutomatedAlarmsButton = (Button) findViewById(R.id.button_location_monitoring);

        mManualAlarmsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LandingActivity.this, WatchZoneViewActivity.class));
            }
        });

        mAutomatedAlarmsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LandingActivity.this, "Coming soon", Toast.LENGTH_LONG).show();
            }
        });

        Intent serviceIntent = new Intent(this, SweeperService.class);
        startService(serviceIntent);
    }
}
