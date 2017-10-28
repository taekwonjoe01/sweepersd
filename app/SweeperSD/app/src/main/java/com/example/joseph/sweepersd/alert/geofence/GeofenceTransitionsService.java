package com.example.joseph.sweepersd.alert.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

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
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "GeofenceTransitionService error " + geofencingEvent.getErrorCode());
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringFences = geofencingEvent.getTriggeringGeofences();
        List<Long> triggeringWatchZoneUids = new ArrayList<>();
        for (Geofence fence : triggeringFences) {
            triggeringWatchZoneUids.add(Long.parseLong(fence.getRequestId()));
        }
        /*Long watchZoneUid = triggeringFences.get(0).getRequestId();
        geofencingEvent.getTriggeringLocation();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );

            // Send notification and log the transition details.
            sendNotification(geofenceTransitionDetails);
            Log.i(TAG, geofenceTransitionDetails);
        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type,
                    geofenceTransition));
        }*/
    }
}

