package com.example.joseph.sweepersd;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.util.Log;

import com.example.joseph.sweepersd.utils.LocationUtils;

import java.util.List;

/**
 * Created by joseph on 4/7/16.
 */
public class LocationDetails {
    private static final String TAG = LocationDetails.class.getSimpleName();

    public static LocationDetails createFromLocation(Context context, Location location) {
        LocationDetails locationDetails = new LocationDetails();
        locationDetails.location = location;

        List<Address> addressesForLimit = LocationUtils.getAddressesForLocation(context, location);
        locationDetails.addresses = addressesForLimit;

        locationDetails.limit = findLimitForAddresses(addressesForLimit);

        return locationDetails;
    }

    private static Limit findLimitForAddresses(List<Address> addresses) {
        Limit result = null;
        for (Address address : addresses) {
            String a = "";
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                a += address.getAddressLine(i) + ",";
            }
            a = a.toLowerCase();
            Log.d(TAG, "findLimitForAddress: " + a);
            // TODO: This should check more dynamically the city.
            if (a.contains("ca") && a.contains("san diego")) {
                String[] split = a.split(",");
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
                    Log.e(TAG, "Malformed address " + a);
                }
            } else {
                Log.w(TAG, "City is not San Diego. Address is  " + a);
            }
        }
        return result;
    }

    private static Limit checkAddress(int houseNumber, String street) {
        Log.d(TAG, "houseNumber: " + houseNumber + " - Street: " + street);
        Limit result = null;
        for (Limit l : LimitManager.getPostedLimits()) {
            if (l.getStreet().toLowerCase().contains(street)) {
                if (houseNumber >= l.getRange()[0] && houseNumber <= l.getRange()[1]) {
                    result = l;
                    Log.d(TAG, "THIS HOUSE IS IN LIMIT RANGE");
                }
            }
        }
        return result;
    }

    public Limit limit;
    public Location location;
    public List<Address> addresses;
}
