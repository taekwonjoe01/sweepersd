package com.example.joseph.sweepersd

import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.joseph.sweepersd.limit.Limit
import com.example.joseph.sweepersd.limit.LimitSchedule
import com.example.joseph.sweepersd.utils.LocationUtils
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository
import com.google.android.gms.maps.model.LatLng
import java.util.*

class DebugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

//        LongPreferenceLiveData updaterStart = new LongPreferenceLiveData(this,
//                Preferences.PREFERENCE_APP_UPDATER_LAST_STARTED);
//        updaterStart.observe(this, new Observer<Long>() {
//            @Override
//            public void onChanged(@Nullable Long timestamp) {
//                if (timestamp != null) {
//                    if (timestamp == 0L) {
//                        mUpdaterLastStarted.setText("never");
//                    } else {
//                        mUpdaterLastStarted.setText(new Date(timestamp).toString());
//                    }
//                }
//            }
//        });
//        LongPreferenceLiveData updaterFinish = new LongPreferenceLiveData(this,
//                Preferences.PREFERENCE_APP_UPDATER_LAST_FINISHED);
//        updaterFinish.observe(this, new Observer<Long>() {
//            @Override
//            public void onChanged(@Nullable Long timestamp) {
//                if (timestamp != null) {
//                    if (timestamp == 0L) {
//                        mUpdaterLastFinished.setText("never");
//                    } else {
//                        mUpdaterLastFinished.setText(new Date(timestamp).toString());
//                    }
//                }
//            }
//        });
//        mScheduleUpdater.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                AppUpdateJob.scheduleJob(DebugActivity.this);
//            }
//        });
//        mCreateActive.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (ContextCompat.checkSelfPermission(DebugActivity.this,
//                        android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
//                        PackageManager.PERMISSION_GRANTED ) {
//                    ActivityCompat.requestPermissions(DebugActivity.this,
//                            new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
//                            0);
//                } else {
//                    LocationServices.getFusedLocationProviderClient(DebugActivity.this).getLastLocation()
//                            .addOnSuccessListener(new OnSuccessListener<Location>() {
//                        @Override
//                        public void onSuccess(Location location) {
//                            if (location != null) {
//                                if (createLimit(location, true)) {
//                                    createWatchZone(location);
//                                }
//                            }
//                        }
//                    });
//                }
//            }
//        });
//        mCreateSoon.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (ContextCompat.checkSelfPermission(DebugActivity.this,
//                        android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
//                        PackageManager.PERMISSION_GRANTED ) {
//                    ActivityCompat.requestPermissions(DebugActivity.this,
//                            new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
//                            0);
//                } else {
//                    LocationServices.getFusedLocationProviderClient(DebugActivity.this).getLastLocation()
//                            .addOnSuccessListener(new OnSuccessListener<Location>() {
//                        @Override
//                        public void onSuccess(Location location) {
//                            if (location != null) {
//                                if (createLimit(location, false)) {
//                                    createWatchZone(location);
//                                }
//                            }
//                        }
//                    });
//                }
//            }
//        });
//        LongPreferenceLiveData alarmScheduledFor = new LongPreferenceLiveData(this,
//                Preferences.PREFERENCE_ALARM_SCHEDULED_FOR);
//        alarmScheduledFor.observe(this, new Observer<Long>() {
//            @Override
//            public void onChanged(@Nullable Long timestamp) {
//                if (timestamp != null) {
//                    if (timestamp == 0L) {
//                        mAlarmScheduledFor.setText("never");
//                    } else {
//                        mAlarmScheduledFor.setText(new Date(timestamp).toString());
//                    }
//                }
//            }
//        });
    }

