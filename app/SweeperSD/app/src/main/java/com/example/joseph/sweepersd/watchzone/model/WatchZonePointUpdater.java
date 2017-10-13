package com.example.joseph.sweepersd.watchzone.model;

import android.text.TextUtils;

import com.example.joseph.sweepersd.utils.LocationUtils;
import com.example.joseph.sweepersd.limit.Limit;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Map;

public class WatchZonePointUpdater implements Runnable {
    public static final long WATCH_ZONE_UP_TO_DATE_TIME_MS = 1000L * 60L * 60L * 24L * 30L;
    private final WatchZonePoint mWatchZonePoint;
    private final WatchZoneUpdater.WatchZonePointSaveDelegate mSaveDelegate;
    private final Map<String, List<Limit>> mLimits;
    private final WatchZoneUpdater.AddressProvider mAddressProvider;

    public WatchZonePointUpdater(WatchZonePoint watchZonePoint,
                                 WatchZoneUpdater.WatchZonePointSaveDelegate saveDelegate,
                                 Map<String, List<Limit>> limits,
                                 WatchZoneUpdater.AddressProvider addressProvider) {
        mWatchZonePoint = watchZonePoint;
        mSaveDelegate = saveDelegate;
        mLimits = limits;
        mAddressProvider = addressProvider;
    }

    @Override
    public void run() {
        long currentTimestamp = System.currentTimeMillis();
        long timeSinceUpdate = currentTimestamp - mWatchZonePoint.getWatchZoneUpdatedTimestampMs();

        if (timeSinceUpdate > WATCH_ZONE_UP_TO_DATE_TIME_MS) {
            LatLng latLng = new LatLng(mWatchZonePoint.getLatitude(), mWatchZonePoint.getLongitude());
            String address = mAddressProvider.getAddressForLatLng(latLng);

            if (address != null) {
                mWatchZonePoint.setAddress(address);
                if (!TextUtils.isEmpty(address)) {
                    Limit limit = LocationUtils.findLimitForAddress(mLimits,
                            address);
                    mWatchZonePoint.setLimitId(limit != null ? limit.getUid() : 0L);
                }
                mWatchZonePoint.setWatchZoneUpdatedTimestampMs(System.currentTimeMillis());
            }

            mSaveDelegate.saveWatchZonePoint(mWatchZonePoint);
        }
    }
}
