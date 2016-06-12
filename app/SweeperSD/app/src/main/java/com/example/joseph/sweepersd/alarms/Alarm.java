package com.example.joseph.sweepersd.alarms;

import com.example.joseph.sweepersd.LocationDetails;
import com.example.joseph.sweepersd.utils.LocationUtils;

import java.util.GregorianCalendar;
import java.util.List;

/**
 * Data model for an Alarm.
 */
public class Alarm {
    private final LocationDetails mLocationDetails;

    public Alarm(LocationDetails locationDetails) {
        mLocationDetails = locationDetails;
    }

    /**
     * Return the next street sweeping day within a 31 day period. If there is no sweeping in 31
     * days, this will return null.
     * @return
     */
    public GregorianCalendar getNextSweepingDate() {
        GregorianCalendar result = null;

        List<GregorianCalendar> sweepingDays = LocationUtils.getSweepingDaysForLimit(
                mLocationDetails.limit, 31);
        if (!sweepingDays.isEmpty()) {
            result = sweepingDays.get(0);
        }
        return result;
    }

    public List<GregorianCalendar> getNextSweepingDates(int maxDays) {
        return LocationUtils.getSweepingDaysForLimit(mLocationDetails.limit, maxDays);
    }

    public LocationDetails getLocationDetails() {
        return mLocationDetails;
    }
}
