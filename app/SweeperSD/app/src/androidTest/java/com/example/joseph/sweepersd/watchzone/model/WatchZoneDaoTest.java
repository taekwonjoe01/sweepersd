package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.example.joseph.sweepersd.AppDatabase;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WatchZoneDaoTest {
    private WatchZoneDao mWatchZoneDao;
    private AppDatabase mDb;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        mDb = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        mWatchZoneDao = mDb.watchZoneDao();
    }

    @After
    public void closeDb() throws IOException {
        mDb.close();
    }

    @Test
    public void testWatchZonesAndPoints() throws Exception {
        for (int i = 0; i < 2; i++) {
            WatchZone watchZone = new WatchZone();
            watchZone.setCenterLatitude(0.0);
            watchZone.setCenterLongitude(0.0);
            watchZone.setRadius(30);
            watchZone.setLabel("Test Zone" + i);
            watchZone.setRemindPolicy(0);
            watchZone.setRemindRange(0);

            WatchZonePoint point1 = new WatchZonePoint();
            point1.setAddress(null);
            point1.setLatitude(0.001);
            point1.setLongitude(0.001);
            point1.setWatchZoneUpdatedTimestampMs(0L);

            WatchZonePoint point2 = new WatchZonePoint();
            point2.setAddress(null);
            point2.setLatitude(-0.001);
            point2.setLongitude(-0.001);
            point2.setWatchZoneUpdatedTimestampMs(0L);

            long uid = mWatchZoneDao.insertWatchZone(watchZone);
            point1.setWatchZoneId(uid);
            point2.setWatchZoneId(uid);

            List<WatchZonePoint> points = new ArrayList<>();
            points.add(point1);
            points.add(point2);
            mWatchZoneDao.insertWatchZonePoints(points);
        }

        List<WatchZone> watchZones = mWatchZoneDao.getAllZones();

        Assert.assertEquals(2, watchZones.size());

        List<WatchZonePoint> points = mWatchZoneDao.getWatchZonePointsForWatchZoneId(watchZones.get(0).getUid());
        Assert.assertEquals(2, points.size());

        points = mWatchZoneDao.getAllWatchZonePoints();
        Assert.assertEquals(4, points.size());

        mWatchZoneDao.deleteWatchZone(watchZones.get(0));

        watchZones = mWatchZoneDao.getAllZones();

        Assert.assertEquals(1, watchZones.size());

        points = mWatchZoneDao.getAllWatchZonePoints();

        Assert.assertEquals(2, points.size());
        for (WatchZonePoint point : points) {
            long uid = watchZones.get(0).getUid();
            Assert.assertEquals(point.getWatchZoneId(), uid);
        }
    }
}
