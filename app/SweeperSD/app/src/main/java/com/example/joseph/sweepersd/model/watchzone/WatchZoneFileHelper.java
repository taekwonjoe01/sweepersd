package com.example.joseph.sweepersd.model.watchzone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.example.joseph.sweepersd.model.limits.LimitDbHelper;
import com.example.joseph.sweepersd.model.limits.LimitSchedule;
import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Helper class for loading and saving watch zones to disk.
 */
public class WatchZoneFileHelper {
    private static final String TAG = WatchZoneFileHelper.class.getSimpleName();

    private static final String FILE_SWEEPING_LOCATIONS = "sweeping_locations.txt";
    private static final String FILE_WATCH_ZONE_DETAILS = "alarm_details.txt";

    private static final String SEPARATOR_SWEEPING_LOCATIONS = "::";

    public static final String ACTION_ALARM_UPDATED =
            "com.example.joseph.sweepersd.ACTION_ALARM_UPDATED";
    public static final String ACTION_ALARM_DELETED =
            "com.example.joseph.sweepersd.ACTION_ALARM_DELETED";

    public static final String ALARM_TIMESTAMP = "ALARM_TIMESTAMP";

    private static Semaphore mSemaphore = new Semaphore(1);

    private final Context mContext;
    private WeakReference<WatchZoneUpdateListener> mWatchZoneUpdateListener;

    public interface WatchZoneUpdateListener {
        void onWatchZoneUpdated(long createdTimestamp);
        void onWatchZoneDeleted(long createdTimestamp);
    }

