package com.example.joseph.sweepersd.watchzone;

import androidx.lifecycle.Observer;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.joseph.sweepersd.AppUpdateJob;
import com.example.joseph.sweepersd.alert.AlertManager;
import com.example.joseph.sweepersd.alert.geofence.WatchZoneFenceManager;
import com.example.joseph.sweepersd.limit.AddressValidatorManager;
import com.example.joseph.sweepersd.scheduling.ScheduleManager;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelUpdater;

import java.util.Map;

public abstract class WatchZoneBaseActivity extends AppCompatActivity {
    public abstract void onWatchZoneUpdateProgress(Map<Long, Integer> progressMap);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WatchZoneModelUpdater.getInstance(this).observe(this, new Observer<Map<Long, Integer>>() {
            @Override
            public void onChanged(@Nullable Map<Long, Integer> longIntegerMap) {
                if (longIntegerMap != null) {
                    onWatchZoneUpdateProgress(longIntegerMap);
                }
            }
        });
        ScheduleManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                // Do nothing.
            }
        });
        WatchZoneFenceManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                // Do nothing.
            }
        });
        AlertManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                // Do nothing
            }
        });
        AddressValidatorManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                // Do nothing
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        AppUpdateJob.cancelJob(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        AppUpdateJob.scheduleJob(this);
    }
}
