package com.example.joseph.sweepersd;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by joseph on 4/7/16.
 */
public class SweepingPosition {
    private static final String TAG = SweepingPosition.class.getSimpleName();

    private final LatLng mLatLng;
    private String mAddress;

    public SweepingPosition(LatLng position) {
        this(position, null);
    }

    public SweepingPosition(LatLng position, String address) {
        mLatLng = position;
        mAddress = address;
    }

    public LatLng getLatLng() {
        return mLatLng;
    }

    public String getAddress() {
        return mAddress;
    }
}
