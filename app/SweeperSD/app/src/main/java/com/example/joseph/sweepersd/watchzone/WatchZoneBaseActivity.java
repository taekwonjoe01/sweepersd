package com.example.joseph.sweepersd.watchzone;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.example.joseph.sweepersd.alert.AlertManager;
import com.example.joseph.sweepersd.scheduling.ScheduleManager;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneBaseObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelUpdater;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;

import java.util.ArrayList;
import java.util.Map;

public abstract class WatchZoneBaseActivity extends AppCompatActivity {
    public abstract void onWatchZoneUpdateProgress(Map<Long, Integer> progressMap);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WatchZoneModelRepository.getInstance(this).observe(this, new WatchZoneModelsObserver(
                new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
            @Override
            public void onModelsChanged(Map<Long, WatchZoneModel> models,
                                        WatchZoneBaseObserver.ChangeSet changeSet) {
                AlertManager alertManager = new AlertManager(WatchZoneBaseActivity.this);
                alertManager.updateAlertNotification(new ArrayList<>(models.values()));
                ScheduleManager scheduleManager = new ScheduleManager(WatchZoneBaseActivity.this);
                scheduleManager.scheduleWatchZones(new ArrayList<>(models.values()));
            }
            @Override
            public void onDataLoaded(Map<Long, WatchZoneModel> models) {
                AlertManager alertManager = new AlertManager(WatchZoneBaseActivity.this);
                alertManager.updateAlertNotification(new ArrayList<>(models.values()));
                ScheduleManager scheduleManager = new ScheduleManager(WatchZoneBaseActivity.this);
                scheduleManager.scheduleWatchZones(new ArrayList<>(models.values()));
            }
            @Override
            public void onDataInvalid() {
                // Do nothing
            }
        }));
        WatchZoneModelUpdater.getInstance(this).observe(this, new Observer<Map<Long, Integer>>() {
            @Override
            public void onChanged(@Nullable Map<Long, Integer> longIntegerMap) {
                if (longIntegerMap != null) {
                    onWatchZoneUpdateProgress(longIntegerMap);
                }
            }
        });
    }
}
