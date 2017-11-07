package com.example.joseph.sweepersd.scheduling;

import android.arch.lifecycle.LiveData;

public class LastAlarm extends LiveData<Long> {
    private static LastAlarm sInstance;

    private LastAlarm() {
        postValue(System.currentTimeMillis());
    }

    public static LastAlarm getInstance() {
        if (sInstance == null) {
            sInstance = new LastAlarm();
        }
        return sInstance;
    }

    public void postAlarm() {
        postValue(System.currentTimeMillis());
    }
}
