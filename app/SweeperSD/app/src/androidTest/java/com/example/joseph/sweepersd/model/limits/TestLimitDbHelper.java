package com.example.joseph.sweepersd.model.limits;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Log;

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
        Log.d(TAG, "getAllLimits duration: " + durationMs + "ms.");
        assertNotSame(0, limits.size());
    }
}
