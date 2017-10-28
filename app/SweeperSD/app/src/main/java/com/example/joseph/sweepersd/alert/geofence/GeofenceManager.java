package com.example.joseph.sweepersd.alert.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import com.example.joseph.sweepersd.AppDatabase;
import com.example.joseph.sweepersd.utils.PendingIntents;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeofenceManager {
    private static final float GEOFENCE_RADIUS_PADDING = 100.0f;
    private final Context mApplicationContext;

    public GeofenceManager(Context context) {
        mApplicationContext = context.getApplicationContext();
    }

    public void updateGeofences(List<WatchZoneModel> models) {
        GeofenceDao dao = AppDatabase.getInstance(mApplicationContext).geofenceDao();
        List<Geofence> geofences = dao.getAllGeofences();

        Map<Long, Geofence> existingGeofences = new HashMap<>();
        for (Geofence geofence : geofences) {
            existingGeofences.put(geofence.getWatchZoneId(), geofence);
        }

        Set<Long> orphans = new HashSet<>(existingGeofences.keySet());
        List<WatchZoneModel> newModels = new ArrayList<>();
        for (WatchZoneModel model : models) {
            orphans.remove(model.getWatchZoneUid());
            if (!existingGeofences.containsKey(model.getWatchZoneUid())) {
                newModels.add(model);
            } else {
                Geofence geofence = existingGeofences.get(model.getWatchZoneUid());
                WatchZone watchZone = model.getWatchZone();
                if (geofence.getCenterLatitude() != watchZone.getCenterLatitude() ||
                        geofence.getCenterLongitude() != geofence.getCenterLongitude()
                        || geofence.getRadius() != watchZone.getRadius()) {
                    orphans.add(model.getWatchZoneUid());
                    newModels.add(model);
                }
            }
        }

        if (!orphans.isEmpty() || !newModels.isEmpty()) {
            LocationServices.getGeofencingClient(mApplicationContext).removeGeofences(
                    getGeofencesPendingIntent());

            for (Long orphaned : orphans) {
                Geofence geofence = existingGeofences.get(orphaned);
                dao.delete(geofence);
                existingGeofences.remove(orphaned);
            }
            for (WatchZoneModel model : newModels) {
                Geofence newGeofence = new Geofence();
                newGeofence.setWatchZoneId(model.getWatchZoneUid());
                newGeofence.setInRegion(false);
                newGeofence.setCenterLatitude(model.getWatchZone().getCenterLatitude());
                newGeofence.setCenterLongitude(model.getWatchZone().getCenterLongitude());
                newGeofence.setRadius(model.getWatchZone().getRadius());
                dao.insertGeofence(newGeofence);
                existingGeofences.put(model.getWatchZoneUid(), newGeofence);
            }

            List<com.google.android.gms.location.Geofence> gmsFences = new ArrayList<>();
            for (Geofence fence : existingGeofences.values()) {
                gmsFences.add(new com.google.android.gms.location.Geofence.Builder()
                        // Set the request ID of the geofence. This is a string to identify this
                        // geofence.
                        .setRequestId(Long.toString(fence.getWatchZoneId()))
                        .setCircularRegion(
                                fence.getCenterLatitude(),
                                fence.getCenterLongitude(),
                                (float)fence.getRadius() + GEOFENCE_RADIUS_PADDING
                        )
                        .setTransitionTypes(com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER |
                                com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build());
            }
            if (ActivityCompat.checkSelfPermission(mApplicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            LocationServices.getGeofencingClient(mApplicationContext).addGeofences(
                    getGeofencingRequest(gmsFences),
                    getGeofencesPendingIntent());
        }
    }

    private GeofencingRequest getGeofencingRequest(List<com.google.android.gms.location.Geofence> geofences) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        return builder.build();
    }

    private PendingIntent getGeofencesPendingIntent() {
        Intent intent = new Intent(mApplicationContext, GeofenceTransitionsService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(mApplicationContext, PendingIntents.REQUEST_CODE_ALARM, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

}
