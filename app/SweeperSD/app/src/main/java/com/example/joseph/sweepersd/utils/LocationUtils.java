package com.example.joseph.sweepersd.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.example.joseph.sweepersd.Limit;
import com.example.joseph.sweepersd.LimitManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by joseph on 4/7/16.
 */
public class LocationUtils {
    private static final String TAG = LocationUtils.class.getSimpleName();

    public static List<Long> getTimesUntilLimit(Limit limit, int maxDays) {
        List<Long> results = new ArrayList<>();
        if (limit != null) {
            List<GregorianCalendar> days = getSweepingDaysForLimit(limit, maxDays);
            GregorianCalendar today = new GregorianCalendar();

            for (GregorianCalendar c : days) {
                long msTilSweeping = c.getTime().getTime() - today.getTime().getTime();
                Log.d(TAG, "msTilSweeping: " + msTilSweeping);
                results.add(msTilSweeping);
            }
        }
        return results;
    }

    public static List<GregorianCalendar> getSweepingDaysForLimit(Limit l, int maxDays) {
        List<GregorianCalendar> results = new ArrayList<>();
        for (String schedule : l.getSchedules()) {
            String timeString = LimitManager.getTimeString(schedule);
            if (timeString == null) {
                Log.e(TAG, "Parse Error on " + l.getStreet() + " :: " + l.getLimit()
                        + " :: " + l.getSchedules());
            } else {
                String[] parsings = timeString.split("-");
                int startTime = LimitManager.convertTimeStringToHour(parsings[0]);
                int endTime = LimitManager.convertTimeStringToHour(parsings[1]);
                if (startTime > -1 && endTime > -1) {
                    long sweepingEndedTime = ((endTime - startTime) * 3600000);
                    for (int i = 0; i < l.getSchedules().size(); i++) {
                        List<GregorianCalendar> sweepingDates = LimitManager.getSweepingDates(startTime, endTime, maxDays,
                                l.getSchedules().get(i));
                        GregorianCalendar today = new GregorianCalendar();
                        for (GregorianCalendar c : sweepingDates) {
                            long valid = c.getTime().getTime() - today.getTime().getTime() + sweepingEndedTime;
                            if (valid > 0) {
                                results.add(c);
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "StartTime or endTime was -1: " + startTime + " " + endTime);
                }
            }
        }
        return results;
    }

    public static List<Address> getAddressesForLocation(Context context, Location location) {
        long start = System.nanoTime();
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        List<Address> addresses = new ArrayList<>();

        if (location != null) {
            try {
                geocoder.getFromLocation()
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        10);
            } catch (IOException ioException) {
                // TODO: Catch network or other I/O problems.
            } catch (IllegalArgumentException illegalArgumentException) {
                // TODO: Catch invalid latitude or longitude values.
            }
        }
        long end = System.nanoTime();
        Log.d(TAG, "getting Address took: " + (end - start) / 1000000 + "ms");
        return addresses;
    }
}
