package com.example.joseph.sweepersd.model.limits;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.example.joseph.sweepersd.model.limits.LimitSchedule;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 9/2/16.
 */
public class TestLimit {
    @Test
    public void testLimit() throws Exception {
        int id = -1;
        String street = "beryl";
        int[] range = new int[2];
        range[0] = 1;
        range[1] = 2;
        String limitDesc = "IamALimit";
        List<LimitSchedule> limitSchedules = new ArrayList<>();

        Limit limit = new Limit(id, street, range, limitDesc, limitSchedules);

        Assert.assertEquals(id, limit.getId());
        Assert.assertEquals(street, limit.getStreet());
        Assert.assertEquals(range[0], limit.getRange()[0]);
        Assert.assertEquals(range[1], limit.getRange()[1]);
        Assert.assertEquals(limitDesc, limit.getLimit());
        Assert.assertEquals(limitSchedules.size(), limit.getSchedules().size());
    }
}
