package com.example.joseph.sweepersd.revision3.watchzone;

import android.text.TextUtils;

import com.example.joseph.sweepersd.revision3.LocationUtils;
import com.example.joseph.sweepersd.revision3.limit.Limit;
import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.google.android.gms.maps.model.LatLng;

public class WatchZonePointUpdater implements Runnable {
    public static final long WATCH_ZONE_UP_TO_DATE_TIME_MS = 1000 * 60 * 60 * 24 * 30;
    private final WatchZonePoint mWatchZonePoint;
    private final WatchZoneRepository mWatchZoneRepository;
    private final LimitRepository mLimitRepository;
    private final WatchZoneUpdater.AddressProvider mAddressProvider;

    public WatchZonePointUpdater(WatchZonePoint watchZonePoint, WatchZoneRepository watchZoneRepository,
                                 LimitRepository limitRepository,
                                 WatchZoneUpdater.AddressProvider addressProvider) {
        mWatchZonePoint = watchZonePoint;
        mWatchZoneRepository = watchZoneRepository;
        mLimitRepository = limitRepository;
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
                    Limit limit = LocationUtils.findLimitForAddress(mLimitRepository,
                            address);
                    mWatchZonePoint.setLimitId(limit != null ? limit.getUid() : 0L);
                }
                mWatchZonePoint.setWatchZoneUpdatedTimestampMs(System.currentTimeMillis());
            }

            mWatchZoneRepository.updateWatchZonePoint(mWatchZonePoint);
        }
    }
}
