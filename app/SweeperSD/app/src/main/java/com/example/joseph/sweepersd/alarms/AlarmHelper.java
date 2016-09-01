package com.example.joseph.sweepersd.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.example.joseph.sweepersd.SweepingAddress;
import com.example.joseph.sweepersd.limits.Limit;
import com.example.joseph.sweepersd.limits.LimitDbHelper;
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
 * Helper class for loading and saving alarms to disk.
 */
public class AlarmHelper {
    private static final String TAG = AlarmHelper.class.getSimpleName();

    private static final String FILE_SWEEPING_LOCATIONS = "sweeping_locations.txt";
    private static final String FILE_ALARM_DETAILS = "alarm_details.txt";

    private static final String SEPARATOR_SWEEPING_LOCATIONS = "::";

    public static final String ACTION_ALARM_UPDATED =
            "com.example.joseph.sweepersd.ACTION_ALARM_UPDATED";
    public static final String ACTION_ALARM_DELETED =
            "com.example.joseph.sweepersd.ACTION_ALARM_DELETED";

    public static final String ALARM_TIMESTAMP = "ALARM_TIMESTAMP";

    private static Semaphore mSemaphore = new Semaphore(1);

    private final Context mContext;
    private WeakReference<AlarmUpdateListener> mAlarmUpdateListener;

    public interface AlarmUpdateListener {
        void onAlarmUpdated(long createdTimestamp);
        void onAlarmDeleted(long createdTimestamp);
    }

