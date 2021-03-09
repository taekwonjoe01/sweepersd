package com.example.joseph.sweepersd.limit;

import androidx.lifecycle.LiveData;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.example.joseph.sweepersd.AppDatabase;
import com.example.joseph.sweepersd.utils.LocationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AddressValidatorManager extends LiveData<Boolean> {
    private static final String TAG = AddressValidatorManager.class.getSimpleName();

    private static final long ONE_MONTH = 1000L * 60L * 60L * 24L * 30L;
    private static final int AUTOSAVE_THRESHOLD = 25;

    private static AddressValidatorManager sInstance;

    private final Context mApplicationContext;
    private final HandlerThread mThread;
    private final Handler mHandler;
    private final AtomicBoolean mIsActive;


    private Map<String, String> mValidatedAddressCache;
    private List<Limit> mUpdatedLimits;

    private AddressValidatorManager(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("AddressValidatorManager-thread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mIsActive = new AtomicBoolean(false);
    }

    public static AddressValidatorManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AddressValidatorManager(context);
        }
        return sInstance;
    }

    @Override
    protected void onActive() {
        super.onActive();
        mIsActive.set(true);
        scheduleWork();
        setValue(true);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        mIsActive.set(false);
        cancelWork();
    }

    private void scheduleWork() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                LimitDao limitDao = AppDatabase.getInstance(mApplicationContext).limitDao();
                if (!mIsActive.get()) {
                    return;
                }
                postValue(true);

                mValidatedAddressCache = new HashMap<>();
                mUpdatedLimits = new ArrayList<>();

                List<Limit> limits = limitDao.getAllLimits();
                int index = 0;
                for (Limit limit : limits) {
                    long timePassed = System.currentTimeMillis() - limit.getAddressValidatedTimestamp();
                    if (timePassed > ONE_MONTH) {
                        mHandler.post(new UpdateAddressTask(limit));
                        index++;
                        if (index >= AUTOSAVE_THRESHOLD) {
                            mHandler.post(new SaveAddressesTask());
                            index = 0;
                        }
                    }
                }
                mHandler.post(new SaveAddressesTask());
                mHandler.post(new SetNotBusyTask());
            }
        });
    }

    private void cancelWork() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mHandler.removeCallbacksAndMessages(null);
                mHandler.post(new SaveAddressesTask());
                mHandler.post(new SetNotBusyTask());
            }
        });
    }

    private class UpdateAddressTask implements Runnable {
        private final Limit mLimit;

        UpdateAddressTask(Limit limit) {
            mLimit = limit;
        }

        @Override
        public void run() {
            String streetBeingValidated = mLimit.getStreet();
            String validatedAddress = null;
            if (mValidatedAddressCache.containsKey(streetBeingValidated)) {
                validatedAddress = mValidatedAddressCache.get(streetBeingValidated);
            } else {
                validatedAddress = LocationUtils.validateStreet(
                        mApplicationContext, streetBeingValidated);
            }

            if (!TextUtils.isEmpty(validatedAddress)) {
                String[] parsings = validatedAddress.split(",");
                if (parsings.length > 0) {
                    String validatedStreet = parsings[0].trim();
                    mLimit.setStreet(validatedStreet);

                    mUpdatedLimits.add(mLimit);
                    mValidatedAddressCache.put(streetBeingValidated, validatedStreet);
                }
            }
        }
    }

    private class SaveAddressesTask implements Runnable {

        SaveAddressesTask() {
        }

        @Override
        public void run() {
            if (!mUpdatedLimits.isEmpty()) {
                LimitDao limitDao = AppDatabase.getInstance(mApplicationContext).limitDao();

                long timestamp = System.currentTimeMillis();
                for (Limit updatedLimit : mUpdatedLimits) {
                    updatedLimit.setAddressValidatedTimestamp(timestamp);
                }
                limitDao.updateLimits(mUpdatedLimits);
                Log.d(TAG, "Updated limit database with " + mUpdatedLimits.size() + " limits.");
            }
            mUpdatedLimits.clear();
        }
    }

    private class SetNotBusyTask implements Runnable {

        SetNotBusyTask() {
        }

        @Override
        public void run() {
            postValue(false);
        }
    }
}
