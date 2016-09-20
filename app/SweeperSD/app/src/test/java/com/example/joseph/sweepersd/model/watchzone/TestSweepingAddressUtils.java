package com.example.joseph.sweepersd.model.watchzone;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.example.joseph.sweepersd.model.limits.LimitSchedule;
import com.google.android.gms.maps.model.LatLng;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by joseph on 9/17/16.
 */
public class TestSweepingAddressUtils {
    @Test
    public void testGetHour() throws Exception {
        for (int i = 0; i < 24; i++) {
            GregorianCalendar test = new GregorianCalendar(
                    2016,
                    1,
                    1,
                    i,
                    0,
                    0);
            Assert.assertEquals(i, WatchZoneUtils.getHour(test));
        }
    }

    @Test
    public void testSortByNextStartTime() throws Exception {

    }

    @Test
    public void testGetUniqueLimits() throws Exception {
        List<SweepingAddress> addresses = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            SweepingAddress address = new SweepingAddress(new LatLng(0, 0), "address",
                    new Limit(i, "street", new int[2], "limit", null));
            addresses.add(address);
        }
        for (int i = 0; i < 5; i++) {
            SweepingAddress address = new SweepingAddress(new LatLng(0, 0), "address",
                    new Limit(i, "street", new int[2], "limit", null));
            addresses.add(address);
        }

        List<Limit> results = WatchZoneUtils.getUniqueLimits(addresses);
        Assert.assertEquals(10, results.size());

        int one = 0;
        int two = 0;
        int three = 0;
        int four = 0;
        int five = 0;
        int six = 0;
        int seven = 0;
        int eight = 0;
        int nine = 0;
        int zero = 0;
        for (Limit l : results) {
            switch (l.getId()) {
                case 0:
                    zero++;
                    break;
                case 1:
                    one++;
                    break;
                case 2:
                    two++;
                    break;
                case 3:
                    three++;
                    break;
                case 4:
                    four++;
                    break;
                case 5:
                    five++;
                    break;
                case 6:
                    six++;
                    break;
                case 7:
                    seven++;
                    break;
                case 8:
                    eight++;
                    break;
                case 9:
                    nine++;
                    break;
            }
        }
        Assert.assertEquals(1, zero);
        Assert.assertEquals(1, one);
        Assert.assertEquals(1, two);
        Assert.assertEquals(1, three);
        Assert.assertEquals(1, four);
        Assert.assertEquals(1, five);
        Assert.assertEquals(1, six);
        Assert.assertEquals(1, seven);
        Assert.assertEquals(1, eight);
        Assert.assertEquals(1, nine);
    }

    @Test
    public void testFilterInvalidLimitSchedules() throws Exception {
        LimitSchedule valid = new LimitSchedule(0, 0, 1, 1);
        LimitSchedule invalid = new LimitSchedule(-1, 0, 1, 1);
        List<LimitSchedule> list = new ArrayList<>();
        list.add(valid);
        list.add(invalid);

        WatchZoneUtils.filterInvalidLimitSchedules(list);

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(valid));
        Assert.assertFalse(list.contains(invalid));
    }

    @Test
    public void testIsValidLimitSchedule() throws Exception {
        // Test every day of the week with every week number with every time
        // Purposefully go over bounds by -1 and +1 to test
        for (int i = 1; i < 5; i++) {
            // Purposefully go over bounds by -1 and +1 to test
            for (int j = 1; j < 8; j++) {
                // Purposefully go over bounds by -1 and +1 to test
                for (int k = 0; k < 24; k++) {
                    // Purposefully go over bounds by -1 and +1 to test
                    for (int l = 0; l < 24; l++) {
                        LimitSchedule schedule = new LimitSchedule(k, l, j, i);
                        Assert.assertTrue(WatchZoneUtils.isValidLimitSchedule(schedule));
                    }
                }
            }
        }
        LimitSchedule schedule = new LimitSchedule(-1, 0, 1, 1);
        Assert.assertFalse(WatchZoneUtils.isValidLimitSchedule(schedule));
        schedule = new LimitSchedule(24, 0, 1, 1);
        Assert.assertFalse(WatchZoneUtils.isValidLimitSchedule(schedule));
        schedule = new LimitSchedule(0, -1, 1, 1);
        Assert.assertFalse(WatchZoneUtils.isValidLimitSchedule(schedule));
        schedule = new LimitSchedule(0, 24, 1, 1);
        Assert.assertFalse(WatchZoneUtils.isValidLimitSchedule(schedule));
        schedule = new LimitSchedule(0, 0, 0, 1);
        Assert.assertFalse(WatchZoneUtils.isValidLimitSchedule(schedule));
        schedule = new LimitSchedule(0, 0, 8, 1);
        Assert.assertFalse(WatchZoneUtils.isValidLimitSchedule(schedule));
        schedule = new LimitSchedule(0, 0, 1, 0);
        Assert.assertFalse(WatchZoneUtils.isValidLimitSchedule(schedule));
        schedule = new LimitSchedule(0, 0, 1, 5);
        Assert.assertFalse(WatchZoneUtils.isValidLimitSchedule(schedule));
    }

    @Test
    public void testFindSweepingDate() throws Exception {
        // Test every day of the week with every week number with every time
        // Purposefully go over bounds by -1 and +1 to test
        for (int i = 0; i < 6; i++) {
            // Purposefully go over bounds by -1 and +1 to test
            for (int j = 0; j < 9; j++) {
                // Purposefully go over bounds by -1 and +1 to test
                for (int k = -1; k < 25; k++) {
                    // Purposefully go over bounds by -1 and +1 to test
                    for (int l = -1; l < 25; l++) {
                        LimitSchedule schedule = new LimitSchedule(k, l, j, i);
                        SweepingDate result = WatchZoneUtils.findSweepingDate(schedule);
                        if (WatchZoneUtils.isValidLimitSchedule(schedule)) {
                            GregorianCalendar today = new GregorianCalendar(
                                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);

                            GregorianCalendar startCalendar = result.getStartTime();
                            GregorianCalendar endCalendar = result.getEndTime();

                            Assert.assertTrue(today.getTime().getTime() <
                                    endCalendar.getTime().getTime());

                            int startDom = startCalendar.get(Calendar.DAY_OF_MONTH);
                            int endDom = endCalendar.get(Calendar.DAY_OF_MONTH);
                            int startDoW = startCalendar.get(Calendar.DAY_OF_WEEK);
                            int endDoW = endCalendar.get(Calendar.DAY_OF_WEEK);

                            // Assert that if the startTime was higher than the end time, the
                            // end calendar is one day ahead
                            if (k > l) {
                                int difference = endDom - startDom;
                                Assert.assertEquals(1, difference);
                                Assert.assertEquals(endDoW, (startDoW % 7) + 1);
                            } else {
                                Assert.assertEquals(startDom, endDom);
                                Assert.assertEquals(startDoW, endDoW);
                            }
                            int hour = WatchZoneUtils.getHour(startCalendar);
                            Assert.assertEquals(hour,
                                    schedule.getStartHour());
                            Assert.assertEquals(startCalendar.get(Calendar.DAY_OF_WEEK),
                                    schedule.getDay());

                            hour = WatchZoneUtils.getHour(endCalendar);
                            Assert.assertEquals(hour,
                                    schedule.getEndHour());

                            Assert.assertEquals(schedule.getWeekNumber(),
                                    ((startCalendar.get(Calendar.DAY_OF_MONTH)-1) / 7) + 1);
                        } else {
                            Assert.assertNull(result);
                        }
                    }
                }
            }
        }
    }
}
