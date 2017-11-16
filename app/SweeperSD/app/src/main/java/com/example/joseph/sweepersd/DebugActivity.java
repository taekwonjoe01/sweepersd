package com.example.joseph.sweepersd;

import android.arch.lifecycle.Observer;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitDao;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.example.joseph.sweepersd.utils.LongPreferenceLiveData;
import com.example.joseph.sweepersd.utils.Preferences;
import com.example.joseph.sweepersd.watchzone.WatchZoneExplorerActivity;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneDao;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DebugActivity extends AppCompatActivity {
    private TextView mUpdaterLastStarted;
    private TextView mUpdaterLastFinished;
    private Button mScheduleUpdater;

    private Button mCreateActive;
    private Button mCreateSoon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        mScheduleUpdater = findViewById(R.id.button_schedule_updater);
        mUpdaterLastStarted = findViewById(R.id.textview_updater_started);
        mUpdaterLastFinished = findViewById(R.id.textview_updater_finished);
        mCreateActive = findViewById(R.id.button_create_zone_and_active_limit);
        mCreateSoon = findViewById(R.id.button_create_zone_and_soon_limit);

        LongPreferenceLiveData updaterStart = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_APP_UPDATER_LAST_STARTED);
        updaterStart.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mUpdaterLastStarted.setText("never");
                    } else {
                        mUpdaterLastStarted.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        LongPreferenceLiveData updaterFinish = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_APP_UPDATER_LAST_FINISHED);
        updaterFinish.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mUpdaterLastFinished.setText("never");
                    } else {
                        mUpdaterLastFinished.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        mScheduleUpdater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppUpdateJob.scheduleJob(DebugActivity.this);
            }
        });
        mCreateActive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(DebugActivity.this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED ) {
                    ActivityCompat.requestPermissions(DebugActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                            0);
                } else {
                    LocationServices.getFusedLocationProviderClient(DebugActivity.this).getLastLocation()
                            .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                if (createLimit(location, true)) {
                                    createWatchZone(location);
                                }
                            }
                        }
                    });
                }
            }
        });
        mCreateSoon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(DebugActivity.this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED ) {
                    ActivityCompat.requestPermissions(DebugActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                            0);
                } else {
                    LocationServices.getFusedLocationProviderClient(DebugActivity.this).getLastLocation()
                            .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                if (createLimit(location, false)) {
                                    createWatchZone(location);
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private void createWatchZone(Location location) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(this).watchZoneDao();

        WatchZoneModelRepository.getInstance(this).createWatchZone("Test Zone",
                location.getLatitude(), location.getLongitude(), 50);

        AppUpdateJob.scheduleJob(this);
    }

    private boolean  createLimit(Location location, boolean active) {
        boolean limitCreated = false;

        LimitDao limitDao = AppDatabase.getInstance(this).limitDao();

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        String address = LocationUtils.getAddressForLatLnt(this, latLng);
        if (address != null && !TextUtils.isEmpty(address)) {

            GregorianCalendar now = new GregorianCalendar();
            if (!active) {
                now.add(Calendar.MINUTE, 2);
            }
            GregorianCalendar end = new GregorianCalendar();
            if (!active) {
                end.add(Calendar.MINUTE, 2);
            }
            end.add(Calendar.HOUR, 1);

            String[] split = address.split(",");
            if (split.length > 1) {
                String streetAddress = split[0];
                String[] streetAddressParsings = streetAddress.split(" ");
                if (streetAddressParsings.length > 1) {
                    String streetNumber = streetAddressParsings[0];
                    String streetName = "";
                    for (int j = 1; j < streetAddressParsings.length; j++) {
                        streetName += " " + streetAddressParsings[j];
                    }
                    streetName = streetName.trim();
                    if (streetNumber.contains("-")) {
                        String[] streetNumberParsings = streetNumber.split("-");
                        if (streetNumberParsings.length == 2) {
                            try {
                                int minNum = Integer.parseInt(streetNumberParsings[0]);

                                Limit limit = new Limit();
                                limit.setAddressValidatedTimestamp(System.currentTimeMillis());
                                limit.setEndRange(minNum + 100);
                                limit.setStartRange(0);
                                limit.setStreet(streetName);
                                limit.setPosted(true);
                                limit.setRawLimitString("some garbage");

                                long uid = limitDao.insertLimit(limit);
                                limit.setUid(uid);

                                LimitSchedule schedule = new LimitSchedule();
                                schedule.setWeekNumber(now.get(Calendar.WEEK_OF_MONTH));
                                schedule.setDayNumber(now.get(Calendar.DAY_OF_WEEK));
                                schedule.setStartHour(now.get(Calendar.HOUR_OF_DAY));
                                schedule.setStartMinute(now.get(Calendar.MINUTE));
                                schedule.setEndHour(end.get(Calendar.HOUR_OF_DAY));
                                schedule.setEndMinute(end.get(Calendar.MINUTE));
                                schedule.setLimitId(uid);

                                limitDao.insertLimitSchedule(schedule);

                                limitCreated = true;
                            } catch (NumberFormatException e) {
                            }
                        }
                    } else {
                        try {
                            int num = Integer.parseInt(streetNumber);

                            Limit limit = new Limit();
                            limit.setAddressValidatedTimestamp(System.currentTimeMillis());
                            limit.setEndRange(num + 100);
                            limit.setStartRange(0);
                            limit.setStreet(streetName);
                            limit.setPosted(true);
                            limit.setRawLimitString("some garbage");

                            long uid = limitDao.insertLimit(limit);
                            limit.setUid(uid);

                            LimitSchedule schedule = new LimitSchedule();
                            schedule.setWeekNumber(now.get(Calendar.WEEK_OF_MONTH));
                            schedule.setDayNumber(now.get(Calendar.DAY_OF_WEEK));
                            schedule.setStartHour(now.get(Calendar.HOUR_OF_DAY));
                            schedule.setStartMinute(now.get(Calendar.MINUTE));
                            schedule.setEndHour(end.get(Calendar.HOUR_OF_DAY));
                            schedule.setEndMinute(end.get(Calendar.MINUTE));
                            schedule.setLimitId(uid);

                            limitDao.insertLimitSchedule(schedule);

                            limitCreated = true;
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
        }
        if (!limitCreated) {
            Toast.makeText(this, "Failed to make limit for current location.", Toast.LENGTH_SHORT).show();
        }

        return limitCreated;
    }
}
