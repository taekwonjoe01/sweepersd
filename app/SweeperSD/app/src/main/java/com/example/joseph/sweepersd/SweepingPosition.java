package com.example.joseph.sweepersd;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by joseph on 4/7/16.
 */
public class SweepingPosition {
    private static final String TAG = SweepingPosition.class.getSimpleName();

    private Limit mLimit;
    private LatLng mLatLng;
    private String mAddress;

    public SweepingPosition(Limit limit, LatLng latLng, String address) {
        mLimit = limit;
        mLatLng = latLng;
        mAddress = address;
    }

    public Limit getLimit() {
        return mLimit;
    }

    public LatLng getLatLng() {
        return mLatLng;
    }

    public String getAddress() {
        return mAddress;
    }

    /*public static SweepingPosition createFromLocation(Context context, Location location) {
        SweepingPosition sweepingPosition = new SweepingPosition();
        sweepingPosition.location = location;

        List<Address> addressesForLimit = LocationUtils.getAddressesForLatLng(context, location);
        sweepingPosition.addresses = addressesForLimit;

        sweepingPosition.limit = findLimitForAddresses(addressesForLimit);

        return sweepingPosition;
    }*/
}
