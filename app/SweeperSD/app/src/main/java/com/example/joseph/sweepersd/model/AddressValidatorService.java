package com.example.joseph.sweepersd.model;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.example.joseph.sweepersd.model.limits.LimitDbHelper;
import com.example.joseph.sweepersd.model.watchzone.WatchZone;
import com.example.joseph.sweepersd.model.watchzone.WatchZoneManager;
import com.example.joseph.sweepersd.utils.LocationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 9/10/16.
 */
public class AddressValidatorService extends IntentService {
    private static final String TAG = AddressValidatorService.class.getSimpleName();
    public static final String ACTION_VALIDATOR_PROGRESS =
            "com.example.joseph.sweepersd.ACTION_VALIDATOR_PROGRESS";
    public static final String ACTION_VALIDATOR_FINISHED =
            "com.example.joseph.sweepersd.ACTION_VALIDATOR_FINISHED";
    public static final String PARAM_PROGRESS = "PARAM_PROGRESS";

    private HashMap<String, String> mFoundAddresses = new HashMap<>();
    private List<Limit> mFailedLimits = new ArrayList<>();

    public AddressValidatorService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Starting " + TAG);

        LimitDbHelper limitHelper = new LimitDbHelper(this);

        List<Limit> limits = limitHelper.getAllLimits();

        for (int i = 0; i < limits.size(); i++) {
            Limit l = limits.get(i);

            int totalSize = limits.size() + mFailedLimits.size();
            int progress = (int) (((double)i / ((double)totalSize)) * 100);
            publishProgress(progress);

            String streetBeingValidated = l.getStreet();
            String validatedAddress = null;
            if (mFoundAddresses.containsKey(streetBeingValidated)) {
                validatedAddress = mFoundAddresses.get(streetBeingValidated);
            } else {
                validatedAddress = LocationUtils.validateStreet(this, streetBeingValidated);
            }
            Log.d(TAG, "validated street for <" + streetBeingValidated + "> is <" + validatedAddress + ">");

            if (validatedAddress != null) {
                String[] parsings = validatedAddress.split(",");
                if (parsings.length > 0) {
                    String validatedStreet = parsings[0].trim();
                    Limit replacementLimit = new Limit(l.getId(), validatedStreet, l.getRange(),
                            l.getLimit(), l.getSchedules());
                    limitHelper.updateLimit(replacementLimit);

                    mFoundAddresses.put(streetBeingValidated, validatedStreet);
                } else {
                    mFailedLimits.add(l);
                }
            } else {
                mFailedLimits.add(l);
            }
        }

        for (int i = 0; i < mFailedLimits.size(); i++) {
            Limit failedLimit = mFailedLimits.get(i);

            int totalSize = limits.size() + mFailedLimits.size();
            int progress = (int) (((double)(i + limits.size()) / ((double)totalSize)) * 100);
            publishProgress(progress);

            String streetBeingValidated = failedLimit.getStreet();
            String validatedAddress = null;
            if (mFoundAddresses.containsKey(streetBeingValidated)) {
                validatedAddress = mFoundAddresses.get(streetBeingValidated);
            } else {
                validatedAddress = LocationUtils.validateStreet(this, streetBeingValidated);
            }

            if (validatedAddress != null) {
                String[] parsings = validatedAddress.split(",");
                if (parsings.length > 0) {
                    String validatedStreet = parsings[0].trim();
                    Limit replacementLimit = new Limit(failedLimit.getId(), validatedStreet,
                            failedLimit.getRange(), failedLimit.getLimit(),
                            failedLimit.getSchedules());
                    limitHelper.updateLimit(replacementLimit);

                    mFoundAddresses.put(streetBeingValidated, validatedStreet);
                }
            }
        }

        Log.i(TAG, "Finishing " + TAG);

        WatchZoneManager manager = new WatchZoneManager(this);
        List<Long> alarmIds = manager.getWatchZones();
        for (Long id : alarmIds) {
            WatchZone watchZone = manager.getWatchZone(id);

            // This will resave the watchZone, and if it's currently being updated,
            // the update will restart.
            manager.updateWatchZone(watchZone);
        }

        publishFinished();
    }

    private void publishProgress(int progress) {
        Log.d(TAG, "publishing progress: " + progress);
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_PROGRESS, progress);

        sendBroadcast(bundle, ACTION_VALIDATOR_PROGRESS);
    }

    private void publishFinished() {
        Bundle bundle = new Bundle();

        sendBroadcast(bundle, ACTION_VALIDATOR_FINISHED);
    }

    private void sendBroadcast(Bundle bundle, String action) {
        Intent intent = new Intent(action);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }
}
