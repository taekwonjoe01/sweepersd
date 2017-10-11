package com.example.joseph.sweepersd.revision3.parkdetection;

import android.arch.lifecycle.LiveData;
import android.content.Context;

/**
 * Created by josephhutchins on 10/10/17.
 */

public class ParkDetectionModel extends LiveData<ParkDetectionModel> {
    private static ParkDetectionModel sInstance;

    private final Context mApplicationContext;

    private ParkDetectionModel(Context context) {
        mApplicationContext = context.getApplicationContext();
    }

    public static synchronized ParkDetectionModel getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ParkDetectionModel(context);
        }
        return sInstance;
    }

    public synchronized void delete() {
        if (sInstance != null) {
            sInstance = null;
        }
    }
}
