package com.example.joseph.sweepersd.model.alarms;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.example.joseph.sweepersd.model.limits.LimitDbHelper;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 9/2/16.
 */
public class AlarmUpdateService extends IntentService implements
        AlarmFileHelper.AlarmUpdateListener {
    private static final String TAG = AlarmUpdateService.class.getSimpleName();
    public static final String ACTION_ALARM_PROGRESS =
            "com.example.joseph.sweepersd.ACTION_ALARM_PROGRESS";
    public static final String ACTION_ALARM_FINISHED =
            "com.example.joseph.sweepersd.ACTION_ALARM_FINISHED";
    public static final String ALARM_SUCCESS = "ALARM_SUCCESS";
    public static final String PARAM_ALARM_ID = "PARAM_ALARM_ID";
    public static final String PARAM_PROGRESS = "PARAM_PROGRESS";

    private long mId;

    private boolean mIsCancelled = false;
    private boolean mShouldRestart = false;
    private boolean mIsSaving = false;

    public AlarmUpdateService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Starting " + TAG);
        Long id = intent.getLongExtra(PARAM_ALARM_ID, 0);
        if (id > 0) {
            mId = id;

            AlarmFileHelper helper = new AlarmFileHelper(this, this);
            boolean update = true;
            while (update) {
                boolean cancelled = updateAlarm(helper);
                if (cancelled) {
                    Log.d(TAG, TAG + " cancelled.");
                    if (!shouldRestart()) {
                        update = false;
                    } else {
                        Log.d(TAG, TAG + " restarting");
                    }
                } else {
                    update = false;
                }
            }

            helper.setAlarmUpdateListener(null);

            publishFinished(true);
        }
    }

    @Override
    public void onAlarmUpdated(long createdTimestamp) {
        if (createdTimestamp == mId && !mIsSaving) {
            mIsCancelled = true;
            setShouldRestart(true);
        } else {
            mIsSaving = false;
        }
    }

    @Override
    public void onAlarmDeleted(long createdTimestamp) {
        if (createdTimestamp == mId) {
            mIsCancelled = true;
            setShouldRestart(false);
        }
    }

    private synchronized void setShouldRestart(boolean shouldRestart) {
        mShouldRestart = shouldRestart;
    }

    private synchronized boolean shouldRestart() {
        return mShouldRestart;
    }

    private boolean updateAlarm(AlarmFileHelper helper) {
        mIsSaving = false;
        Alarm alarmToUpdate = helper.loadAlarm(mId);
        if (alarmToUpdate != null) {
            LimitDbHelper limitHelper = new LimitDbHelper(this);

            if (mIsCancelled) {
                return true;
            }

            String address  = LocationUtils.getAddressForLatLnt(this, alarmToUpdate.getCenter());
            String[] addressSplit = address.split(",");
            String alarmAddress = "";
            if (addressSplit.length > 0) {
                alarmAddress = addressSplit[0];

                Alarm updatedAlarm = new Alarm(alarmToUpdate.getCreatedTimestamp(),
                        System.currentTimeMillis(), alarmAddress, alarmToUpdate.getCenter(),
                        alarmToUpdate.getRadius(), null);

                mIsSaving = true;
                helper.saveAlarm(updatedAlarm);
            }

            List<LatLng> latLngs = LocationUtils.getLatLngsInRadius(alarmToUpdate.getCenter(),
                    alarmToUpdate.getRadius());

            HashMap<String, SweepingAddress> sweepingAddresses = new HashMap<>();

            String centerAdd  = LocationUtils.getAddressForLatLnt(this, alarmToUpdate.getCenter());
            SweepingAddress centerSA = buildSweepingAddress(limitHelper,
                    alarmToUpdate.getCenter(), centerAdd);
            if (centerSA != null) {
                sweepingAddresses.put(centerAdd, centerSA);
            }
            for (int i = 0; i < latLngs.size(); i++) {
                if (mIsCancelled) {
                    return true;
                }
                int progress = (int) (((double)i / (double)latLngs.size()) * 100);
                publishProgress(progress);

                LatLng latLng = latLngs.get(i);

                String add = LocationUtils.getAddressForLatLnt(this, latLng);
                if (!sweepingAddresses.containsKey(add)) {
                    SweepingAddress sa = buildSweepingAddress(limitHelper, latLng, add);
                    if (sa != null) {
                        sweepingAddresses.put(add, sa);
                    }
                }
            }
            Log.d(TAG, "finished generating SweepingAddresses. Size " + sweepingAddresses.size());

            Alarm updatedAlarm = new Alarm(alarmToUpdate.getCreatedTimestamp(),
                    System.currentTimeMillis(), alarmAddress, alarmToUpdate.getCenter(),
                    alarmToUpdate.getRadius(), new ArrayList<>(sweepingAddresses.values()));

            mIsSaving = true;
            boolean saveSuccess = helper.saveAlarm(updatedAlarm);
            if (!saveSuccess) {
                Log.d(TAG, "Failed to save updated Alarm! " + alarmToUpdate.getCreatedTimestamp());
            }
        }
        return false;
    }

    private SweepingAddress buildSweepingAddress(LimitDbHelper limitHelper, LatLng latLng,
                                                 String address) {
        SweepingAddress result = null;

        Log.d(TAG, "buildSweepingAddress address: " + address);
        Limit limit = LocationUtils.findLimitForAddress(limitHelper, address);
        if (limit != null) {
            result = new SweepingAddress(latLng, address, limit);
        }

        return result;
    }

    private void publishProgress(int progress) {
        Log.d(TAG, "publishing progress: " + progress);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_PROGRESS, progress);

        sendBroadcast(bundle, ACTION_ALARM_PROGRESS);
    }

    private void publishFinished(boolean success) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ALARM_SUCCESS, success);

        sendBroadcast(bundle, ACTION_ALARM_FINISHED);
    }

    private void sendBroadcast(Bundle bundle, String action) {
        bundle.putLong(PARAM_ALARM_ID, mId);

        Intent intent = new Intent(action);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }
}
