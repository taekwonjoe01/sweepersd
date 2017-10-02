package com.example.joseph.sweepersd.revision3.watchzone;

import android.os.Handler;

import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class WatchZoneUpdater {
    private final List<Handler> mHandlers;
    private final Listener mListener;
    private final List<WatchZone> mWatchZonesToUpdate;
    private final Map<WatchZone, ProgressListener> mProgressListenerMap;
    private final Map<WatchZone, UpdateProgress> mUpdateProgressMap;
    private final WatchZoneRepository mWatchZoneRepository;
    private final LimitRepository mLimitRepository;
    private final AddressProvider mAddressProvider;

    private AtomicBoolean mIsCancelled;

    public enum Result {
        CANCELLED,
        CORRUPT,
        NETWORK_ERROR,
        SUCCESS
    }

    interface ProgressListener {
        void onProgress(UpdateProgress progress);
    }

    public interface Listener {
        void onUpdateComplete();
    }

    /**
     * Interface that provides the Address for a LatLng. Implementers should generally provide this
     * through the Android Geocoder.
     */
    public interface AddressProvider {
        /**
         * @param latLng
         * @return Expected null if the lookup fails, empty String if there is no address, or address.
         */
        String getAddressForLatLng(LatLng latLng);
    }

    public WatchZoneUpdater(Listener listener, List<WatchZone> watchZonesToUpdate,
                            List<Handler> threadedHandlers,
                            WatchZoneRepository watchZoneRepository,
                            LimitRepository limitRepository, AddressProvider addressProvider) {
        mListener = listener;
        mWatchZonesToUpdate = watchZonesToUpdate;
        mHandlers = threadedHandlers;
        mWatchZoneRepository = watchZoneRepository;
        mLimitRepository = limitRepository;
        mAddressProvider = addressProvider;

        mIsCancelled.set(false);

        mProgressListenerMap = new HashMap<>();
        mUpdateProgressMap = new HashMap<>();
    }

    public void cancel() {
        mIsCancelled.set(true);
    }

    public Map<WatchZone, UpdateProgress> execute() {
        return update();
    }

    void registerProgressUpdates(WatchZone watchZone, ProgressListener progressListener) {
        mProgressListenerMap.put(watchZone, progressListener);
    }

    void unregisterProgressUpdates(WatchZone watchZone) {
        mProgressListenerMap.remove(watchZone);
    }

    private Map<WatchZone, UpdateProgress> update() {
        if (mIsCancelled.get()) {
            for (WatchZone watchZone : mWatchZonesToUpdate) {
                publishProgress(watchZone, new UpdateProgress(0,
                        UpdateProgress.Status.CANCELLED));
            }
            return mUpdateProgressMap;
        }

        final List<Runnable> runnables = new ArrayList<>();
        final List<CountDownLatch> countDownLatches = new ArrayList<>();

        for (final WatchZone watchZone : mWatchZonesToUpdate) {
            final List<WatchZonePoint> watchZonePoints = mWatchZoneRepository.getWatchZonePoints(watchZone);

            if (watchZonePoints == null || watchZonePoints.isEmpty()) {
                publishProgress(watchZone, new UpdateProgress(0,
                        UpdateProgress.Status.CORRUPT));
            } else {
                final int size = watchZonePoints.size();
                final CountDownLatch latch = new CountDownLatch(size);
                for (final WatchZonePoint p : watchZonePoints) {
                    final WatchZonePointUpdater updater = new WatchZonePointUpdater(p,
                            mWatchZoneRepository, mLimitRepository, mAddressProvider);
                    runnables.add(new Runnable() {
                        @Override
                        public void run() {
                            if (!mIsCancelled.get()) {
                                updater.run();

                                synchronized (watchZonePoints) {
                                    int numDone = size - (int) latch.getCount();
                                    int progress = (int) (((double) numDone / (double) size) * 100);
                                    if (latch.getCount() > 0) {
                                        publishProgress(watchZone, new UpdateProgress(progress,
                                                UpdateProgress.Status.UPDATING));
                                    } else {
                                        publishProgress(watchZone, new UpdateProgress(progress,
                                                UpdateProgress.Status.COMPLETE));
                                    }
                                }

                                latch.countDown();
                            }
                        }
                    });
                }
                countDownLatches.add(latch);
            }
        }

        if (mIsCancelled.get()) {
            for (WatchZone watchZone : mWatchZonesToUpdate) {
                publishProgress(watchZone, new UpdateProgress(0,
                        UpdateProgress.Status.CANCELLED));
            }
            return mUpdateProgressMap;
        }

        for (WatchZone watchZone : mWatchZonesToUpdate) {
            publishProgress(watchZone, new UpdateProgress(0,
                    UpdateProgress.Status.UPDATING));
        }

        int handlerIndex = 0;
        int numHandlers = mHandlers.size();

        for (Runnable r : runnables) {
            Handler handler = mHandlers.get(0);
            handler.post(r);
            handlerIndex = (handlerIndex + 1) % numHandlers;
        }

        try {
            for (CountDownLatch latch : countDownLatches) {
                latch.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (mIsCancelled.get()) {
            for (WatchZone watchZone : mWatchZonesToUpdate) {
                UpdateProgress progress = mUpdateProgressMap.get(watchZone);
                if (UpdateProgress.Status.UPDATING == progress.getStatus()) {
                    publishProgress(watchZone, new UpdateProgress(progress.getProgress(),
                            UpdateProgress.Status.CANCELLED));
                }
            }
            return mUpdateProgressMap;
        }
        for (WatchZone watchZone : mWatchZonesToUpdate) {
            UpdateProgress progress = mUpdateProgressMap.get(watchZone);
            if (UpdateProgress.Status.UPDATING == progress.getStatus()) {
                publishProgress(watchZone, new UpdateProgress(progress.getProgress(),
                        UpdateProgress.Status.COMPLETE));
            }
        }

        //watchZone.setLastSweepingUpdated(System.currentTimeMillis());
        //mWatchZoneRepository.updateWatchZone(watchZone);

        return mUpdateProgressMap;
    }

    private void publishProgress(WatchZone watchZone, UpdateProgress progress) {
        mUpdateProgressMap.put(watchZone, progress);
        ProgressListener listener = mProgressListenerMap.get(watchZone);
        if (listener != null) {
            listener.onProgress(progress);
        }
    }

    public static class UpdateProgress {
        private final int mProgress;
        private final Status mStatus;

        public enum Status {
            CORRUPT,
            UPDATING,
            CANCELLED,
            COMPLETE
        }

        public UpdateProgress(int progress, Status status) {
            mProgress = progress;
            mStatus = status;
        }

        public int getProgress() {
            return mProgress;
        }

        public Status getStatus() {
            return mStatus;
        }
    }
}
