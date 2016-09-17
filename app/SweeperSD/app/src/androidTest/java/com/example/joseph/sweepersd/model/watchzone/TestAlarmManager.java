package com.example.joseph.sweepersd.model.watchzone;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Log;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.example.joseph.sweepersd.model.limits.LimitDbHelper;
import com.example.joseph.sweepersd.model.limits.LimitSchedule;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by joseph on 9/2/16.
 */
public class TestAlarmManager extends AndroidTestCase {
    private static final String TAG = TestAlarmManager.class.getSimpleName();
    private RenamingDelegatingContext mContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mContext = new RenamingDelegatingContext(getContext(), "test_");

        File file = new File(WatchZoneFileHelper.getAlarmDirPath(mContext));

        deleteRecursive(file);

        // Initialize the database.
        LimitDbHelper.LimitImporter importer = new LimitDbHelper.LimitImporter() {
            @Override
            public List<Limit> importLimits(Context context) {
                return createTestLimits();
            }
        };
        LimitDbHelper helper = new LimitDbHelper(mContext, importer);
        helper.getAllLimits();
    }

    private List<Limit> createTestLimits() {
        List<Limit> limits = new ArrayList<>();
        for (int i = 1; i < 50; i++) {
            List<LimitSchedule> limitSchedules = new ArrayList<>();
            int val = i & 5;
            limitSchedules.add(new LimitSchedule(10, 13, 1+val, 2));
            limitSchedules.add(new LimitSchedule(10, 10+val, 5, 4));

            // Create the Limit
            int limitId = 1;
            String address = "address" + i;
            int[] range = new int[2];
            range[0] = 1;
            range[1] = 50;
            String descLimit = "descLimit";
            limits.add(new Limit(limitId, address, range, descLimit, limitSchedules));
        }

        return limits;
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAlarmManager() {
        WatchZoneManager manager = new WatchZoneManager(mContext);

        final CountDownLatch updatedLatch = new CountDownLatch(1);
        final CountDownLatch createdLatch = new CountDownLatch(1);
        final CountDownLatch deletedLatch = new CountDownLatch(1);
        final CountDownLatch assureListenerRemovedLatch = new CountDownLatch(100);
        WatchZoneManager.AlarmChangeListener alarmChangeListener =
                new WatchZoneManager.AlarmChangeListener() {
            @Override
            public void onAlarmUpdated(Long createdTimestamp) {
                updatedLatch.countDown();
                assureListenerRemovedLatch.countDown();
            }

            @Override
            public void onAlarmCreated(Long createdTimestamp) {
                createdLatch.countDown();
                assureListenerRemovedLatch.countDown();
            }

            @Override
            public void onAlarmDeleted(Long createdTimestamp) {
                deletedLatch.countDown();
                assureListenerRemovedLatch.countDown();
            }
        };
        final CountDownLatch progressLatch = new CountDownLatch(9);
        final CountDownLatch completeLatch = new CountDownLatch(1);
        WatchZoneUpdateManager.AlarmProgressListener alarmProgressListener =
                new WatchZoneUpdateManager.AlarmProgressListener() {
            @Override
            public void onAlarmUpdateProgress(long createdTimestamp, int progress) {
                progressLatch.countDown();
                assureListenerRemovedLatch.countDown();
            }

            @Override
            public void onAlarmUpdateComplete(long createdTimestamp) {
                completeLatch.countDown();
                assureListenerRemovedLatch.countDown();
            }
        };
        MockAlarmUpdaterFactory testFactory = new MockAlarmUpdaterFactory();
        WatchZoneUpdateManager.getInstance(mContext).setAlarmUpdaterFactory(testFactory);

        manager.addAlarmChangeListener(alarmChangeListener);
        manager.addAlarmProgressListener(alarmProgressListener);

        LatLng center = new LatLng(10, 10);
        int radius = 10;

        long timestamp = manager.createAlarm(center, radius);
        Log.e("Joey", "createAlarm1");

        boolean created = false;
        boolean updated = false;
        boolean deleted = false;
        boolean progressed = false;
        boolean completed = false;
        try {
            created = createdLatch.await(1000, TimeUnit.MILLISECONDS);
            updated = updatedLatch.await(1000, TimeUnit.MILLISECONDS);
            deleted = deletedLatch.await(1000, TimeUnit.MILLISECONDS);
            progressed = progressLatch.await(1000, TimeUnit.MILLISECONDS);
            completed = completeLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch(InterruptedException e) {

        }
        assertTrue(created);
        assertFalse(updated);
        assertFalse(deleted);
        assertTrue(progressed);
        assertTrue(completed);

        Set<Long> timestamps = manager.getAlarms();
        assertTrue(timestamps.contains(timestamp));
        assertEquals(1, timestamps.size());

        WatchZone watchZone = manager.getAlarm(timestamp);
        assertNotNull(watchZone);
        assertEquals(radius, watchZone.getRadius());
        assertEquals(center, watchZone.getCenter());

        manager.removeAlarmChangeListener(alarmChangeListener);
        manager.removeAlarmProgressListener(alarmProgressListener);

        final CountDownLatch updatedLatch2 = new CountDownLatch(2);
        final CountDownLatch createdLatch2 = new CountDownLatch(2);
        final CountDownLatch deletedLatch2 = new CountDownLatch(2);
        alarmChangeListener =
                new WatchZoneManager.AlarmChangeListener() {
                    @Override
                    public void onAlarmUpdated(Long createdTimestamp) {
                        updatedLatch2.countDown();
                    }

                    @Override
                    public void onAlarmCreated(Long createdTimestamp) {
                        createdLatch2.countDown();
                    }

                    @Override
                    public void onAlarmDeleted(Long createdTimestamp) {
                        deletedLatch2.countDown();
                    }
                };
        final CountDownLatch progressLatch2 = new CountDownLatch(18);
        final CountDownLatch completeLatch2 = new CountDownLatch(2);
        alarmProgressListener =
                new WatchZoneUpdateManager.AlarmProgressListener() {
                    @Override
                    public void onAlarmUpdateProgress(long createdTimestamp, int progress) {
                        progressLatch2.countDown();
                    }

                    @Override
                    public void onAlarmUpdateComplete(long createdTimestamp) {
                        completeLatch2.countDown();
                    }
                };

        manager.addAlarmChangeListener(alarmChangeListener);
        manager.addAlarmProgressListener(alarmProgressListener);

        LatLng center2 = new LatLng(11, 11);
        int radius2 = 11;
        long timestamp2 = manager.createAlarm(center2, radius2);
        Log.e("Joey", "createAlarm2");

        LatLng center3 = new LatLng(12, 12);
        int radius3 = 12;
        long timestamp3 = manager.createAlarm(center3, radius3);
        Log.e("Joey", "createAlarm3");

        created = false;
        updated = false;
        deleted = false;
        progressed = false;
        completed = false;
        boolean assureRemovedListener = false;
        try {
            created = createdLatch2.await(1000, TimeUnit.MILLISECONDS);
            updated = updatedLatch2.await(1000, TimeUnit.MILLISECONDS);
            deleted = deletedLatch2.await(1000, TimeUnit.MILLISECONDS);
            progressed = progressLatch2.await(1000, TimeUnit.MILLISECONDS);
            completed = completeLatch2.await(1000, TimeUnit.MILLISECONDS);
            assureRemovedListener = assureListenerRemovedLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch(InterruptedException e) {

        }
        assertTrue(created);
        assertFalse(updated);
        assertFalse(deleted);
        assertTrue(progressed);
        assertTrue(completed);
        assertFalse(assureRemovedListener);

        timestamps = manager.getAlarms();
        assertTrue(timestamps.contains(timestamp));
        assertTrue(timestamps.contains(timestamp2));
        assertTrue(timestamps.contains(timestamp3));
        assertEquals(3, timestamps.size());

        watchZone = manager.getAlarm(timestamp);
        assertNotNull(watchZone);
        assertEquals(radius, watchZone.getRadius());
        assertEquals(center, watchZone.getCenter());

        watchZone = manager.getAlarm(timestamp2);
        assertNotNull(watchZone);
        assertEquals(radius2, watchZone.getRadius());
        assertEquals(center2, watchZone.getCenter());

        watchZone = manager.getAlarm(timestamp3);
        assertNotNull(watchZone);
        assertEquals(radius3, watchZone.getRadius());
        assertEquals(center3, watchZone.getCenter());



        manager.removeAlarmChangeListener(alarmChangeListener);
        manager.removeAlarmProgressListener(alarmProgressListener);

        final CountDownLatch updatedLatch3 = new CountDownLatch(1);
        final CountDownLatch createdLatch3 = new CountDownLatch(1);
        final CountDownLatch deletedLatch3 = new CountDownLatch(1);
        alarmChangeListener =
                new WatchZoneManager.AlarmChangeListener() {
                    @Override
                    public void onAlarmUpdated(Long createdTimestamp) {
                        updatedLatch3.countDown();
                    }

                    @Override
                    public void onAlarmCreated(Long createdTimestamp) {
                        createdLatch3.countDown();
                    }

                    @Override
                    public void onAlarmDeleted(Long createdTimestamp) {
                        deletedLatch3.countDown();
                    }
                };
        final CountDownLatch progressLatch3 = new CountDownLatch(18);
        final CountDownLatch completeLatch3 = new CountDownLatch(2);
        alarmProgressListener =
                new WatchZoneUpdateManager.AlarmProgressListener() {
                    @Override
                    public void onAlarmUpdateProgress(long createdTimestamp, int progress) {
                        progressLatch3.countDown();
                    }

                    @Override
                    public void onAlarmUpdateComplete(long createdTimestamp) {
                        completeLatch3.countDown();
                    }
                };

        manager.addAlarmChangeListener(alarmChangeListener);
        manager.addAlarmProgressListener(alarmProgressListener);

        LatLng updatedLatLng = new LatLng(13, 13);
        int updatedRadius = 13;
        manager.refreshAlarm(timestamp);
        Log.e("Joey", "refreshAlarm");
        manager.updateAlarm(timestamp2, updatedLatLng, updatedRadius);
        Log.e("Joey", "updateAlarm");
        manager.deleteAlarm(timestamp3);
        Log.e("Joey", "deleteAlarm");

        created = false;
        updated = false;
        deleted = false;
        progressed = false;
        completed = false;
        try {
            created = createdLatch3.await(1000, TimeUnit.MILLISECONDS);
            updated = updatedLatch3.await(1000, TimeUnit.MILLISECONDS);
            deleted = deletedLatch3.await(1000, TimeUnit.MILLISECONDS);
            progressed = progressLatch3.await(1000, TimeUnit.MILLISECONDS);
            completed = completeLatch3.await(1000, TimeUnit.MILLISECONDS);
        } catch(InterruptedException e) {

        }
        assertFalse(created);
        assertTrue(updated);
        assertTrue(deleted);
        assertTrue(progressed);
        assertTrue(completed);

        timestamps = manager.getAlarms();
        assertTrue(timestamps.contains(timestamp));
        assertTrue(timestamps.contains(timestamp2));
        assertFalse(timestamps.contains(timestamp3));
        assertEquals(2, timestamps.size());

        watchZone = manager.getAlarm(timestamp);
        assertNotNull(watchZone);
        assertEquals(radius, watchZone.getRadius());
        assertEquals(center, watchZone.getCenter());

        watchZone = manager.getAlarm(timestamp2);
        assertNotNull(watchZone);
        assertEquals(updatedRadius, watchZone.getRadius());
        assertEquals(updatedLatLng, watchZone.getCenter());
    }

    private class MockAlarmUpdaterFactory implements WatchZoneUpdateManager.AlarmUpdaterFactory {
        @Override
        public WatchZoneUpdateManager.AlarmUpdater createNewAlarmUpdater() {
            return new MockAlarmUpdater();
        }
    }

    private class MockAlarmUpdater implements WatchZoneUpdateManager.AlarmUpdater {
        private final Handler mHandler;
        private int mProgress = 0;
        private WatchZoneUpdateManager.AlarmProgressListener mListener;
        private WatchZone mWatchZone;
        public MockAlarmUpdater() {
            mHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public int getProgress() {
            return mProgress;
        }

        @Override
        public void updateAlarm(WatchZone watchZone, WatchZoneUpdateManager.AlarmProgressListener listener) {
            mListener = listener;
            mWatchZone = watchZone;
            scheduleWork();
        }

        private void scheduleWork() {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgress += 10;
                    if (mProgress >= 100) {
                        mProgress = 100;
                        mListener.onAlarmUpdateComplete(mWatchZone.getCreatedTimestamp());
                    } else {
                        mListener.onAlarmUpdateProgress(mWatchZone.getCreatedTimestamp(), mProgress);
                        scheduleWork();
                    }
                }
            }, 50);
        }
    }
}
