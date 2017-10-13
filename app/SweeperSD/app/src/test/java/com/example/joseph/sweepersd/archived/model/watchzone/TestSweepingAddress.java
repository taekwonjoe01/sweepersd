package com.example.joseph.sweepersd.archived.model.watchzone;

import com.example.joseph.sweepersd.archived.model.limits.Limit;
import com.google.android.gms.maps.model.LatLng;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Created by joseph on 9/2/16.
 */
public class TestSweepingAddress {
    @Test
    public void testAlarm() throws Exception {
        LatLng latLng = new LatLng(0, 0);
        String address = "label";
        Limit limit = null;

        SweepingAddress testAddress = new SweepingAddress(latLng, address, limit);
        Assert.assertEquals(latLng, testAddress.getLatLng());
        Assert.assertEquals(address, testAddress. getAddress());
        Assert.assertNull(testAddress.getLimit());
    }
}
