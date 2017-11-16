package com.example.joseph.sweepersd;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.joseph.sweepersd.utils.LongPreferenceLiveData;
import com.example.joseph.sweepersd.utils.Preferences;

import java.util.Date;

public class DebugActivity extends AppCompatActivity {
    private TextView mUpdaterLastStarted;
    private TextView mUpdaterLastFinished;
    private Button mScheduleUpdater;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        mScheduleUpdater = findViewById(R.id.button_schedule_updater);
        mUpdaterLastStarted = findViewById(R.id.textview_updater_started);
        mUpdaterLastFinished = findViewById(R.id.textview_updater_finished);

        LongPreferenceLiveData updaterStart = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_APP_UPDATER_LAST_STARTED);
        updaterStart.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mUpdaterLastStarted.setText("never");
                    } else {
                        mUpdaterLastStarted.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        LongPreferenceLiveData updaterFinish = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_APP_UPDATER_LAST_FINISHED);
        updaterFinish.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mUpdaterLastFinished.setText("never");
                    } else {
                        mUpdaterLastFinished.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        mScheduleUpdater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppUpdateJob.scheduleJob(DebugActivity.this);
            }
        });
    }

}
