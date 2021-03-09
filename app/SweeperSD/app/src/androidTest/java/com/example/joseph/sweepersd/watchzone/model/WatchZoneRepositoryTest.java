package com.example.joseph.sweepersd.watchzone.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.Context;

import com.example.joseph.sweepersd.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest(AppDatabase.class)
@RunWith(PowerMockRunner.class)
public class WatchZoneRepositoryTest {
    @Mock
    Context mMockContext;
    @Mock
    Context mMockApplicationContext;
    @Mock
    AppDatabase mMockDatabase;
    @Mock
    WatchZoneDao mMockDao;

    private Map<Long, LiveData<WatchZoneModel>> mMockData;

    @Before
    public void setup() {
        mMockData = new HashMap<>();

        when(mMockContext.getApplicationContext()).thenReturn(mMockApplicationContext);
        when(AppDatabase.getInstance(any(Context.class))).thenReturn(mMockDatabase);
        when(mMockDatabase.watchZoneDao()).thenReturn(mMockDao);

        /*when(mMockDao.getZoneLiveDataForUid())
        when(mMockDao.deleteWatchZone())
        when(mMockDao.insertWatchZonePoints())
        when(mMockDao.updateWatchZone())
        when(mMockDao.getWatchZone())
        when(mMockDao.insertWatchZonePointLimits())
        when(mMockDao.deleteWatchZonePointLimits())
        when(mMockDao.updateWatchZonePoint())
        when(mMockDao.insertWatchZone())*/
    }

    @After
    public void cleanup() {

    }

    // create Watch Zone
    // update WZP
    // updateWatchZone
    // delete WatchZone
    // getters
    @Test
    public void test() {
        when(mMockDao.getAllZonesLiveData()).thenReturn(null);
        LiveData<List<WatchZoneModel>> liveDataModels = WatchZoneModelRepository.getInstance(mMockContext).getCachedWatchZoneModelsLiveData();
        assertThat(liveDataModels, null);


        when(mMockDao.getZoneLiveDataForUid(1L)).thenReturn(new MutableLiveData<WatchZoneModel>());
        LiveData<WatchZoneModel> liveModel = WatchZoneModelRepository.getInstance(mMockContext).getZoneModelForUid(1L);
        verify(mMockDao.getZoneLiveDataForUid(anyLong()), times(1));
        assertNotNull(liveModel);

        liveModel = WatchZoneModelRepository.getInstance(mMockContext).getZoneModelForUid(1L);
        verify(mMockDao.getZoneLiveDataForUid(anyLong()), times(1));
    }
}
