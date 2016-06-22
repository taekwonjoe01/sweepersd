package com.example.joseph.sweepersd.alarms;

import com.example.joseph.sweepersd.SweepingPosition;
import com.example.joseph.sweepersd.utils.LocationUtils;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Data model for an Alarm.
 */
public class Alarm {
    private final List<SweepingPosition> mSweepingPositions;

    public Alarm(List<SweepingPosition> sweepingPositions) {
        mSweepingPositions = sweepingPositions;
    }

    /**
     * Return the next street sweeping day within a 31 day period. If there is no sweeping in 31
     * days, this will return null.
     * @return
     */
    public GregorianCalendar getNextSweepingDate() {
        GregorianCalendar result = null;
        for (SweepingPosition pos : mSweepingPositions) {
            List<GregorianCalendar> sweepingDays = LocationUtils.getSweepingDaysForLimit(
                    pos.getLimit(), 31);
            if (!sweepingDays.isEmpty()) {
                GregorianCalendar potentialNew = sweepingDays.get(0);
                if (result == null ||
                        potentialNew.getTime().getTime() < result.getTime().getTime()) {
                    result = potentialNew;
                }
            }
        }
        return result;
    }

    public List<GregorianCalendar> getNextSweepingDates(int maxDays) {
        List<GregorianCalendar> result = new ArrayList<>();
        for (SweepingPosition pos : mSweepingPositions) {
            result.addAll(LocationUtils.getSweepingDaysForLimit(pos.getLimit(), maxDays));
        }
        return result;
    }

    public List<SweepingPosition> getSweepingPositions() {
        return mSweepingPositions;
    }
}
