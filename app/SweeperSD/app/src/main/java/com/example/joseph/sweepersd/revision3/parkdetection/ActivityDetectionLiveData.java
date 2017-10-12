package com.example.joseph.sweepersd.revision3.parkdetection;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.Task;

/**
 * Created by joseph on 10/11/17.
 */

public class ActivityDetectionLiveData extends LiveData<Void> {

    private final Context mContext;

    private ActivityRecognitionClient mActivityRecognitionClient;

    public ActivityDetectionLiveData(Context context) {
        mContext = context;
    }

    @Override
    protected void onActive() {
        //mActivityRecognitionClient = ActivityRecognition.getClient(context);
        //Task task = activityRecognitionClient.requestActivityUpdates(180_000L, pendingIntent);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
    }
}
