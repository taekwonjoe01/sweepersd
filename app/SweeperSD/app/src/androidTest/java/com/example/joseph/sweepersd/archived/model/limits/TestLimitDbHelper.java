package com.example.joseph.sweepersd.archived.model.limits;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Log;

import com.example.joseph.sweepersd.archived.model.AddressValidatorManager;

import java.util.List;

/**
 * Created by joseph on 8/28/16.
 */
public class TestLimitDbHelper extends AndroidTestCase {
    private static final String TAG = TestLimitDbHelper.class.getSimpleName();
    private LimitDbHelper mDbHelper;
    private RenamingDelegatingContext mContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mContext = new RenamingDelegatingContext(getContext(), "test_");
        mDbHelper = new LimitDbHelper(mContext, new FileLimitImporter());
        AddressValidatorManager.getInstance(mContext).setAddressValidator(
                new AddressValidatorManager.AddressValidator() {
            @Override
            public void validateAddresses(AddressValidatorManager.ValidatorProgressListener listener) {
                // Do nothing
            }

            @Override
            public int getProgress() {
                return AddressValidatorManager.INVALID_PROGRESS;
            }
        });
    }

    @Override
    public void tearDown() throws Exception {
        mDbHelper.close();
        super.tearDown();
    }

    public void testDatabaseLoad() {
        long start = System.nanoTime();
        List<Limit> limits = mDbHelper.getAllLimits();
        long end = System.nanoTime();

        long durationMs = (end - start) / 1000000;
        Log.d(TAG, "getAllWatchZones duration: " + durationMs + "ms.");
        assertNotSame(0, limits.size());

        limits = mDbHelper.getLimitsForStreet("beryl st");
        assertNotSame(0, limits.size());
        for (Limit limit : limits) {
            Log.d(TAG, limit.getStreet());
            for (LimitSchedule schedule : limit.getSchedules()) {
                Log.d(TAG, schedule.getDay() + " " + schedule.getWeekNumber());
            }
        }
    }
}
