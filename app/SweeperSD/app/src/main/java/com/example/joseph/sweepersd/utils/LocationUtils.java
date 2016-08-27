package com.example.joseph.sweepersd.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.example.joseph.sweepersd.Limit;
import com.example.joseph.sweepersd.LimitManager;
import com.google.android.gms.maps.model.LatLng;

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

    public static List<LatLng> getLatLngsInRadius(LatLng latLng, int radius) {
        double latitude = latLng.latitude;
        double longitude = latLng.longitude;

        double latitudeRadians = latLng.latitude * Math.PI / 180;
        double longitudeRadians = latLng.longitude * Math.PI / 180;

        // Set up "Constants"
        double m1 = 111132.92;     // latitude calculation term 1
        double m2 = -559.82;       // latitude calculation term 2
        double m3 = 1.175;         // latitude calculation term 3
        double m4 = -0.0023;       // latitude calculation term 4
        double p1 = 111412.84;     // longitude calculation term 1
        double p2 = -93.5;         // longitude calculation term 2
        double p3 = 0.118;         // longitude calculation term 3

        // Calculate the length of a degree of latitude and longitude in meters
        double metersPerDegreeLat = m1 + (m2 * Math.cos(2 * latitudeRadians)) + (m3 * Math.cos(4 * latitudeRadians)) +
                (m4 * Math.cos(6 * latitudeRadians));
        double metersPerDegreeLng = (p1 * Math.cos(latitudeRadians)) + (p2 * Math.cos(3 * latitudeRadians)) +
                (p3 * Math.cos(5 * latitudeRadians));

        int meterStep = 10;
        int numSteps = radius / meterStep;

        List<LatLng> latLngs = new ArrayList<>();

        for (int i = 0; i <= numSteps; i++) {
            double lngLimit = ((double)radius) *
                    Math.sin(Math.acos(((double)(i * meterStep)) / ((double)radius)));
            double curLngMeters = 0;
            while (curLngMeters < lngLimit) {
                double newLat = latitude + ((i * meterStep) / metersPerDegreeLat);
                double newLng = longitude + (curLngMeters / metersPerDegreeLng);
                latLngs.add(new LatLng(newLat, newLng));
                curLngMeters += meterStep;
            }
            curLngMeters = meterStep;
            while (curLngMeters < lngLimit) {
                double newLat = latitude + ((i * meterStep) / metersPerDegreeLat);
                double newLng = longitude - (curLngMeters / metersPerDegreeLng);
                latLngs.add(new LatLng(newLat, newLng));
                curLngMeters += meterStep;
            }
        }
        for (int i = 1; i <= numSteps; i++) {
            double lngLimit = ((double)radius) *
                    Math.sin(Math.acos(((double)(i * meterStep)) / ((double)radius)));
            double curLngMeters = 0;
            while (curLngMeters < lngLimit) {
                double newLat = latitude - ((i * meterStep) / metersPerDegreeLat);
                double newLng = longitude + (curLngMeters / metersPerDegreeLng);
                latLngs.add(new LatLng(newLat, newLng));
                curLngMeters += meterStep;
            }
            curLngMeters = meterStep;
            while (curLngMeters < lngLimit) {
                double newLat = latitude - ((i * meterStep) / metersPerDegreeLat);
                double newLng = longitude - (curLngMeters / metersPerDegreeLng);
                latLngs.add(new LatLng(newLat, newLng));
                curLngMeters += meterStep;
            }
        }

        return latLngs;
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

    public static String getAddressForLatLnt(Context context, LatLng latLng) {
        String result = null;
        List<Address> addresses = getAddressesForLatLng(context, latLng);
        if (addresses != null && !addresses.isEmpty()) {
            Address first = addresses.get(0);
            result = "";
            for (int i = 0; i < first.getMaxAddressLineIndex(); i++) {
                result += first.getAddressLine(i) + ",";
            }
            result = result.toLowerCase();
        }
        return result;
    }

    public static List<Address> getAddressesForLatLng(Context context, LatLng latLng) {
        long start = System.nanoTime();
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        List<Address> addresses = new ArrayList<>();

        if (latLng != null) {
            try {
                //geocoder.getFromLocation()
                addresses = geocoder.getFromLocation(
                        latLng.latitude,
                        latLng.longitude,
                        10);
            } catch (IOException ioException) {
                // TODO: Catch network or other I/O problems.
            } catch (IllegalArgumentException illegalArgumentException) {
                // TODO: Catch invalid latitude or longitude values.
            }
        }
        long end = System.nanoTime();
        //Log.d(TAG, "getting Address took: " + (end - start) / 1000000 + "ms");
        return addresses;
    }

    public static Limit findLimitForAddresses(List<Address> addresses) {
        Limit result = null;
        for (Address address : addresses) {
            String a = "";
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                a += address.getAddressLine(i) + ",";
            }
            a = a.toLowerCase();
            Log.d(TAG, "findLimitForAddress: " + a);
            // TODO: This should check more dynamically the city.
            findLimitForAddress(a);
        }
        return result;
    }

    public static Limit findLimitForAddress(String address) {
        Limit result = null;
        if (address != null && address.contains("ca") && address.contains("san diego")) {
            String[] split = address.split(",");
            if (split.length > 1) {
                String streetAddress = split[0];
                String[] streetAddressParsings = streetAddress.split(" ");
                if (streetAddressParsings.length > 1) {
                    String streetNumber = streetAddressParsings[0];
                    String streetName = "";
                    for (int j = 1; j < streetAddressParsings.length; j++) {
                        streetName += " " + streetAddressParsings[j];
                    }
                    streetName = streetName.trim();
                    if (streetNumber.contains("-")) {
                        String[] streetNumberParsings = streetNumber.split("-");
                        if (streetNumberParsings.length == 2) {
                            try {
                                int minNum = Integer.parseInt(streetNumberParsings[0]);
                                int maxNum = Integer.parseInt(streetNumberParsings[1]);

                                Limit minLimit = checkAddress(minNum, streetName);
                                Limit maxLimit = checkAddress(maxNum, streetName);
                                result = (minLimit != null) ? minLimit :
                                        (maxLimit != null) ? maxLimit : null;
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Malformed Street numbers: " + streetNumber);
                            }
                        } else {
                            Log.e(TAG, "Malformed Street numbers: " + streetNumber);
                        }
                    } else {
                        try {
                            int num = Integer.parseInt(streetNumber);
                            Limit l = checkAddress(num, streetName);
                            result = (l != null) ? l : null;
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Malformed Street numbers: " + streetNumber);
                        }
                    }
                } else {
                    Log.e(TAG, "Malformed street address " + streetAddress);
                }
            } else {
                Log.e(TAG, "Malformed address " + address);
            }
        } else {
            Log.w(TAG, "City is not San Diego. Address is  " + address);
        }
        return result;
    }

    private static Limit checkAddress(int houseNumber, String street) {
        //Log.d(TAG, "houseNumber: " + houseNumber + " - Street: " + street);
        Limit result = null;
        for (Limit l : LimitManager.getPostedLimits()) {
            if (l.getStreet().toLowerCase().contains(street)) {
                if (houseNumber >= l.getRange()[0] && houseNumber <= l.getRange()[1]) {
                    result = l;
                }
            }
        }
        return result;
    }
}