//    private fun createWatchZone(location: Location) {
//        val watchZoneDao = AppDatabase.getInstance(this).watchZoneDao()
//        WatchZoneModelRepository.getInstance(this).createWatchZone("Test Zone",
//                location.latitude, location.longitude, 50)
//        AppUpdateJob.scheduleJob(this)
//    }
//
//    private fun createLimit(location: Location, active: Boolean): Boolean {
//        var limitCreated = false
//        val limitDao = AppDatabase.getInstance(this).limitDao()
//        val latLng = LatLng(location.latitude, location.longitude)
//        val address = LocationUtils.getAddressForLatLnt(this, latLng)
//        if (address != null && !TextUtils.isEmpty(address)) {
//            val now = GregorianCalendar()
//            if (!active) {
//                now.add(Calendar.MINUTE, 2)
//            }
//            val end = GregorianCalendar()
//            if (!active) {
//                end.add(Calendar.MINUTE, 2)
//            }
//            end.add(Calendar.HOUR, 1)
//            val split = address.split(",").toTypedArray()
//            if (split.size > 1) {
//                val streetAddress = split[0]
//                val streetAddressParsings = streetAddress.split(" ").toTypedArray()
//                if (streetAddressParsings.size > 1) {
//                    val streetNumber = streetAddressParsings[0]
//                    var streetName = ""
//                    for (j in 1 until streetAddressParsings.size) {
//                        streetName += " " + streetAddressParsings[j]
//                    }
//                    streetName = streetName.trim { it <= ' ' }
//                    if (streetNumber.contains("-")) {
//                        val streetNumberParsings = streetNumber.split("-").toTypedArray()
//                        if (streetNumberParsings.size == 2) {
//                            try {
//                                val minNum = streetNumberParsings[0].toInt()
//                                val limit = Limit()
//                                limit.addressValidatedTimestamp = System.currentTimeMillis()
//                                limit.endRange = minNum + 100
//                                limit.startRange = 0
//                                limit.street = streetName
//                                limit.isPosted = true
//                                limit.rawLimitString = "some garbage"
//                                val uid = limitDao.insertLimit(limit)
//                                limit.uid = uid
//                                val schedule = LimitSchedule()
//                                schedule.weekNumber = now[Calendar.WEEK_OF_MONTH]
//                                schedule.dayNumber = now[Calendar.DAY_OF_WEEK]
//                                schedule.startHour = now[Calendar.HOUR_OF_DAY]
//                                schedule.startMinute = now[Calendar.MINUTE]
//                                schedule.endHour = end[Calendar.HOUR_OF_DAY]
//                                schedule.endMinute = end[Calendar.MINUTE]
//                                schedule.limitId = uid
//                                limitDao.insertLimitSchedule(schedule)
//                                limitCreated = true
//                            } catch (e: NumberFormatException) {
//                            }
//                        }
//                    } else {
//                        try {
//                            val num = streetNumber.toInt()
//                            val limit = Limit()
//                            limit.addressValidatedTimestamp = System.currentTimeMillis()
//                            limit.endRange = num + 100
//                            limit.startRange = 0
//                            limit.street = streetName
//                            limit.isPosted = true
//                            limit.rawLimitString = "some garbage"
//                            val uid = limitDao.insertLimit(limit)
//                            limit.uid = uid
//                            val schedule = LimitSchedule()
//                            schedule.weekNumber = now[Calendar.WEEK_OF_MONTH]
//                            schedule.dayNumber = now[Calendar.DAY_OF_WEEK]
//                            schedule.startHour = now[Calendar.HOUR_OF_DAY]
//                            schedule.startMinute = now[Calendar.MINUTE]
//                            schedule.endHour = end[Calendar.HOUR_OF_DAY]
//                            schedule.endMinute = end[Calendar.MINUTE]
//                            schedule.limitId = uid
//                            limitDao.insertLimitSchedule(schedule)
//                            limitCreated = true
//                        } catch (e: NumberFormatException) {
//                        }
//                    }
//                }
//            }
//        }
//        if (!limitCreated) {
//            Toast.makeText(this, "Failed to make limit for current location.", Toast.LENGTH_SHORT).show()
//        }
//        return limitCreated
//    }
}