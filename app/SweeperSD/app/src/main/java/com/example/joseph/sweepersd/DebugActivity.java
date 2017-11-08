package com.example.joseph.sweepersd;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.joseph.sweepersd.utils.LongPreferenceLiveData;
import com.example.joseph.sweepersd.utils.Preferences;

import java.util.Date;

public class DebugActivity extends AppCompatActivity {
    private Button mScheduleAddressValidator;
    private TextView mAddressValidatorTimeStart;
    private TextView mAddressValidatorTimeFinish;
    private Button mScheduleUpdater;
    private TextView mWZUTimeStart;
    private TextView mWZUTimeFinish;
    private TextView mWZNTimeStart;
    private TextView mWZNTimeFinish;
    private TextView mSchedTimeStart;
    private TextView mSchedTimeFinish;

    private Button mLimitCacheClear;
    private TextView mLimitCacheSize;

    private Button mWZRCacheClear;
    private TextView mWZCacheSize;

    private Button mWZMRCacheClear;
    private TextView mWZMCacheSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        mScheduleAddressValidator = findViewById(R.id.button_schedule_validator);
        mAddressValidatorTimeStart = findViewById(R.id.textview_address_validator_started);
        mAddressValidatorTimeFinish = findViewById(R.id.textview_address_validator_finished);
        mScheduleUpdater = findViewById(R.id.button_schedule_updater);
        mWZUTimeStart = findViewById(R.id.textview_wzu_started);
        mWZUTimeFinish = findViewById(R.id.textview_wzu_finished);
        mWZNTimeStart = findViewById(R.id.textview_wzn_started);
        mWZNTimeFinish = findViewById(R.id.textview_wzn_finished);
        mSchedTimeStart = findViewById(R.id.textview_schedulejob_started);
        mSchedTimeFinish = findViewById(R.id.textview_schedulejob_finished);

        mLimitCacheClear = findViewById(R.id.button_limit_clear);
        mLimitCacheSize = findViewById(R.id.textview_limit_repo);

        mWZRCacheClear = findViewById(R.id.button_wz_clear);
        mWZCacheSize = findViewById(R.id.textview_wz_repo);

        mWZMRCacheClear = findViewById(R.id.button_wzm_clear);
        mWZMCacheSize = findViewById(R.id.textview_wzm_repo);

        mLimitCacheClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //LimitRepository.getInstance(DebugActivity.this).delete();
            }
        });
        /*LimitRepository.getInstanceLiveData().observe(this, new Observer<LimitRepository>() {
            @Override
            public void onChanged(@Nullable LimitRepository limitRepository) {
                if (limitRepository == null) {
                    mLimitCacheSize.setText("null");
                } else {
                    mLimitCacheSize.setText("allocated");
                }
            }
        });*/
        mLimitCacheSize.setText("TODO");
        mWZRCacheClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //WatchZoneRepository.getInstance(DebugActivity.this).delete();
            }
        });
        /*WatchZoneRepository.getInstanceLiveData().observe(this, new Observer<WatchZoneRepository>() {
            @Override
            public void onChanged(@Nullable WatchZoneRepository watchZoneRepository) {
                if (watchZoneRepository == null) {
                    mWZCacheSize.setText("null");
                } else {
                    mWZCacheSize.setText("allocated");
                }
            }
        });*/
        mWZCacheSize.setText("TODO");
        mWZMRCacheClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //WatchZoneModelRepository.getInstance(DebugActivity.this).delete();
            }
        });
        /*WatchZoneModelRepository.getInstanceLiveData().observe(this, new Observer<WatchZoneModelRepository>() {
            @Override
            public void onChanged(@Nullable WatchZoneModelRepository watchZoneModelRepository) {
                if (watchZoneModelRepository == null) {
                    mWZMCacheSize.setText("null");
                } else {
                    mWZMCacheSize.setText("allocated");
                }
            }
        });*/
        mWZMCacheSize.setText("TODO");

        mScheduleAddressValidator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*AddressValidatorJob.scheduleJob(DebugActivity.this);
                AddressValidatorJob.scheduleMonthlyJob(DebugActivity.this);*/
            }
        });

        LongPreferenceLiveData validatorStart = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_ADDRESS_VALIDATOR_LAST_STARTED);
        validatorStart.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mAddressValidatorTimeStart.setText("never");
                    } else {
                        mAddressValidatorTimeStart.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        LongPreferenceLiveData validatorFinish = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_ADDRESS_VALIDATOR_LAST_FINISHED);
        validatorFinish.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mAddressValidatorTimeFinish.setText("never");
                    } else {
                        mAddressValidatorTimeFinish.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        mScheduleUpdater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //WatchZoneUpdateJob.scheduleAppForegroundJob(DebugActivity.this);
            }
        });
        LongPreferenceLiveData updaterStart = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_WATCH_ZONE_UPDATE_LAST_STARTED);
        updaterStart.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mWZUTimeStart.setText("never");
                    } else {
                        mWZUTimeStart.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        LongPreferenceLiveData updaterFinish = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_WATCH_ZONE_UPDATE_LAST_FINISHED);
        updaterFinish.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mWZUTimeFinish.setText("never");
                    } else {
                        mWZUTimeFinish.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        LongPreferenceLiveData notifStarted = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_WATCH_ZONE_NOTIFICATION_LAST_STARTED);
        notifStarted.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mWZNTimeStart.setText("never");
                    } else {
                        mWZNTimeStart.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        LongPreferenceLiveData notifFinished = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_WATCH_ZONE_NOTIFICATION_LAST_FINISHED);
        notifFinished.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mWZNTimeFinish.setText("never");
                    } else {
                        mWZNTimeFinish.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        LongPreferenceLiveData schedStarted = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_SCHEDULE_JOB_LAST_STARTED);
        notifStarted.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mWZNTimeStart.setText("never");
                    } else {
                        mWZNTimeStart.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
        LongPreferenceLiveData schedFinished = new LongPreferenceLiveData(this,
                Preferences.PREFERENCE_SCHEDULE_JOB_LAST_FINISHED);
        notifFinished.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long timestamp) {
                if (timestamp != null) {
                    if (timestamp == 0L) {
                        mWZNTimeFinish.setText("never");
                    } else {
                        mWZNTimeFinish.setText(new Date(timestamp).toString());
                    }
                }
            }
        });
    }

}
