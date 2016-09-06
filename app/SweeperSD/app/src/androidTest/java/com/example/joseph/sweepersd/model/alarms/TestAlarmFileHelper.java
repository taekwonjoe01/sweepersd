package com.example.joseph.sweepersd.model.alarms;

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
public class TestAlarmFileHelper extends AndroidTestCase {
    private static final String TAG = TestAlarmFileHelper.class.getSimpleName();
    private RenamingDelegatingContext mContext;
    private AlarmFileHelper.AlarmUpdateListener mMockListener;
    private CountDownLatch mOnAlarmUpdatedLatch;
    private CountDownLatch mOnAlarmDeletedLatch;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mContext = new RenamingDelegatingContext(getContext(), "test_");

        File file = new File(AlarmFileHelper.getAlarmDirPath(mContext));

        deleteRecursive(file);

        mMockListener = new AlarmFileHelper.AlarmUpdateListener() {
            @Override
            public void onAlarmUpdated(long createdTimestamp) {
                mOnAlarmUpdatedLatch.countDown();
            }

            @Override
            public void onAlarmDeleted(long createdTimestamp) {
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
        AlarmFileHelper helper = new AlarmFileHelper(mContext, mMockListener);
        List<Alarm> alarms = helper.loadAlarms();

        assertEquals(0, alarms.size());

        // Create a test alarm.
        // Create the Limit
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the alarm
        long createdTimestamp = 1;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        Alarm alarm = new Alarm(createdTimestamp, lastUpdatedTimestamp, center, radius,
                sweepingAddresses);

        // Save the alarm.
        helper.saveAlarm(alarm);

        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        Alarm loadedAlarm = helper.loadAlarm(createdTimestamp);

        alarms = helper.loadAlarms();

        assertEquals(1, alarms.size());

        // Check the Alarm
        assertEquals(createdTimestamp, loadedAlarm.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedAlarm.getLastUpdatedTimestamp());
        assertEquals(center, loadedAlarm.getCenter());
        assertEquals(radius, loadedAlarm.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses = loadedAlarm.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses.size());
        SweepingAddress loadedSwAddress = loadedSwAddresses.get(0);
        assertEquals(center, loadedSwAddress.getLatLng());
        assertEquals(address, loadedSwAddress.getAddress());
    }

    public void testOverwriteAlarm() {
        AlarmFileHelper helper = new AlarmFileHelper(mContext, mMockListener);
        List<Alarm> alarms = helper.loadAlarms();

        assertEquals(0, alarms.size());

        // Create a test alarm.
        // Create the Limit
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the alarm
        long createdTimestamp = 1;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        Alarm alarm = new Alarm(createdTimestamp, lastUpdatedTimestamp, center, radius,
                sweepingAddresses);

        // Save the alarm.
        mOnAlarmUpdatedLatch = new CountDownLatch(2);
        helper.saveAlarm(alarm);
        // Overwrite the save!
        helper.saveAlarm(alarm);

        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        Alarm loadedAlarm = helper.loadAlarm(createdTimestamp);

        alarms = helper.loadAlarms();

        assertEquals(1, alarms.size());

        // Check the Alarm
        assertEquals(createdTimestamp, loadedAlarm.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedAlarm.getLastUpdatedTimestamp());
        assertEquals(center, loadedAlarm.getCenter());
        assertEquals(radius, loadedAlarm.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses = loadedAlarm.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses.size());
        SweepingAddress loadedSwAddress = loadedSwAddresses.get(0);
        assertEquals(center, loadedSwAddress.getLatLng());
        assertEquals(address, loadedSwAddress.getAddress());
    }

    public void testMultipleAlarms() {
        AlarmFileHelper helper = new AlarmFileHelper(mContext, mMockListener);
        List<Alarm> alarms = helper.loadAlarms();

        assertEquals(0, alarms.size());

        // Create a test alarm.
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the alarm
        long createdTimestamp = 1;
        long createdTimestamp2 = 2;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        Alarm alarm = new Alarm(createdTimestamp, lastUpdatedTimestamp, center, radius,
                sweepingAddresses);
        Alarm alarm2 = new Alarm(createdTimestamp2, lastUpdatedTimestamp, center, radius,
                sweepingAddresses);

        // Save the alarm.
        mOnAlarmUpdatedLatch = new CountDownLatch(2);
        helper.saveAlarm(alarm);
        helper.saveAlarm(alarm2);


        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        Alarm loadedAlarm = helper.loadAlarm(createdTimestamp);
        Alarm loadedAlarm2 = helper.loadAlarm(createdTimestamp2);

        alarms = helper.loadAlarms();

        assertEquals(2, alarms.size());

        // Check the Alarm
        assertEquals(createdTimestamp, loadedAlarm.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedAlarm.getLastUpdatedTimestamp());
        assertEquals(center, loadedAlarm.getCenter());
        assertEquals(radius, loadedAlarm.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses = loadedAlarm.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses.size());
        SweepingAddress loadedSwAddress = loadedSwAddresses.get(0);
        assertEquals(center, loadedSwAddress.getLatLng());
        assertEquals(address, loadedSwAddress.getAddress());

        // Check the Second Alarm
        assertEquals(createdTimestamp2, loadedAlarm2.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedAlarm2.getLastUpdatedTimestamp());
        assertEquals(center, loadedAlarm2.getCenter());
        assertEquals(radius, loadedAlarm2.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses2 = loadedAlarm2.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses2.size());
        SweepingAddress loadedSwAddress2 = loadedSwAddresses2.get(0);
        assertEquals(center, loadedSwAddress2.getLatLng());
        assertEquals(address, loadedSwAddress2.getAddress());
    }

    public void testDeleteAlarm() {
        AlarmFileHelper helper = new AlarmFileHelper(mContext, mMockListener);
        List<Alarm> alarms = helper.loadAlarms();

        assertEquals(0, alarms.size());

        // Create a test alarm.
        // Create the Limit
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the alarm
        long createdTimestamp = 1;
        long createdTimestamp2 = 2;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        Alarm alarm = new Alarm(createdTimestamp, lastUpdatedTimestamp, center, radius,
                sweepingAddresses);
        Alarm alarm2 = new Alarm(createdTimestamp2, lastUpdatedTimestamp, center, radius,
                sweepingAddresses);

        // Save the alarm.
        mOnAlarmUpdatedLatch = new CountDownLatch(2);
        helper.saveAlarm(alarm);
        helper.saveAlarm(alarm2);
        helper.deleteAlarm(alarm2);


        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(2000, TimeUnit.MILLISECONDS);
            countedDown &= mOnAlarmDeletedLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        Alarm loadedAlarm = helper.loadAlarm(createdTimestamp);
        Alarm loadedAlarm2 = helper.loadAlarm(createdTimestamp2);

        alarms = helper.loadAlarms();

        assertEquals(1, alarms.size());

        // Check the Alarm
        assertEquals(createdTimestamp, loadedAlarm.getCreatedTimestamp());
        assertEquals(lastUpdatedTimestamp, loadedAlarm.getLastUpdatedTimestamp());
        assertEquals(center, loadedAlarm.getCenter());
        assertEquals(radius, loadedAlarm.getRadius());
        // Check the SweepingAddresses
        List<SweepingAddress> loadedSwAddresses = loadedAlarm.getSweepingAddresses();
        assertEquals(1, loadedSwAddresses.size());
        SweepingAddress loadedSwAddress = loadedSwAddresses.get(0);
        assertEquals(center, loadedSwAddress.getLatLng());
        assertEquals(address, loadedSwAddress.getAddress());

        // Check the Second Alarm
        assertNull(loadedAlarm2);
    }

    public void testMultipleHelpers() {
        AlarmFileHelper helper = new AlarmFileHelper(mContext, null);
        AlarmFileHelper helper2 = new AlarmFileHelper(mContext, mMockListener);
        List<Alarm> alarms = helper.loadAlarms();

        assertEquals(0, alarms.size());

        // Create a test alarm.
        // Create the Limit
        Limit limit = createTestLimit();

        // Create a SweepingAddress
        String address = "1061 beryl st";
        LatLng center = new LatLng(12,12);
        SweepingAddress swAddress = new SweepingAddress(center, address, limit);

        // Create the alarm
        long createdTimestamp = 1;
        long createdTimestamp2 = 2;
        long lastUpdatedTimestamp = 0;
        int radius = 12;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();
        sweepingAddresses.add(swAddress);

        Alarm alarm = new Alarm(createdTimestamp, lastUpdatedTimestamp, center, radius,
                sweepingAddresses);
        Alarm alarm2 = new Alarm(createdTimestamp2, lastUpdatedTimestamp, center, radius,
                sweepingAddresses);

        // Save the alarm.
        mOnAlarmUpdatedLatch = new CountDownLatch(2);
        helper.saveAlarm(alarm);
        helper.saveAlarm(alarm2);
        helper.deleteAlarm(alarm2);


        boolean countedDown = false;
        try {
            countedDown = mOnAlarmUpdatedLatch.await(2000, TimeUnit.MILLISECONDS);
            countedDown &= mOnAlarmDeletedLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {

        }
        assertTrue(countedDown);

        Alarm loadedAlarm = helper2.loadAlarm(createdTimestamp);
        assertNotNull(loadedAlarm);
    }
}
