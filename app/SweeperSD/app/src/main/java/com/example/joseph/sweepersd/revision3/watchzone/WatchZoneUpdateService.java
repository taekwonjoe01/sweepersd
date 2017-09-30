package com.example.joseph.sweepersd.revision3.watchzone;

import android.app.IntentService;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ServiceLifecycleDispatcher;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.example.joseph.sweepersd.revision3.AppDatabase;
import com.example.joseph.sweepersd.revision3.LocationUtils;
import com.example.joseph.sweepersd.revision3.limit.Limit;
import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class WatchZoneUpdateService extends IntentService implements LifecycleOwner {
    private static final String TAG = WatchZoneUpdateService.class.getSimpleName();
    public static final String ACTION_WATCH_ZONE_UPDATE_PROGRESS =
            "com.example.joseph.sweepersd.ACTION_WATCH_ZONE_UPDATE_PROGRESS";
    public static final String ACTION_WATCH_ZONE_UPDATE_COMPLETE =
            "com.example.joseph.sweepersd.ACTION_WATCH_ZONE_UPDATE_COMPLETE";
    public static final String ACTION_UPDATE_CANCEL =
            "com.example.joseph.sweepersd.ACTION_UPDATE_CANCEL";
    public static final String PARAM_WATCH_ZONE_SUCCESS = "PARAM_WATCH_ZONE_SUCCESS";
    public static final String PARAM_WATCH_ZONE_ID = "PARAM_WATCH_ZONE_ID";
    public static final String PARAM_INTENT_TRIGGER_TIME = "PARAM_INTENT_TRIGGER_TIME";
    public static final String PARAM_FULL_REFRESH = "PARAM_FULL_REFRESH";
    public static final String PARAM_PROGRESS = "PARAM_PROGRESS";

    private ServiceLifecycleDispatcher mDispatcher = new ServiceLifecycleDispatcher(this);

    private LiveData<WatchZone> mWatchZone;
    private LiveData<List<Limit>> mLimits;

    private long mId;
    private long mRescheduleUntilId = 0L;
    private long mRescheduleUntilFullSweepId = 0L;

    private boolean mIsCancelled = false;
    private boolean mShouldRestart = false;
    private boolean mShouldReschedule = false;

    public WatchZoneUpdateService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        mDispatcher.onServicePreSuperOnCreate();
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_CANCEL);
        registerReceiver(mCancelReceiver, filter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mDispatcher.onServicePreSuperOnBind();
        return super.onBind(intent);
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        mDispatcher.onServicePreSuperOnStart();
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy();
        super.onDestroy();
    }

    @Override
    public Lifecycle getLifecycle() {
        return mDispatcher.getLifecycle();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Starting " + TAG);
        mId = 0;
        mIsCancelled = false;
        setShouldRestart(false);
        setShouldReshedule(false);

        Long id = intent.getLongExtra(PARAM_WATCH_ZONE_ID, 0);
        Long intentTriggerTime = intent.getLongExtra(PARAM_INTENT_TRIGGER_TIME, 0);
        boolean fullRefresh = intent.getBooleanExtra(PARAM_FULL_REFRESH, false);

        if (id == 0) {
            return;
        }

        // Prune RescheduleUntil:
        Log.i(TAG, "Pruning the reschedule-until list.");
        if (mRescheduleUntilId != 0L) {
            Log.i(TAG, "Reschedule tasks until hitting specified id.");
            if (mRescheduleUntilFullSweepId != id) {
                WatchZone watchZone = AppDatabase.getInstance(
                        WatchZoneUpdateService.this).watchZoneDao().getWatchZone(mRescheduleUntilId);
                if (watchZone == null) {
                    Log.i(TAG, "Subject of reschedule-until does not exist. Clearing the subject.");
                    mRescheduleUntilId = 0L;
                } else {
                    Log.i(TAG, "Subject of reschedule-until still exists.");
                }
            } else {
                Log.i(TAG, "Swept through all tasks. Subject of reschedule-until was not " +
                        "found! Clearing the subject.");
                mRescheduleUntilId = 0L;
                mRescheduleUntilFullSweepId = 0L;
            }
        }

        if (mRescheduleUntilId == 0L || mRescheduleUntilId == id) {
            mId = id;
            if (mId == mRescheduleUntilId) {
                Log.i(TAG, "Found the subject of reschedule-until!");
            }

            WatchZoneRepository watchZoneRepository = WatchZoneRepository.getInstance(this);
            LimitRepository limitRepository = LimitRepository.getInstance(this);

            if (mWatchZone != null) {
                mWatchZone.removeObservers(this);
            }
            mWatchZone = watchZoneRepository.getWatchZoneLiveData(mId);

            final CountDownLatch latchForWatchZone = new CountDownLatch(1);
            Observer<WatchZone> watchZoneObserver = new Observer<WatchZone>() {
                @Override
                public void onChanged(@Nullable WatchZone watchZone) {
                    if (latchForWatchZone.getCount() > 0) {
                        latchForWatchZone.countDown();
                    } else {
                        setShouldRestart(false);
                        mIsCancelled = true;
                    }
                }
            };
            mWatchZone.observe(this, watchZoneObserver);

            if (mLimits != null) {
                mLimits.removeObservers(this);
            }
            mLimits = limitRepository.getLimits();

            final CountDownLatch latchForLimits = new CountDownLatch(1);
            Observer<List<Limit>> limitsObserver = new Observer<List<Limit>>() {
                @Override
                public void onChanged(@Nullable List<Limit> limits) {
                    if (latchForLimits.getCount() > 0) {
                        latchForLimits.countDown();
                    } else {
                        setShouldRestart(true);
                        mIsCancelled = true;
                    }
                }
            };
            // This observe is to be safe and restart everything if the limits database is updated.
            // This can be optimized to be done later in the process.
            mLimits.observe(this, limitsObserver);

            try {
                latchForWatchZone.await();
                latchForLimits.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WatchZone watchZone = mWatchZone.getValue();

            if (watchZone != null && intentTriggerTime > watchZone.getLastSweepingUpdated()) {
                boolean update = true;
                while (update) {
                    boolean cancelled = updateWatchZone(watchZone, fullRefresh);
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

                if (mRescheduleUntilId == mId) {
                    mRescheduleUntilId = 0L;
                }
                publishFinished(true);
            }
            mWatchZone.removeObserver(watchZoneObserver);
        } else {
            setShouldReshedule(true);
        }

        if (shouldReschedule()) {
            startService(intent);
        }
    }

    private synchronized void setShouldRestart(boolean shouldRestart) {
        mShouldRestart = shouldRestart;
    }

    private synchronized boolean shouldRestart() {
        return mShouldRestart;
    }

    private synchronized void setShouldReshedule(boolean shouldReshedule) {
        mShouldReschedule = shouldReshedule;
    }

    private synchronized boolean shouldReschedule() {
        return mShouldReschedule;
    }

    private boolean updateWatchZone(final WatchZone watchZoneToUpdate,
                                    boolean fullRefresh) {
        if (mId == 666) {
            //WatchZoneUtils.scheduleWatchZoneAlarm(this, watchZoneToUpdate);
            return false;
        }
        WatchZoneRepository repository = WatchZoneRepository.getInstance(this);

        List<WatchZonePoint> oldPoints = repository.getWatchZonePoints(watchZoneToUpdate);

        // Cache the old points in case update failures. This will be empty if we are doing a full
        // refresh or the WatchZonePoints have not been populated yet (generally a new WatchZone).
        final HashMap<LatLng, WatchZonePoint> oldPointCache = new HashMap<>();
        List<LatLng> latLngs = new ArrayList<>();
        if (!fullRefresh && oldPoints != null && !oldPoints.isEmpty()) {
            for (WatchZonePoint p : oldPoints) {
                LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
                oldPointCache.put(latLng, p);
                latLngs.add(latLng);
            }
        } else {
            fullRefresh = true;
            latLngs = LocationUtils.getLatLngsInRadius(new LatLng(watchZoneToUpdate.getCenterLatitude(),
                            watchZoneToUpdate.getCenterLongitude()),
                            watchZoneToUpdate.getRadius());
        }

        final boolean needsFullRefresh = fullRefresh;

        if (mIsCancelled) {
            return true;
        }

        final WatchZoneDao watchZoneDao = AppDatabase.getInstance(this).watchZoneDao();
        int deletedPoints = 0;
        if (fullRefresh) {
            deletedPoints = watchZoneDao.deleteWatchZonePoints(oldPoints);
        }

        final List<WatchZonePoint> successfulWatchZonePoints = new ArrayList<>();

        final CountDownLatch latch = new CountDownLatch(latLngs.size());
        final LimitRepository limitRepository = LimitRepository.getInstance(this);

        // Create threads to parallel process requests.
        int numThreads = Runtime.getRuntime().availableProcessors() * 2;
        List<HandlerThread> threads = new ArrayList<>();
        List<Handler> handlers = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            HandlerThread thread = new HandlerThread("addressLookupThread" + (i+1));
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            threads.add(thread);
            handlers.add(handler);
        }

        int threadIndex = 0;
        final int size = latLngs.size();
        Log.d(TAG, "Starting " + numThreads + " threads...");
        for (final LatLng latLng : latLngs) {
            handlers.get(threadIndex).post(new Runnable() {
                @Override
                public void run() {
                    if (!mIsCancelled) {
                        String address = LocationUtils.getAddressForLatLnt(
                                WatchZoneUpdateService.this, latLng);
                        WatchZonePoint watchZonePoint = buildWatchZonePoint(oldPointCache,
                                limitRepository, latLng, address, needsFullRefresh);
                        watchZonePoint.setWatchZoneId(watchZoneToUpdate.getUid());
                        boolean success = false;
                        if (needsFullRefresh) {
                            success = watchZoneDao.insertWatchZonePoint(watchZonePoint) == 1;
                        } else {
                            success = watchZoneDao.updateWatchZone(watchZoneToUpdate) == 1;
                        }
                        if (success) {
                            synchronized (successfulWatchZonePoints) {
                                successfulWatchZonePoints.add(watchZonePoint);
                                int numDone = size - (int) latch.getCount();
                                int progress = (int) (((double) numDone / (double) size) * 100);
                                publishProgress(progress);
                            }
                        }
                        latch.countDown();
                    }
                }
            });
            threadIndex = (threadIndex + 1) % numThreads;
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (HandlerThread thread : threads) {
            thread.quit();
        }

        if (mIsCancelled) {
            // Delete all the added WatchZonePoints from the database.
            watchZoneDao.deleteWatchZonePoints(successfulWatchZonePoints);
            return true;
        }

        watchZoneToUpdate.setLastSweepingUpdated(System.currentTimeMillis());

        int updateWatchZone = watchZoneDao.updateWatchZone(watchZoneToUpdate);

        boolean watchZoneUpdated = updateWatchZone == 1;
        boolean pointsDeleted = true;
        boolean pointsInserted = true;
        boolean pointsUpdated = true;
        if (needsFullRefresh) {
            pointsDeleted = deletedPoints == oldPoints.size();
            pointsInserted = latLngs.size() == successfulWatchZonePoints.size();
        } else {
            pointsUpdated &= latLngs.size() == successfulWatchZonePoints.size();
        }

        boolean saveSuccess = watchZoneUpdated && pointsDeleted && pointsInserted && pointsUpdated;

        if (!watchZoneUpdated) {
            Log.e(TAG, "Failed to save the WatchZone " + watchZoneToUpdate.getLabel());
        }
        if (!pointsDeleted) {
            Log.e(TAG, "Failed to delete all old WatchZonePoints for the WatchZone "
                    + watchZoneToUpdate.getLabel() + ". Points expected to be deleted: "
                    + oldPoints.size() + ". Points actually deleted: " + deletedPoints);
        }
        if (!pointsInserted) {
            Log.e(TAG, "Failed to insert all old WatchZonePoints for the WatchZone "
                    + watchZoneToUpdate.getLabel() + ". Points expected to be inserted: "
                    + successfulWatchZonePoints.size() + ". Points actually inserted: " + successfulWatchZonePoints);
        }
        if (!pointsUpdated) {
            Log.e(TAG, "Failed to update all old WatchZonePoints for the WatchZone "
                    + watchZoneToUpdate.getLabel() + ". Points expected to be updated: "
                    + successfulWatchZonePoints.size() + ". Points actually updated: " + successfulWatchZonePoints);
        }

        if (!saveSuccess) {
            Log.d(TAG, "Failed to save updated WatchZone! " + watchZoneToUpdate.getLabel());
        } else {
            //WatchZoneUtils.scheduleWatchZoneAlarm(this, watchZoneToUpdate);
        }
        return false;
    }

    private interface LatLngLookupCallback {
        void onTaskFinished(String address);
    }

    private WatchZonePoint buildWatchZonePoint(HashMap<LatLng, WatchZonePoint> oldCachedPoints,
                                               LimitRepository limitRepository,
                                               LatLng latLng,
                                               String address, boolean needsFullRefresh) {
        boolean isValid = address != null;

        WatchZonePoint point = null;
        if (needsFullRefresh) {
            point = new WatchZonePoint();
            // Address might be empty string, that's fine.
            point.setAddress(address);
            point.setValid(isValid);
            if (isValid && !TextUtils.isEmpty(address)) {
                // Attempt to find the limit for the address.
                Limit limit = LocationUtils.findLimitForAddress(limitRepository, address);
                point.setLimitId(limit != null ? limit.getUid() : 0L);
            }
        } else {
            // At this point, it should be guaranteed there is no null.
            point = oldCachedPoints.get(latLng);
            // Always true, because if it's really not, we are falling back to old data.
            point.setValid(true);
            if (isValid) {
                point.setAddress(address);
                if (!TextUtils.isEmpty(address)) {
                    Limit limit = LocationUtils.findLimitForAddress(limitRepository, address);
                    point.setLimitId(limit != null ? limit.getUid() : 0L);
                } else {
                    point.setLimitId(0L);
                }
            }
        }
        point.setLatitude(latLng.latitude);
        point.setLongitude(latLng.longitude);
        return point;
    }

    private void publishProgress(int progress) {
        Log.d(TAG, "publishing progress: " + progress);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_PROGRESS, progress);

        sendBroadcast(bundle, ACTION_WATCH_ZONE_UPDATE_PROGRESS);
    }

    private void publishFinished(boolean success) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(PARAM_WATCH_ZONE_SUCCESS, success);

        sendBroadcast(bundle, ACTION_WATCH_ZONE_UPDATE_COMPLETE);
    }

    private void sendBroadcast(Bundle bundle, String action) {
        bundle.putLong(PARAM_WATCH_ZONE_ID, mId);

        Intent intent = new Intent(action);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    /**
     * An explicit way to reschedule any pending updating services to get a specific uid updating
     * immediately.
     * @param context
     * @param watchZoneUid
     */
    public static void requestAttention(Context context, long watchZoneUid) {
        Bundle bundle = new Bundle();
        bundle.putLong(PARAM_WATCH_ZONE_ID, watchZoneUid);

        Intent intent = new Intent(ACTION_UPDATE_CANCEL);
        intent.putExtras(bundle);
        context.sendBroadcast(intent);
    }

    private final BroadcastReceiver mCancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(WatchZoneUpdateService.PARAM_WATCH_ZONE_ID, 0);

            mRescheduleUntilId = id;
            if (mId != id) {
                mIsCancelled = true;
                setShouldRestart(false);
                setShouldReshedule(true);
                mRescheduleUntilFullSweepId = mId;
            }
        }
    };
}
