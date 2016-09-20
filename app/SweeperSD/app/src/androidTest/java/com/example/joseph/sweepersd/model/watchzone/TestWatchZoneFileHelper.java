package com.example.joseph.sweepersd.model.watchzone;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.example.joseph.sweepersd.model.limits.LimitDbHelper;
import com.example.joseph.sweepersd.model.limits.LimitSchedule;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Created by joseph on 9/4/16.
 */
public class TestWatchZoneFileHelper extends AndroidTestCase {
    private static final String TAG = TestWatchZoneFileHelper.class.getSimpleName();
    private RenamingDelegatingContext mContext;
    private WatchZoneFileHelper.WatchZoneUpdateListener mMockListener;
    private CountDownLatch mOnAlarmUpdatedLatch;
    private CountDownLatch mOnAlarmDeletedLatch;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mContext = new RenamingDelegatingContext(getContext(), "test_");

        File file = new File(WatchZoneFileHelper.getAlarmDirPath(mContext));

        deleteRecursive(file);

        mMockListener = new WatchZoneFileHelper.WatchZoneUpdateListener() {
            @Override
            public void onWatchZoneUpdated(long createdTimestamp) {
                mOnAlarmUpdatedLatch.countDown();
            }

            @Override
            public void onWatchZoneDeleted(long createdTimestamp) {
                mOnAlarmDeletedLatch.countDown();
            }
        };

        mOnAlarmUpdatedLatch = new CountDownLatch(1);
        mOnAlarmDeletedLatch = new CountDownLatch(1);