    public WatchZoneFileHelper(Context context, WatchZoneUpdateListener listener) {
        mContext = context;
        mWatchZoneUpdateListener = new WeakReference<>(listener);

        if (listener != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_ALARM_UPDATED);
            filter.addAction(ACTION_ALARM_DELETED);
            mContext.registerReceiver(mWatchZoneUpdateReceiver, filter);
        }
    }

    public void setWatchZoneUpdateListener(WatchZoneUpdateListener listener) {
        if (listener == null) {
            mContext.unregisterReceiver(mWatchZoneUpdateReceiver);
        }
        mWatchZoneUpdateListener = new WeakReference<>(listener);
    }

    public List<Long> getWatchZoneList() {
        List<Long> results = new ArrayList<>();

        File alarmsDir = new File(getAlarmDirPath(mContext));
        File[] alarmDirs = null;

        try {
            mSemaphore.acquire();
            if (alarmsDir.exists()) {
                alarmDirs = alarmsDir.listFiles();
            }
            mSemaphore.release();
        } catch (InterruptedException e) {

        }

        if (alarmDirs != null) {
            for (File alarmDir : alarmDirs) {
                long watchZone = Long.parseLong(alarmDir.getName());
                results.add(watchZone);
            }
        }

        return results;
    }

    public List<WatchZone> loadWatchZones() {
        List<WatchZone> results = new ArrayList<>();

        File alarmsDir = new File(getAlarmDirPath(mContext));
        File[] alarmDirs = null;

        try {
            mSemaphore.acquire();
            if (alarmsDir.exists()) {
                alarmDirs = alarmsDir.listFiles();
            }
            mSemaphore.release();
        } catch (InterruptedException e) {

        }

        if (alarmDirs != null) {
            for (File alarmDir : alarmDirs) {
                WatchZone watchZone = loadWatchZone(Long.parseLong(alarmDir.getName()));
                if (watchZone != null) {
                    results.add(watchZone);
                } else {
                    Log.e(TAG, "WatchZone was corrupt. " + alarmDir.getName());
                }
            }
        }
        return results;
    }

    public WatchZone loadWatchZoneBrief(long createdTimestamp) {
        WatchZoneBuilder builder = new WatchZoneBuilder();
        try {
            mSemaphore.acquire();
            try {
                File dir = new File(getAlarmDirPath(mContext) + createdTimestamp);
                if (dir.exists()) {
                    builder.createdTimestamp = createdTimestamp;

                    File alarmDetailsFile = new File(dir.getAbsolutePath() + "/" +
                            FILE_WATCH_ZONE_DETAILS);
                    File sweepingLocationsFile = new File(dir.getAbsolutePath() + "/" +
                            FILE_SWEEPING_LOCATIONS);

                    InputStream ADis = new FileInputStream(alarmDetailsFile);


                    if (ADis != null) {
                        InputStreamReader inputStreamReader = new InputStreamReader(ADis);
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                        String receiveString = bufferedReader.readLine();
                        String[] parsings = receiveString.split(",");
                        builder.latitude = Double.parseDouble(parsings[0]);
                        builder.longitude = Double.parseDouble(parsings[1]);

                        receiveString = bufferedReader.readLine();
                        builder.label = receiveString;

                        receiveString = bufferedReader.readLine();
                        builder.radius = Integer.parseInt(receiveString);

                        receiveString = bufferedReader.readLine();
                        builder.lastUpdatedTimestamp = Long.parseLong(receiveString);

                        ADis.close();
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.toString());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } finally {
                mSemaphore.release();
            }
        } catch (InterruptedException e) {

        }

        return builder.build();
    }

    public WatchZone loadWatchZone(long createdTimestamp) {
        WatchZoneBuilder builder = new WatchZoneBuilder();
        try {
            mSemaphore.acquire();
            try {
                File dir = new File(getAlarmDirPath(mContext) + createdTimestamp);
                if (dir.exists()) {
                    builder.createdTimestamp = createdTimestamp;

                    File alarmDetailsFile = new File(dir.getAbsolutePath() + "/" +
                            FILE_WATCH_ZONE_DETAILS);
                    File sweepingLocationsFile = new File(dir.getAbsolutePath() + "/" +
                            FILE_SWEEPING_LOCATIONS);

                    InputStream ADis = new FileInputStream(alarmDetailsFile);


                    if (ADis != null) {
                        InputStreamReader inputStreamReader = new InputStreamReader(ADis);
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                        String receiveString = bufferedReader.readLine();
                        String[] parsings = receiveString.split(",");
                        builder.latitude = Double.parseDouble(parsings[0]);
                        builder.longitude = Double.parseDouble(parsings[1]);

                        receiveString = bufferedReader.readLine();
                        builder.label = receiveString;

                        receiveString = bufferedReader.readLine();
                        builder.radius = Integer.parseInt(receiveString);

                        receiveString = bufferedReader.readLine();
                        builder.lastUpdatedTimestamp = Long.parseLong(receiveString);

                        ADis.close();
                    }

                    if (sweepingLocationsFile.exists()) {
                        InputStream SLis = new FileInputStream(sweepingLocationsFile);
                        if (SLis != null) {
                            builder.positionStubs = new ArrayList<>();

                            InputStreamReader inputStreamReader = new InputStreamReader(SLis);
                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                            String receiveString = "";

                            while ((receiveString = bufferedReader.readLine()) != null) {
                                String[] parsings = receiveString.split(
                                        SEPARATOR_SWEEPING_LOCATIONS);

                                if (parsings.length == 3) {
                                    SweepingPositionStub stub = new SweepingPositionStub();

                                    String latLngData = parsings[0];
                                    stub.latitude = Double.parseDouble(latLngData.split(",")[0]);
                                    stub.longitude = Double.parseDouble(latLngData.split(",")[1]);

                                    String address = parsings[1];
                                    stub.address = TextUtils.isEmpty(address) ? "" : address;
                                    String limitId = parsings[2];
                                    stub.limitId = TextUtils.isEmpty(limitId) ? -1 :
                                            Integer.parseInt(parsings[2]);

                                    builder.positionStubs.add(stub);
                                }
                            }

                            SLis.close();
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.toString());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } finally {
                mSemaphore.release();
            }
        } catch (InterruptedException e) {

        }

        return builder.build();
    }

    public boolean saveWatchZone(WatchZone watchZone) {
        boolean result = false;

        Long tsLong = watchZone.getCreatedTimestamp();
        String ts = tsLong.toString();
        try {
            mSemaphore.acquire();
            File alarmDir = new File(getAlarmDirPath(mContext) + ts);
            boolean dirsMade = alarmDir.exists();
            if (!dirsMade) {
                dirsMade = alarmDir.mkdirs();
            }
            if (!dirsMade) {
                Log.e(TAG, "Failed to create alarmDir " + alarmDir.getAbsolutePath());
                mSemaphore.release();
            } else {
                try {
                    File alarmDetailsFile = new File(alarmDir,
                            FILE_WATCH_ZONE_DETAILS);
                    File sweepingLocationsFile = new File(alarmDir,
                            FILE_SWEEPING_LOCATIONS);
                    alarmDetailsFile.createNewFile();
                    sweepingLocationsFile.createNewFile();

                    FileWriter ADwriter = new FileWriter(alarmDetailsFile);

                    if (ADwriter != null) {
                        String LatLng = watchZone.getCenter().latitude + "," +
                                watchZone.getCenter().longitude;
                        ADwriter.write(LatLng + "\n");

                        if (TextUtils.isEmpty(watchZone.getLabel())) {
                            ADwriter.write("Unlabeled" + "\n");
                        } else {
                            ADwriter.write(watchZone.getLabel() + "\n");
                        }

                        ADwriter.write(watchZone.getRadius() + "\n");

                        ADwriter.write(watchZone.getLastUpdatedTimestamp() + "\n");

                        ADwriter.close();
                    }

                    if (watchZone.getSweepingAddresses() != null) {
                        FileWriter SLwriter = new FileWriter(sweepingLocationsFile);
                        if (SLwriter != null) {
                            for (SweepingAddress pos : watchZone.getSweepingAddresses()) {
                                String LatLng = pos.getLatLng().latitude + "," +
                                        pos.getLatLng().longitude;
                                String address = pos.getAddress() == null ? "" : pos.getAddress();
                                String limitId = pos.getLimit() == null ? "" :
                                        pos.getLimit().getId() + "";

                                SLwriter.write(LatLng + SEPARATOR_SWEEPING_LOCATIONS +
                                        address + SEPARATOR_SWEEPING_LOCATIONS +
                                        limitId + "\n");
                            }

                            SLwriter.close();
                        }
                    } else {
                        sweepingLocationsFile.delete();
                    }
                    result = true;
                    sendUpdatedBroadcast(tsLong);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, e.toString());
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                } finally {
                    mSemaphore.release();
                }
            }
        } catch (InterruptedException e) {

        }
        return result;
    }

    public boolean deleteWatchZone(Long watchZoneTimestamp) {
        boolean result = false;

        String ts = watchZoneTimestamp.toString();
        try {
            mSemaphore.acquire();
            String path = mContext.getFilesDir() + "/alarms/" + ts;
            File alarmDir = new File(path);
            if (alarmDir.exists()) {
                if (alarmDir.isDirectory()) {
                    String[] children = alarmDir.list();
                    for (int i = 0; i < children.length; i++) {
                        new File(alarmDir, children[i]).delete();
                    }
                }
                alarmDir.delete();
                result = true;
            }
            mSemaphore.release();
            if (result) {
                sendDeletedBroadcast(watchZoneTimestamp);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
        return result;
    }

    public static String getAlarmDirPath(Context context) {
         return context.getFilesDir() + "/alarms/";
    }

    private void sendUpdatedBroadcast(long createdTimestamp) {
        Bundle bundle = new Bundle();
        bundle.putLong(ALARM_TIMESTAMP, createdTimestamp);

        sendBroadcast(bundle, ACTION_ALARM_UPDATED);
    }

    private void sendDeletedBroadcast(long createdTimestamp) {
        Bundle bundle = new Bundle();
        bundle.putLong(ALARM_TIMESTAMP, createdTimestamp);

        sendBroadcast(bundle, ACTION_ALARM_DELETED);
    }

    private void sendBroadcast(Bundle bundle, String action) {
        Intent intent = new Intent(action);
        intent.putExtras(bundle);
        mContext.sendBroadcast(intent);
    }

    private final BroadcastReceiver mWatchZoneUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WatchZoneUpdateListener listener = mWatchZoneUpdateListener.get();
            if (listener != null) {
                long createdTimestamp = intent.getExtras().getLong(ALARM_TIMESTAMP);
                switch (intent.getAction()) {
                    case ACTION_ALARM_UPDATED:
                        listener.onWatchZoneUpdated(createdTimestamp);
                        break;
                    case ACTION_ALARM_DELETED:
                        listener.onWatchZoneDeleted(createdTimestamp);
                        break;
                    default:
                }
            } else {
                mContext.unregisterReceiver(this);
            }
        }
    };

    private class WatchZoneBuilder {
        long createdTimestamp = -1;
        long lastUpdatedTimestamp = -1;
        String label = "";
        double latitude = Double.MIN_VALUE;
        double longitude = Double.MIN_VALUE;
        int radius = -1;

        List<SweepingPositionStub> positionStubs;

        WatchZone build() {
            WatchZone result = null;
            if (createdTimestamp < 0 || lastUpdatedTimestamp < 0 || latitude == Double.MIN_VALUE ||
                    longitude == Double.MIN_VALUE || radius < 0) {
                String error = "createdTimestamp=" + createdTimestamp +
                        "lastUpdatedTimestamp=" + lastUpdatedTimestamp +
                        "latitude=" + latitude +
                        "longitude=" + longitude +
                        "radius=" + radius;
                Log.e(TAG, "Attempted to build an WatchZone but failed:\n" + error);
            } else {
                List<SweepingAddress> sweepingAddresses = new ArrayList<>();
                LimitDbHelper helper = new LimitDbHelper(mContext);
                if (positionStubs != null) {
                    for (SweepingPositionStub stub : positionStubs) {
                        if (stub.limitId == 666666666) {
                            // TODO REMOVE THIS
                        }
                        Limit limit = helper.getLimitForId(stub.limitId);
                        if (stub.limitId == 666666666) {
                            List<LimitSchedule> schedules = new ArrayList<>();
                            for (int i = 1; i < 5; i++) {
                                for (int j = 1; j < 8; j++) {
                                    schedules.add(new LimitSchedule(12, 15, j, i));
                                }
                            }
                            limit = new Limit(666, "Satan's Butthole", new int[]{0, 666666666},
                                    "Satan's Butthole", schedules);
                        }
                        LatLng latLng = new LatLng(stub.latitude, stub.longitude);
                        sweepingAddresses.add(new SweepingAddress(latLng, stub.address, limit));
                    }
                }

                LatLng center = new LatLng(latitude, longitude);
                result = new WatchZone(createdTimestamp, lastUpdatedTimestamp, label, center, radius,
                        sweepingAddresses);
            }

            return result;
        }
    }

    private class SweepingPositionStub {
        double longitude;
        double latitude;
        String address;
        int limitId;
    }
}
