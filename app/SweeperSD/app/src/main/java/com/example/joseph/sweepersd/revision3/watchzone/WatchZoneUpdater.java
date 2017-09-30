package com.example.joseph.sweepersd.revision3.watchzone;

import android.os.Handler;
import android.text.TextUtils;

import com.example.joseph.sweepersd.revision3.LocationUtils;
import com.example.joseph.sweepersd.revision3.limit.Limit;
import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class WatchZoneUpdater {
    private static final long SEVEN_DAYS = 1000 * 60 * 60 * 24 * 7;

    private final List<Handler> mHandlers;
    private final Listener mListener;
    private final long mWatchZoneUid;
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

    public interface Listener {
        void onUpdateComplete(Result result);
        void onProgress(int progress);
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

    public WatchZoneUpdater(Listener listener, long watchZoneUid,
                            List<Handler> threadedHandlers,
                            WatchZoneRepository watchZoneRepository,
                            LimitRepository limitRepository, AddressProvider addressProvider) {
        mListener = listener;
        mWatchZoneUid = watchZoneUid;
        mHandlers = threadedHandlers;
        mWatchZoneRepository = watchZoneRepository;
        mLimitRepository = limitRepository;
        mAddressProvider = addressProvider;

        mIsCancelled.set(false);
    }

    public void cancel() {
        mIsCancelled.set(true);
    }

    public void execute() {
        Result result = update();

        if (mListener != null) {
            mListener.onUpdateComplete(result);
        }
    }

    private Result update() {
        if (mIsCancelled.get()) {
            return Result.CANCELLED;
        }

        if (mWatchZoneUid < 1) {
            return Result.CORRUPT;
        }

        WatchZone watchZone = mWatchZoneRepository.getWatchZone(mWatchZoneUid);

        if (watchZone == null) {
            return Result.CORRUPT;
        }

        List<WatchZonePoint> watchZonePoints = mWatchZoneRepository.getWatchZonePoints(watchZone);

        if (watchZonePoints == null || watchZonePoints.isEmpty()) {
            return Result.CORRUPT;
        }

        if (mIsCancelled.get()) {
            return Result.CANCELLED;
        }

        final List<WatchZonePoint> watchZonePointsToUpdate = new ArrayList<>();
        long currentTimestamp = System.currentTimeMillis();

        for (WatchZonePoint p : watchZonePoints) {
            long timeSinceUpdate = currentTimestamp - p.getWatchZoneUpdatedTimestampMs();
            if (timeSinceUpdate > SEVEN_DAYS) {
                watchZonePointsToUpdate.add(p);
            }
        }

        int handlerIndex = 0;
        int numHandlers = mHandlers.size();

        final int size = watchZonePoints.size();
        final CountDownLatch latch = new CountDownLatch(size);

        for (final WatchZonePoint p : watchZonePoints) {
            Handler handler = mHandlers.get(0);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!mIsCancelled.get()) {
                        LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
                        String address = mAddressProvider.getAddressForLatLng(latLng);

                        if (address != null) {
                            p.setAddress(address);
                            if (!TextUtils.isEmpty(address)) {
                                Limit limit = LocationUtils.findLimitForAddress(mLimitRepository,
                                        address);
                                p.setLimitId(limit != null ? limit.getUid() : 0L);
                            }
                            p.setWatchZoneUpdatedTimestampMs(System.currentTimeMillis());
                        }

                        mWatchZoneRepository.updateWatchZonePoint(p);

                        synchronized (watchZonePoints) {
                            int numDone = size - (int) latch.getCount();
                            int progress = (int) (((double) numDone / (double) size) * 100);
                            publishProgress(progress);
                        }

                        latch.countDown();
                    }
                }
            });
            handlerIndex = (handlerIndex + 1) % numHandlers;
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Result.CANCELLED;
        }

        if (mIsCancelled.get()) {
            return Result.CANCELLED;
        }

        List<WatchZonePoint> watchZonePointsResult =
                mWatchZoneRepository.getWatchZonePoints(watchZone);
        Result result = Result.SUCCESS;
        for (WatchZonePoint p : watchZonePointsResult) {
            // This means it wasn't updated during this call to update().
            if (p.getWatchZoneUpdatedTimestampMs() < currentTimestamp) {
                result = Result.NETWORK_ERROR;
            }
        }

        watchZone.setLastSweepingUpdated(System.currentTimeMillis());
        mWatchZoneRepository.updateWatchZone(watchZone);

        return result;
    }

    private void publishProgress(int progress) {
        if (mListener != null) {
            mListener.onProgress(progress);
        }
    }
}