        // Initialize the database.
        LimitDbHelper.LimitImporter importer = new LimitDbHelper.LimitImporter() {
            @Override
            public List<Limit> importLimits(Context context) {
                List<Limit> limits = new ArrayList<>();
                limits.add(createTestLimit());
                return limits;
            }
        };
        LimitDbHelper helper = new LimitDbHelper(mContext, importer);
        helper.getAllLimits();
    }

    private Limit createTestLimit() {
        // Create a limitSchedule:
        int startHour = 10;
        int endHour = 13;
        int day = 4;
        int week = 1;
        LimitSchedule schedule = new LimitSchedule(startHour, endHour, day, week);

        // Create the Limit
        int limitId = 1;
        String address = "beryl";
        int[] range = new int[2];
        range[0] = 1;
        range[1] = 3;
        String descLimit = "descLimit";
        List<LimitSchedule> limitSchedules = new ArrayList<>();
        limitSchedules.add(schedule);
        Limit limit = new Limit(limitId, address, range, descLimit, limitSchedules);

        return limit;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public void testAddAlarm() {
        WatchZoneFileHelper helper = new WatchZoneFileHelper(mContext, mMockListener);
        List<WatchZone> watchZones = helper.loadWatchZones();

        assertEquals(0, watchZones.size());

        // Create a test watchZone.
        // Create the Limit
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the watchZone
        long createdTimestamp = 1;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        WatchZone watchZone = new WatchZone(createdTimestamp, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);

        // Save the watchZone.
        helper.saveWatchZone(watchZone);

        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        WatchZone loadedWatchZone = helper.loadWatchZone(createdTimestamp);

        watchZones = helper.loadWatchZones();

        assertEquals(1, watchZones.size());

        // Check the WatchZone
        assertEquals(createdTimestamp, loadedWatchZone.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedWatchZone.getLastUpdatedTimestamp());
        assertEquals(center, loadedWatchZone.getCenter());
        assertEquals(radius, loadedWatchZone.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses = loadedWatchZone.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses.size());
        SweepingAddress loadedSwAddress = loadedSwAddresses.get(0);
        assertEquals(center, loadedSwAddress.getLatLng());
        assertEquals(address, loadedSwAddress.getAddress());
    }

    public void testOverwriteAlarm() {
        WatchZoneFileHelper helper = new WatchZoneFileHelper(mContext, mMockListener);
        List<WatchZone> watchZones = helper.loadWatchZones();

        assertEquals(0, watchZones.size());

        // Create a test watchZone.
        // Create the Limit
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the watchZone
        long createdTimestamp = 1;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        WatchZone watchZone = new WatchZone(createdTimestamp, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);

        // Save the watchZone.
        mOnAlarmUpdatedLatch = new CountDownLatch(2);
        helper.saveWatchZone(watchZone);
        // Overwrite the save!
        helper.saveWatchZone(watchZone);

        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        WatchZone loadedWatchZone = helper.loadWatchZone(createdTimestamp);

        watchZones = helper.loadWatchZones();

        assertEquals(1, watchZones.size());

        // Check the WatchZone
        assertEquals(createdTimestamp, loadedWatchZone.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedWatchZone.getLastUpdatedTimestamp());
        assertEquals(center, loadedWatchZone.getCenter());
        assertEquals(radius, loadedWatchZone.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses = loadedWatchZone.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses.size());
        SweepingAddress loadedSwAddress = loadedSwAddresses.get(0);
        assertEquals(center, loadedSwAddress.getLatLng());
        assertEquals(address, loadedSwAddress.getAddress());
    }

    public void testMultipleAlarms() {
        WatchZoneFileHelper helper = new WatchZoneFileHelper(mContext, mMockListener);
        List<WatchZone> watchZones = helper.loadWatchZones();

        assertEquals(0, watchZones.size());

        // Create a test watchZone.
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the watchZone
        long createdTimestamp = 1;
        long createdTimestamp2 = 2;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        WatchZone watchZone = new WatchZone(createdTimestamp, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);
        WatchZone watchZone2 = new WatchZone(createdTimestamp2, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);

        // Save the watchZone.
        mOnAlarmUpdatedLatch = new CountDownLatch(2);
        helper.saveWatchZone(watchZone);
        helper.saveWatchZone(watchZone2);


        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        WatchZone loadedWatchZone = helper.loadWatchZone(createdTimestamp);
        WatchZone loadedWatchZone2 = helper.loadWatchZone(createdTimestamp2);

        watchZones = helper.loadWatchZones();

        assertEquals(2, watchZones.size());

        // Check the WatchZone
        assertEquals(createdTimestamp, loadedWatchZone.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedWatchZone.getLastUpdatedTimestamp());
        assertEquals(center, loadedWatchZone.getCenter());
        assertEquals(radius, loadedWatchZone.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses = loadedWatchZone.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses.size());
        SweepingAddress loadedSwAddress = loadedSwAddresses.get(0);
        assertEquals(center, loadedSwAddress.getLatLng());
        assertEquals(address, loadedSwAddress.getAddress());

        // Check the Second WatchZone
        assertEquals(createdTimestamp2, loadedWatchZone2.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedWatchZone2.getLastUpdatedTimestamp());
        assertEquals(center, loadedWatchZone2.getCenter());
        assertEquals(radius, loadedWatchZone2.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses2 = loadedWatchZone2.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses2.size());
        SweepingAddress loadedSwAddress2 = loadedSwAddresses2.get(0);
        assertEquals(center, loadedSwAddress2.getLatLng());
        assertEquals(address, loadedSwAddress2.getAddress());
    }

    public void testGetWatchZoneList() {
        WatchZoneFileHelper helper = new WatchZoneFileHelper(mContext, mMockListener);
        List<Long> watchZones = helper.getWatchZoneList();

        assertEquals(0, watchZones.size());

        // Create a test watchZone.
        // Create the Limit
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the watchZone
        long createdTimestamp = 1;
        long createdTimestamp2 = 2;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        WatchZone watchZone = new WatchZone(createdTimestamp, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);
        WatchZone watchZone2 = new WatchZone(createdTimestamp2, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);

        // Save the watchZone.
        mOnAlarmUpdatedLatch = new CountDownLatch(2);
        helper.saveWatchZone(watchZone);
        helper.saveWatchZone(watchZone2);
        helper.deleteWatchZone(watchZone2);


        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(2000, TimeUnit.MILLISECONDS);
            countedDown &= mOnAlarmDeletedLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        WatchZone loadedWatchZone = helper.loadWatchZone(createdTimestamp);
        WatchZone loadedWatchZone2 = helper.loadWatchZone(createdTimestamp2);

        watchZones = helper.getWatchZoneList();

        assertEquals(1, watchZones.size());

        // Check the WatchZone
        assertEquals(createdTimestamp, loadedWatchZone.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedWatchZone.getLastUpdatedTimestamp());
        assertEquals(center, loadedWatchZone.getCenter());
        assertEquals(radius, loadedWatchZone.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses = loadedWatchZone.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses.size());
        SweepingAddress loadedSwAddress = loadedSwAddresses.get(0);
        assertEquals(center, loadedSwAddress.getLatLng());
        assertEquals(address, loadedSwAddress.getAddress());

        // Check the Second WatchZone
        assertNull(loadedWatchZone2);
    }

    public void testDeleteAlarm() {
        WatchZoneFileHelper helper = new WatchZoneFileHelper(mContext, mMockListener);
        List<WatchZone> watchZones = helper.loadWatchZones();

        assertEquals(0, watchZones.size());

        // Create a test watchZone.
        // Create the Limit
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the watchZone
        long createdTimestamp = 1;
        long createdTimestamp2 = 2;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        WatchZone watchZone = new WatchZone(createdTimestamp, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);
        WatchZone watchZone2 = new WatchZone(createdTimestamp2, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);

        // Save the watchZone.
        mOnAlarmUpdatedLatch = new CountDownLatch(2);
        helper.saveWatchZone(watchZone);
        helper.saveWatchZone(watchZone2);
        helper.deleteWatchZone(watchZone2);


        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(2000, TimeUnit.MILLISECONDS);
            countedDown &= mOnAlarmDeletedLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        WatchZone loadedWatchZone = helper.loadWatchZone(createdTimestamp);
        WatchZone loadedWatchZone2 = helper.loadWatchZone(createdTimestamp2);

        watchZones = helper.loadWatchZones();

        assertEquals(1, watchZones.size());

        // Check the WatchZone
        assertEquals(createdTimestamp, loadedWatchZone.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedWatchZone.getLastUpdatedTimestamp());
        assertEquals(center, loadedWatchZone.getCenter());
        assertEquals(radius, loadedWatchZone.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses = loadedWatchZone.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses.size());
        SweepingAddress loadedSwAddress = loadedSwAddresses.get(0);
        assertEquals(center, loadedSwAddress.getLatLng());
        assertEquals(address, loadedSwAddress.getAddress());

        // Check the Second WatchZone
        assertNull(loadedWatchZone2);
    }

    public void testMultipleHelpers() {
        WatchZoneFileHelper helper = new WatchZoneFileHelper(mContext, null);
        WatchZoneFileHelper helper2 = new WatchZoneFileHelper(mContext, mMockListener);
        List<WatchZone> watchZones = helper.loadWatchZones();

        assertEquals(0, watchZones.size());

        // Create a test watchZone.
        // Create the Limit
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the watchZone
        long createdTimestamp = 1;
        long createdTimestamp2 = 2;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        WatchZone watchZone = new WatchZone(createdTimestamp, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);
        WatchZone watchZone2 = new WatchZone(createdTimestamp2, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);

        // Save the watchZone.
        mOnAlarmUpdatedLatch = new CountDownLatch(2);
        helper.saveWatchZone(watchZone);
        helper.saveWatchZone(watchZone2);
        helper.deleteWatchZone(watchZone2);


        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(2000, TimeUnit.MILLISECONDS);
            countedDown &= mOnAlarmDeletedLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        WatchZone loadedWatchZone = helper2.loadWatchZone(createdTimestamp);
        assertNotNull(loadedWatchZone);
    }
}
