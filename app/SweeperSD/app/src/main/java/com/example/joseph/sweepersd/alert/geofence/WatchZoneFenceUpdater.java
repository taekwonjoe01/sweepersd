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
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WatchZoneFenceUpdater {
    private static final float GEOFENCE_RADIUS_PADDING = 100.0f;
    private final Context mApplicationContext;

    public WatchZoneFenceUpdater(Context context) {
        mApplicationContext = context.getApplicationContext();
    }

    public void updateGeofences(List<WatchZoneModel> models) {
        WatchZoneFenceDao dao = AppDatabase.getInstance(mApplicationContext).watchZoneFenceDao();
        List<WatchZoneFence> watchZoneFences = dao.getAllGeofences();

        Map<Long, WatchZoneFence> existingGeofences = new HashMap<>();
        for (WatchZoneFence watchZoneFence : watchZoneFences) {
            existingGeofences.put(watchZoneFence.getWatchZoneId(), watchZoneFence);
        }

        Set<Long> orphans = new HashSet<>(existingGeofences.keySet());
        List<WatchZoneModel> newModels = new ArrayList<>();
        for (WatchZoneModel model : models) {
            orphans.remove(model.watchZone.getUid());
            if (!existingGeofences.containsKey(model.watchZone.getUid())
                    && model.watchZone.getRemindPolicy() == WatchZone.REMIND_POLICY_NEARBY) {
                newModels.add(model);
            } else if (existingGeofences.containsKey(model.watchZone.getUid())){
                WatchZoneFence watchZoneFence = existingGeofences.get(model.watchZone.getUid());
                WatchZone watchZone = model.watchZone;
                if (watchZoneFence.getCenterLatitude() != watchZone.getCenterLatitude() ||
                        watchZoneFence.getCenterLongitude() != watchZoneFence.getCenterLongitude()
                        || watchZoneFence.getRadius() != watchZone.getRadius() ||
                        watchZone.getRemindPolicy() == WatchZone.REMIND_POLICY_ANYWHERE) {
                    orphans.add(model.watchZone.getUid());
                    if (watchZone.getRemindPolicy() == WatchZone.REMIND_POLICY_NEARBY) {
                        newModels.add(model);
                    }
                }
            }
        }

        if (!orphans.isEmpty() || !newModels.isEmpty()) {
            LocationServices.getGeofencingClient(mApplicationContext).removeGeofences(
                    getGeofencesPendingIntent());

            for (Long orphaned : orphans) {
                WatchZoneFence watchZoneFence = existingGeofences.get(orphaned);
                dao.delete(watchZoneFence);
                existingGeofences.remove(orphaned);
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

            for (WatchZoneModel model : newModels) {
                WatchZoneFence newWatchZoneFence = new WatchZoneFence();
                newWatchZoneFence.setWatchZoneId(model.watchZone.getUid());
                newWatchZoneFence.setInRegion(false);
                newWatchZoneFence.setCenterLatitude(model.watchZone.getCenterLatitude());
                newWatchZoneFence.setCenterLongitude(model.watchZone.getCenterLongitude());
                newWatchZoneFence.setRadius(model.watchZone.getRadius());
                long uid = dao.insertGeofence(newWatchZoneFence);
                newWatchZoneFence.setUid(uid);
                existingGeofences.put(model.watchZone.getUid(), newWatchZoneFence);
            }

            List<com.google.android.gms.location.Geofence> gmsFences = new ArrayList<>();
            for (WatchZoneFence fence : existingGeofences.values()) {
                gmsFences.add(new com.google.android.gms.location.Geofence.Builder()
                        // Set the request ID of the geofence. This is a string to identify this
                        // geofence.
                        .setRequestId(Long.toString(fence.getUid()))
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setCircularRegion(
                                fence.getCenterLatitude(),
                                fence.getCenterLongitude(),
                                (float)fence.getRadius() + GEOFENCE_RADIUS_PADDING
                        )
                        .setTransitionTypes(com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER |
                                com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build());
            }
            if (!gmsFences.isEmpty()) {
                LocationServices.getGeofencingClient(mApplicationContext).addGeofences(
                        getGeofencingRequest(gmsFences),
                        getGeofencesPendingIntent());
            }
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
