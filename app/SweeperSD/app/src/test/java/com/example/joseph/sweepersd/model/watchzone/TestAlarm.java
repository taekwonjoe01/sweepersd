package com.example.joseph.sweepersd.model.watchzone;

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
        String address = "address";
        int radius = 5;
        List<SweepingAddress> sweepingAddresses = new ArrayList<>();

        WatchZone testWatchZone = new WatchZone(createdTimestamp, lastUpdatedTimestamp, address, center, radius,
                sweepingAddresses);

        Assert.assertEquals(createdTimestamp, testWatchZone.getCreatedTimestamp());
        Assert.assertEquals(lastUpdatedTimestamp, testWatchZone.getLastUpdatedTimestamp());
        Assert.assertEquals(center, testWatchZone.getCenter());
        Assert.assertEquals(address, testWatchZone.getAddress());
        Assert.assertEquals(radius, testWatchZone.getRadius());
        Assert.assertEquals(sweepingAddresses.size(), testWatchZone.getSweepingAddresses().size());

        WatchZone copyWatchZone = new WatchZone(testWatchZone);
        Assert.assertEquals(createdTimestamp, copyWatchZone.getCreatedTimestamp());
        Assert.assertEquals(lastUpdatedTimestamp, copyWatchZone.getLastUpdatedTimestamp());
        Assert.assertEquals(center, copyWatchZone.getCenter());
        Assert.assertEquals(address, testWatchZone.getAddress());
        Assert.assertEquals(radius, copyWatchZone.getRadius());
        Assert.assertEquals(sweepingAddresses.size(), copyWatchZone.getSweepingAddresses().size());
    }
}
