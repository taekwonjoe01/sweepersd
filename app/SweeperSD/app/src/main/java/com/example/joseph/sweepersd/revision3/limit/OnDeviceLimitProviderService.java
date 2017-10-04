package com.example.joseph.sweepersd.revision3.limit;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.joseph.sweepersd.revision3.AppDatabase;
import com.example.joseph.sweepersd.revision3.utils.Preferences;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnDeviceLimitProviderService extends IntentService {
    private static final String TAG = OnDeviceLimitProviderService.class.getSimpleName();
    private static final String ON_DEVICE_FILE_PREFIX = "district";

    public OnDeviceLimitProviderService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean(Preferences.PREFERENCE_ON_DEVICE_LIMITS_LOADED, false)) {
            return;
        }
        Log.i(TAG, "Starting " + TAG);

        Log.i(TAG, "Parsing on-device files.");
        boolean parseSuccessful = false;
        Map<Limit, List<LimitSchedule>> limitsAndSchedules = new HashMap<>();
        try {
            for (int i = 1; i < 10; i++) {
                String filename = ON_DEVICE_FILE_PREFIX + i + ".txt";
                InputStream is = getAssets().open(filename);
                BufferedReader in=
                        new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String str;

                while ((str=in.readLine()) != null) {
                    Limit l = LimitParser.buildLimitFromLine(str);
                    if (l != null) {
                        List<LimitSchedule> schedules = LimitParser.buildSchedulesFromLine(str);
                        limitsAndSchedules.put(l, schedules);
                    }
                }

                in.close();
                is.close();
            }
            parseSuccessful = true;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        Log.i(TAG, "Finished parsing on-device files. Found " + limitsAndSchedules.size() + " limits.");

        if (parseSuccessful) {
            Log.i(TAG, "Deleting limit database.");
            LimitDao limitDao = AppDatabase.getInstance(this).limitDao();
            List<Limit> toDelete = limitDao.getAllLimits();
            limitDao.deleteAll(toDelete);
            Log.i(TAG, "Limit database deleted.");

            Log.i(TAG, "Processing limits...");
            List<LimitSchedule> totalLimitSchedulesList = new ArrayList<>();
            List<Limit> limits = new ArrayList<>(limitsAndSchedules.keySet());
            for (Limit limit : limits) {
                List<LimitSchedule> schedules = limitsAndSchedules.get(limit);

                if (schedules == null) {
                    limit.setPosted(false);
                } else {
                    limit.setPosted(true);
                }
            }

            Log.i(TAG, "Populating limits table.");
            long[] uids = limitDao.insertLimits(limits);

            Log.i(TAG, "Processing schedules...");
            int limitIndex = 0;
            for (Limit limit : limits) {
                long uid = uids[limitIndex];
                List<LimitSchedule> schedules = limitsAndSchedules.get(limit);

                if (schedules != null) {
                    limit.setPosted(true);
                    for (LimitSchedule sched : schedules) {
                        sched.setLimitId(uid);
                    }
                    totalLimitSchedulesList.addAll(schedules);
                }

                limitIndex++;
            }
            Log.i(TAG, "Populating limit schedules table.");
            limitDao.insertLimitSchedules(totalLimitSchedulesList);

            Log.i(TAG, "Finished service.");

            preferences.edit().putBoolean(Preferences.PREFERENCE_ON_DEVICE_LIMITS_LOADED, true).commit();

            // Any existing WatchZones need to be updated!
            WatchZoneRepository watchZoneRepository = WatchZoneRepository.getInstance(this);
            watchZoneRepository.triggerRefreshAll();
        }
    }
}