    public AlarmHelper(Context context, AlarmUpdateListener listener) {
        mContext = context;
        mAlarmUpdateListener = new WeakReference<>(listener);

        if (listener != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_ALARM_UPDATED);
            filter.addAction(ACTION_ALARM_DELETED);
            mContext.registerReceiver(mAlarmUpdateReceiver, filter);
        }
    }

    public void setAlarmUpdateListener(AlarmUpdateListener listener) {
        if (listener == null) {
            mContext.unregisterReceiver(mAlarmUpdateReceiver);
        }
        mAlarmUpdateListener = new WeakReference<>(listener);
    }

    public List<Alarm> loadAlarms() {
        List<Alarm> results = new ArrayList<>();

        File alarmsDir = new File(getAlarmDirPath());
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
                Alarm alarm = loadAlarm(Long.parseLong(alarmDir.getName()));
                if (alarm != null) {
                    results.add(alarm);
                } else {
                    Log.e(TAG, "Alarm was corrupt. " + alarmDir.getName());
                }
            }
        }
        return results;
    }

    public Alarm loadAlarm(long createdTimestamp) {
        AlarmBuilder builder = new AlarmBuilder();
        try {
            mSemaphore.acquire();
            try {
                File dir = new File(getAlarmDirPath() + createdTimestamp);
                if (dir.exists()) {
                    builder.createdTimestamp = createdTimestamp;

                    File alarmDetailsFile = new File(dir.getAbsolutePath() + "/" +
                            FILE_ALARM_DETAILS);
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

                                    stub.address = parsings[1];
                                    stub.limitId = Integer.parseInt(parsings[2]);

                                    builder.positionStubs.add(stub);
                                }
                            }

                            SLis.close();
                        }
                    }
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            } finally {
                mSemaphore.release();
            }
        } catch (InterruptedException e) {

        }

        return builder.build();
    }

    public void saveAlarm(Alarm alarm) {
        Long tsLong = alarm.getCreatedTimestamp();
        String ts = tsLong.toString();
        try {
            mSemaphore.acquire();
            File alarmDir = new File(getAlarmDirPath() + ts);
            alarmDir.mkdirs();
            try {
                File alarmDetailsFile = new File(alarmDir.getAbsolutePath() + "/" +
                        FILE_ALARM_DETAILS);
                File sweepingLocationsFile = new File(alarmDir.getAbsolutePath() + "/" +
                        FILE_SWEEPING_LOCATIONS);
                alarmDetailsFile.createNewFile();
                sweepingLocationsFile.createNewFile();

                FileWriter ADwriter = new FileWriter(alarmDetailsFile);

                if (ADwriter != null) {
                    String LatLng = alarm.getCenter().latitude + "," + alarm.getCenter().longitude;
                    ADwriter.write(LatLng + "\n");

                    ADwriter.write(alarm.getRadius() + "\n");

                    ADwriter.write(alarm.getLastUpdatedTimestamp() + "\n");

                    ADwriter.close();
                }

                if (alarm.getSweepingPositions() != null) {
                    FileWriter SLwriter = new FileWriter(sweepingLocationsFile);
                    if (SLwriter != null) {
                        for (SweepingAddress pos : alarm.getSweepingPositions()) {
                            String LatLng = pos.getLatLng().latitude + "," + pos.getLatLng().longitude;
                            String address = pos.getAddress();
                            String limitId = pos.getLimit().getId() + "";

                            SLwriter.write(LatLng + SEPARATOR_SWEEPING_LOCATIONS +
                                    address + SEPARATOR_SWEEPING_LOCATIONS +
                                    limitId + "\n");
                        }

                        SLwriter.close();
                    }
                }
                sendAlarmUpdatedBroadcast(tsLong);
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            } finally {
                mSemaphore.release();
            }
        } catch (InterruptedException e) {

        }

    }

    public void deleteAlarm(Alarm alarm) {
        Long tsLong = alarm.getCreatedTimestamp();
        String ts = tsLong.toString();
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
            }
            mSemaphore.release();
            sendAlarmDeletedBroadcast(tsLong);
        } catch (InterruptedException e) {

        }
    }

    private String getAlarmDirPath() {
         return mContext + "/alarms/";
    }

    private void sendAlarmUpdatedBroadcast(long createdTimestamp) {
        Bundle bundle = new Bundle();
        bundle.putLong(ALARM_TIMESTAMP, createdTimestamp);

        sendBroadcast(bundle, ACTION_ALARM_UPDATED);
    }

    private void sendAlarmDeletedBroadcast(long createdTimestamp) {
        Bundle bundle = new Bundle();
        bundle.putLong(ALARM_TIMESTAMP, createdTimestamp);

        sendBroadcast(bundle, ACTION_ALARM_DELETED);
    }

    private void sendBroadcast(Bundle bundle, String action) {
        Intent intent = new Intent(action);
        intent.putExtras(bundle);
        mContext.sendBroadcast(intent);
    }

    private final BroadcastReceiver mAlarmUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AlarmUpdateListener listener = mAlarmUpdateListener.get();
            if (listener != null) {
                long createdTimestamp = intent.getExtras().getLong(ALARM_TIMESTAMP);
                switch (intent.getAction()) {
                    case ACTION_ALARM_UPDATED:
                        listener.onAlarmUpdated(createdTimestamp);
                        break;
                    case ACTION_ALARM_DELETED:
                        listener.onAlarmDeleted(createdTimestamp);
                        break;
                    default:
                }
            } else {
                mContext.unregisterReceiver(this);
            }
        }
    };

    private class AlarmBuilder {
        long createdTimestamp = -1;
        long lastUpdatedTimestamp = -1;
        double latitude = -1;
        double longitude = -1;
        int radius = -1;

        List<SweepingPositionStub> positionStubs;

        Alarm build() {
            Alarm result = null;
            if (createdTimestamp < 0 || lastUpdatedTimestamp < 0 || latitude < 0 ||
                    longitude < 0 || radius < 0 || positionStubs == null ||
                    positionStubs.isEmpty()) {
                String error = "createdTimestamp=" + createdTimestamp +
                        "lastUpdatedTimestamp=" + lastUpdatedTimestamp +
                        "latitude=" + latitude +
                        "longitude=" + longitude +
                        "radius=" + radius +
                        "positionStubs=" + (positionStubs == null ? 0 : positionStubs.size());
                Log.e(TAG, "Attempted to build an Alarm but failed:\n" + error);
            } else {
                List<SweepingAddress> sweepingAddresses = new ArrayList<>();
                LimitDbHelper helper = new LimitDbHelper(mContext);
                for (SweepingPositionStub stub : positionStubs) {
                    Limit limit = helper.getLimitForId(stub.limitId);
                    if (limit != null) {
                        LatLng latLng = new LatLng(stub.latitude, stub.longitude);
                        sweepingAddresses.add(new SweepingAddress(latLng, stub.address, limit));
                    }
                }

                LatLng center = new LatLng(latitude, longitude);
                result = new Alarm(createdTimestamp, lastUpdatedTimestamp, center, radius,
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
