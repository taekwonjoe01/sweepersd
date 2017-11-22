package com.example.joseph.sweepersd.limit;

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
public class LimitDaoTest {
    private LimitDao mLimitDao;
    private AppDatabase mDb;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        mDb = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        mLimitDao = mDb.limitDao();
    }

    @After
    public void closeDb() throws IOException {
        mDb.close();
    }

    @Test
    public void testLimitsAndSchedules() throws Exception {
        for (int i = 0; i < 2; i++) {
            Limit limit = new Limit();
            limit.setAddressValidatedTimestamp(0L);
            limit.setStartRange(0);
            limit.setEndRange(10);
            limit.setPosted(true);
            limit.setStreet("Test Street" + i);

            LimitSchedule schedule1 = new LimitSchedule();
            schedule1.setWeekNumber(1);
            schedule1.setDayNumber(1);
            schedule1.setStartHour(12);
            schedule1.setStartMinute(30);
            schedule1.setEndHour(14);
            schedule1.setEndMinute(15);

            LimitSchedule schedule2 = new LimitSchedule();
            schedule2.setWeekNumber(1);
            schedule2.setDayNumber(2);
            schedule2.setStartHour(12);
            schedule2.setStartMinute(30);
            schedule2.setEndHour(14);
            schedule2.setEndMinute(15);

            long uid = mLimitDao.insertLimit(limit);
            schedule1.setLimitId(uid);
            schedule2.setLimitId(uid);

            mLimitDao.insertLimitSchedule(schedule1);
            mLimitDao.insertLimitSchedule(schedule2);
        }

        List<Limit> limits = mLimitDao.getAllLimits();

        Assert.assertEquals(2, limits.size());

        List<LimitSchedule> schedules = mLimitDao.getAllSchedulesByLimitId(limits.get(0).getUid());
        Assert.assertEquals(2, schedules.size());

        schedules = mLimitDao.getAllSchedules();
        Assert.assertEquals(4, schedules.size());

        List<Limit> toDelete = new ArrayList<>();
        toDelete.add(limits.get(0));
        mLimitDao.deleteAll(toDelete);

        limits = mLimitDao.getAllLimits();

        Assert.assertEquals(1, limits.size());

        schedules = mLimitDao.getAllSchedules();

        Assert.assertEquals(2, schedules.size());
        for (LimitSchedule schedule : schedules) {
            long uid = limits.get(0).getUid();
            Assert.assertEquals(schedule.getLimitId(), uid);
        }
    }
}
