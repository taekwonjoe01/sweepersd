package com.example.joseph.sweepersd.model.alarms;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 9/2/16.
 */
public class TestAlarm {

    @Test
    public void testAlarm() throws Exception {
        long createdTimestamp = 1;
        long lastUpdatedTimestamp = 2;
        LatLng center = new LatLng(0, 0);
        int radius = 5;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();

        Alarm testAlarm = new Alarm(createdTimestamp, lastUpdatedTimestamp, center, radius,
                sweepingAddresses);

        Assert.assertEquals(createdTimestamp, testAlarm.getCreatedTimestamp());
        Assert.assertEquals(lastUpdatedTimestamp, testAlarm.getLastUpdatedTimestamp());
        Assert.assertEquals(center, testAlarm.getCenter());
        Assert.assertEquals(radius, testAlarm.getRadius());
        Assert.assertEquals(sweepingAddresses.size(), testAlarm.getSweepingAddresses().size());

        Alarm copyAlarm = new Alarm(testAlarm);
        Assert.assertEquals(createdTimestamp, copyAlarm.getCreatedTimestamp());
        Assert.assertEquals(lastUpdatedTimestamp, copyAlarm.getLastUpdatedTimestamp());
        Assert.assertEquals(center, copyAlarm.getCenter());
        Assert.assertEquals(radius, copyAlarm.getRadius());
        Assert.assertEquals(sweepingAddresses.size(), copyAlarm.getSweepingAddresses().size());
    }
}
