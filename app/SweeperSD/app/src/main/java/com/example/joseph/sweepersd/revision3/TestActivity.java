package com.example.joseph.sweepersd.revision3;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.revision3.limit.Limit;
import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.example.joseph.sweepersd.revision3.limit.LimitSchedule;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestActivity extends AppCompatActivity {
    private LimitRepository mLimitRepository;

    private Map<Limit, Observer> mPostedLimits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        /*mPostedLimits = new HashMap<>();
        mLimitRepository = LimitRepository.getInstance(this);
        Log.e("Joey" ,"starting..");

        mLimitRepository.getLimits().observe(this, new Observer<List<Limit>>() {
            @Override
            public void onChanged(@Nullable List<Limit> limits) {
                //Log.e("Joey", "onChanged");

                if (limits != null && !limits.isEmpty()) {
                    Log.e("Joey", "number of limits: " + limits.size());
                    for (final Limit l : limits) {
                        if (l.isPosted() && !mPostedLimits.containsKey(l)) {
                            Observer<List<LimitSchedule>> observer = new Observer<List<LimitSchedule>>() {
                                @Override
                                public void onChanged(@Nullable List<LimitSchedule> schedules) {
                                    if (schedules != null && !schedules.isEmpty()) {
                                        for (LimitSchedule schedule : schedules) {
                                            if (schedule.getLimitId() != l.getUid()) {
                                                Log.e("Joey", "sched doesn't equal!");
                                            }
                                        }
                                    }
                                }
                            };
                            mLimitRepository.getLimitSchedules(l).observe(TestActivity.this, observer);
                            LiveData<List<LimitSchedule>> cachedvalue = mLimitRepository.getLimitSchedules(l);
                            mPostedLimits.put(l, observer);
                        }
                    }
                }
            }
        });*/

        WatchZoneRepository repository = WatchZoneRepository.getInstance(this);
        repository.createWatchZone("sampleWatchZone", 32.952848,
                -117.249217, 200);

        for (int i = 0; i < 20; i++) {
            long start = SystemClock.elapsedRealtime();
            repository.createWatchZone("sampleWatchZone", 32.952848,
                    -117.249217, 1000);
            long end = SystemClock.elapsedRealtime();
            long differenceMillis = end - start;
            Log.e("Joey", "elapsed time " + differenceMillis + " ms");
        }
    }

}
