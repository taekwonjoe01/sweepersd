package com.example.joseph.sweepersd.alert.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.example.joseph.sweepersd.AppDatabase;
import com.example.joseph.sweepersd.AppUpdateJob;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

public class GeofenceTransitionsService extends IntentService {
    public static final String TAG = GeofenceTransitionsService.class.getSimpleName();

    public GeofenceTransitionsService() {
        super("GeofenceTransitionsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "starting geofence transition service.");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "GeofenceTransitionService error " + geofencingEvent.getErrorCode());
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringFences = geofencingEvent.getTriggeringGeofences();
        WatchZoneFenceDao dao = AppDatabase.getInstance(this).watchZoneFenceDao();
        List<WatchZoneFence> fences = new ArrayList<>();
        for (Geofence geofence : triggeringFences) {
            long uid = Long.parseLong(geofence.getRequestId());
            WatchZoneFence fence = dao.getFenceByWatchZoneUid(uid);
            if (fence != null) {
                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    fence.setInRegion(true);
                } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    fence.setInRegion(false);
                }
                fences.add(fence);
            }
        }

        dao.updateGeofences(fences);

        AppUpdateJob.scheduleJob(this);
    }
}

