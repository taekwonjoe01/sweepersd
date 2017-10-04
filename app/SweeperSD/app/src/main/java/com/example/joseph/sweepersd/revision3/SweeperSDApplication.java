package com.example.joseph.sweepersd.revision3;

import android.app.Application;

import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneModelUpdater;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneRepository;

public class SweeperSDApplication extends Application {
    public static volatile boolean needsCoarsePermission;
    public static volatile boolean needsFinePermission;


    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level > Application.TRIM_MEMORY_BACKGROUND) {
            trimMemory();
        }
    }

    private void trimMemory() {
        WatchZoneRepository.getInstance(this).delete();
        WatchZoneModelUpdater.getInstance(this).delete();
        LimitRepository.getInstance(this).delete();
    }
}
