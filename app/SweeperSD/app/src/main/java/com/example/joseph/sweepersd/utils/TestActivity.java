package com.example.joseph.sweepersd.utils;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitModel;
import com.example.joseph.sweepersd.limit.LimitRepository;
import com.example.joseph.sweepersd.limit.LimitSchedule;

import java.util.List;
import java.util.Map;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        LimitRepository.getInstance(this).getPostedLimitsLiveData().observe(this, new Observer<List<LimitModel>>() {
            @Override
            public void onChanged(@Nullable List<LimitModel> limitModels) {
                Log.e("Joey", "onChanged");
                if (limitModels != null) {
                    Log.e("Joey", "not null");
                    if (!limitModels.isEmpty()) {
                        Log.e("Joey", "not empty");
                        for (LimitModel m : limitModels) {
                            List<LimitSchedule> schedules = m.schedules;
                            if (schedules != null) {
                                Log.e("Joey", "schedules not null");
                                if (!schedules.isEmpty()) {
                                    Log.e("Joey", "schedules not empty");
                                    break;
                                }
                            }
                        }
                    }
                }
                Log.e("Joey", "~~~~~~~~~~~~~~~~~~~~~~~~");
            }
        });
    }

}
