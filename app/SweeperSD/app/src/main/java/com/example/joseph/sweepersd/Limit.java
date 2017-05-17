package com.example.joseph.sweepersd;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by joseph on 3/13/16.
 */
public class Limit {
    private String mStreet;
    private int[] mRange = new int[2];
    private String mLimit;
    private List<String> mSchedules;
    private LatLng mStartLatLng;
    private LatLng mEndLatLng;

    public Limit(String street, int[] range, String limit, List<String> schedules) {
        mStreet = street;
        mRange = range;
        mLimit = limit;
        mSchedules = schedules;
    }

    public String getStreet() {
        return this.mStreet;
    }

    public int[] getRange() {
        return this.mRange;
    }

    public String getLimit() {
        return this.mLimit;
    }

    public List<String> getSchedules() {
        return this.mSchedules;
    }

    public void setStartLatLng(LatLng latLng) {
        mStartLatLng = latLng;
    }

    public LatLng getStartLatLng() {
        return mStartLatLng;
    }

    public void setEndLatLng(LatLng latLng) {
        mEndLatLng = latLng;
    }

    public LatLng getEndLatLng() {
        return mEndLatLng;
    }
}
