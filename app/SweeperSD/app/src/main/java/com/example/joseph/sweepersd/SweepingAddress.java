package com.example.joseph.sweepersd;

import com.example.joseph.sweepersd.limits.Limit;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by joseph on 4/7/16.
 */
public class SweepingAddress {
    private static final String TAG = SweepingAddress.class.getSimpleName();

    private final LatLng mLatLng;
    private final String mAddress;
    private final Limit mLimit;

    public SweepingAddress(LatLng position, String address, Limit limit) {
        mLatLng = position;
        mAddress = address;
        mLimit = limit;
    }

    public LatLng getLatLng() {
        return mLatLng;
    }

    public String getAddress() {
        return mAddress;
    }

    public Limit getLimit() {
        return mLimit;
    }
}
