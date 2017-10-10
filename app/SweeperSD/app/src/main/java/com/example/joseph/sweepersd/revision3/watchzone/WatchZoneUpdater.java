package com.example.joseph.sweepersd.revision3.watchzone;

import android.os.Handler;

import com.example.joseph.sweepersd.revision3.limit.Limit;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

class WatchZoneUpdater {
    private final List<Handler> mHandlers;
    private final WatchZoneModel mModel;
    private final WatchZonePointSaveDelegate mSaveDelegate;
    private final ProgressListener mProgressListener;
    private final List<Limit> mLimits;
    private final AddressProvider mAddressProvider;

    private AtomicBoolean mIsCancelled;
    private UpdateProgress mProgress;

    interface ProgressListener {
        void onProgress(UpdateProgress progress);
    }

    /**
     * Interface that provides the Address for a LatLng. Implementers should generally provide this
     * through the Android Geocoder.
     */
    interface AddressProvider {
        /**
         * @param latLng
         * @return Expected null if the lookup fails, empty String if there is no address, or address.
         */
        String getAddressForLatLng(LatLng latLng);
    }

    interface WatchZonePointSaveDelegate {
        void saveWatchZonePoint(WatchZonePoint p);
    }

    public WatchZoneUpdater(WatchZoneModel watchZoneModel, ProgressListener listener,
                            List<Handler> threadedHandlers,
                            WatchZonePointSaveDelegate saveDelegate,
                            List<Limit> limits, AddressProvider addressProvider) {
        mModel = watchZoneModel;
        mProgressListener = listener;
        mHandlers = threadedHandlers;
        mSaveDelegate = saveDelegate;
        mLimits = limits;
        mAddressProvider = addressProvider;

        mIsCancelled = new AtomicBoolean(false);
    }

    public void cancel() {
        mIsCancelled.set(true);
    }

    public UpdateProgress execute() {
        return update();
    }

    private UpdateProgress update() {
        if (mIsCancelled.get()) {
            mProgress = new UpdateProgress(0, UpdateProgress.Status.CANCELLED);
            publishProgress(mProgress);
            return mProgress;
        }
        Map<String, List<Limit>> limitHash = new HashMap<>();
        for (Limit l : mLimits) {
            List<Limit> list = limitHash.get(l.getStreet());
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(l);
            limitHash.put(l.getStreet(), list);
        }

        final List<WatchZonePoint> watchZonePoints = mModel.getWatchZonePointsModel().getWatchZonePointsList();

        mProgress = new UpdateProgress(0, UpdateProgress.Status.UPDATING);
        publishProgress(mProgress);

        final int size = watchZonePoints.size();
        final CountDownLatch latch = new CountDownLatch(size);

        int handlerIndex = 0;
        int numHandlers = mHandlers.size();
        for (final WatchZonePoint p : watchZonePoints) {
            Handler handler = mHandlers.get(0);

            final WatchZonePointUpdater updater = new WatchZonePointUpdater(p,
                    mSaveDelegate, limitHash, mAddressProvider);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!mIsCancelled.get()) {
                        updater.run();

                        if (!mIsCancelled.get()) {
                            synchronized (watchZonePoints) {
                                int numDone = size - (int) latch.getCount();
                                int progress = (int) (((double) numDone / (double) size) * 100);
                                if (latch.getCount() > 0) {
                                    mProgress = new UpdateProgress(progress,
                                            UpdateProgress.Status.UPDATING);
                                } else {
                                    mProgress = new UpdateProgress(progress,
                                            UpdateProgress.Status.COMPLETE);
                                }
                                publishProgress(mProgress);
                            }
                        }
                    }

                    latch.countDown();
                }
            });
            handlerIndex = (handlerIndex + 1) % numHandlers;
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (mIsCancelled.get()) {
            if (UpdateProgress.Status.UPDATING == mProgress.getStatus()) {
                mProgress = new UpdateProgress(mProgress.getProgress(), UpdateProgress.Status.CANCELLED);
                publishProgress(mProgress);
            }
            return mProgress;
        }

        mProgress = new UpdateProgress(mProgress.getProgress(), UpdateProgress.Status.COMPLETE);

        return mProgress;
    }

    private void publishProgress(UpdateProgress progress) {
        if (mProgressListener != null) {
            mProgressListener.onProgress(progress);
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
