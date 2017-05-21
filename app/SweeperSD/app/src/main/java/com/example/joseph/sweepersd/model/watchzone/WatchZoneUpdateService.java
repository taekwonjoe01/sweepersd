package com.example.joseph.sweepersd.model.watchzone;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.example.joseph.sweepersd.model.limits.LimitDbHelper;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by joseph on 9/2/16.
 */
public class WatchZoneUpdateService extends IntentService implements
        WatchZoneFileHelper.WatchZoneUpdateListener {
    private static final String TAG = WatchZoneUpdateService.class.getSimpleName();
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

    public WatchZoneUpdateService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Starting " + TAG);
        Long id = intent.getLongExtra(PARAM_ALARM_ID, 0);
        if (id > 0) {
            mId = id;

            WatchZoneFileHelper helper = new WatchZoneFileHelper(this, this);
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

            helper.setWatchZoneUpdateListener(null);

            publishFinished(true);
        }
    }

    @Override
    public void onWatchZoneUpdated(long createdTimestamp) {
        if (createdTimestamp == mId && !mIsSaving) {
            mIsCancelled = true;
            setShouldRestart(true);
        } else {
            mIsSaving = false;
        }
    }

    @Override
    public void onWatchZoneDeleted(long createdTimestamp) {
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

    private boolean updateAlarm(WatchZoneFileHelper helper) {
        mIsSaving = false;
        WatchZone watchZoneToUpdate = helper.loadWatchZone(mId);
        if (mId == 666) {
            WatchZoneUtils.scheduleWatchZoneAlarm(this, watchZoneToUpdate);
            return false;
        }
        if (watchZoneToUpdate != null ) {
            List<SweepingAddress> oldSweeingAddresses = watchZoneToUpdate.getSweepingAddresses();
            final HashMap<LatLng, SweepingAddress> oldSweepingAddressMap = new HashMap<>();
            List<LatLng> latLngs = new ArrayList<>();
            if (oldSweeingAddresses != null && !oldSweeingAddresses.isEmpty()) {
                for (SweepingAddress a : watchZoneToUpdate.getSweepingAddresses()) {
                    oldSweepingAddressMap.put(a.getLatLng(), a);
                    latLngs.add(a.getLatLng());
                }
            } else {
                latLngs = LocationUtils.getLatLngsInRadius(watchZoneToUpdate.getCenter(),
                        watchZoneToUpdate.getRadius());
            }

            final LimitDbHelper limitHelper = new LimitDbHelper(this);

            if (mIsCancelled) {
                return true;
            }

            String address  = LocationUtils.getAddressForLatLnt(this, watchZoneToUpdate.getCenter());
            if (address == null) {
                return false;
            }
            final String[] addressSplit = address.split(",");
            String alarmAddress = "";
            if (addressSplit.length > 0) {
                alarmAddress = addressSplit[0];

                WatchZone updatedWatchZone = new WatchZone(watchZoneToUpdate.getCreatedTimestamp(),
                        System.currentTimeMillis(), watchZoneToUpdate.getLabel(),
                        watchZoneToUpdate.getCenter(),
                        watchZoneToUpdate.getRadius(), null);

                mIsSaving = true;
                helper.saveWatchZone(updatedWatchZone);
            }

            final List<SweepingAddress> sweepingAddresses = new ArrayList<>();

            final CountDownLatch latch = new CountDownLatch(latLngs.size());

            Log.e("Joey", "Starting " + latLngs.size() + " threads...");
            Log.e("Joey", "Number of available cores: " + Runtime.getRuntime().availableProcessors());
            for (int i = 0; i < latLngs.size(); i++) {
                final LatLng latLng = latLngs.get(i);
                final int size = latLngs.size();
                new LatLngLookupTask(latLng, new LatLngLookupCallback() {
                    @Override
                    public void onTaskFinished(String address) {
                        SweepingAddress sweepingAddress = buildSweepingAddress(oldSweepingAddressMap, limitHelper,
                                latLng, address);
                        synchronized (this) {
                            sweepingAddresses.add(sweepingAddress);
                            int numDone = size - (int) latch.getCount();
                            int progress = (int) (((double)numDone / (double)size) * 100);
                            publishProgress(progress);
                        }
                        latch.countDown();
                    }
                }).start();
                /*if (mIsCancelled) {
                    return true;
                }
                int progress = (int) (((double)i / (double)latLngs.size()) * 100);
                publishProgress(progress);

                LatLng latLng = latLngs.get(i);

                String add = LocationUtils.getAddressForLatLnt(this, latLng);
                sweepingAddresses.add(buildSweepingAddress(oldSweepingAddressMap,
                        limitHelper, latLng, add));*/
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "finished generating SweepingAddresses. Size " + sweepingAddresses.size());

            WatchZone updatedWatchZone = new WatchZone(watchZoneToUpdate.getCreatedTimestamp(),
                    System.currentTimeMillis(), watchZoneToUpdate.getLabel(),
                    watchZoneToUpdate.getCenter(),
                    watchZoneToUpdate.getRadius(), new ArrayList<>(sweepingAddresses));

            mIsSaving = true;
            boolean saveSuccess = helper.saveWatchZone(updatedWatchZone);
            if (!saveSuccess) {
                Log.d(TAG, "Failed to save updated WatchZone! " + watchZoneToUpdate.getCreatedTimestamp());
            } else {
                WatchZoneUtils.scheduleWatchZoneAlarm(this, updatedWatchZone);
            }
        }
        return false;
    }

    private interface LatLngLookupCallback {
        void onTaskFinished(String address);
    }

    private class LatLngLookupTask extends Thread {
        private final LatLng mLatLng;
        private final LatLngLookupCallback mCallback;
        LatLngLookupTask(LatLng latLng, LatLngLookupCallback callback) {
            mLatLng = latLng;
            mCallback = callback;
        }
        @Override
        public void run() {
            super.run();
            String add = LocationUtils.getAddressForLatLnt(WatchZoneUpdateService.this, mLatLng);
            mCallback.onTaskFinished(add);
        }
    }

    private SweepingAddress buildSweepingAddress(HashMap<LatLng, SweepingAddress> oldAddresses,
                                                 LimitDbHelper limitHelper, LatLng latLng,
                                                 String address) {
        String finalAddress = null;
        Limit finalLimit = null;
        if (!TextUtils.isEmpty(address)) {
            finalAddress = address;
            Limit limit = LocationUtils.findLimitForAddress(limitHelper, address);
            if (limit != null) {
                finalLimit = limit;
            } else {
                SweepingAddress old = oldAddresses.get(latLng);
                if (old != null) {
                    finalLimit = old.getLimit();
                }
            }
        } else {
            SweepingAddress old = oldAddresses.get(latLng);
            if (old != null) {
                finalAddress = old.getAddress();
                finalLimit = old.getLimit();
            }
        }
        return new SweepingAddress(latLng, finalAddress, finalLimit);
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
