package com.example.joseph.sweepersd.limit;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.arch.lifecycle.Observer;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.joseph.sweepersd.AppDatabase;
import com.example.joseph.sweepersd.utils.Jobs;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.example.joseph.sweepersd.utils.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by josephhutchins on 10/13/17.
 */

public class AddressValidatorJob extends JobService {
    private static final String TAG = AddressValidatorJob.class.getSimpleName();
    private static final long ONE_HOUR = 1000L * 60L * 60L;
    private static final long TEN_SECONDS = 1000L * 10L;

    private Handler mMainHandler;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private JobParameters mJobParameters;

    private AtomicBoolean mIsCancelled;
    private CountDownLatch mFinishedLatch;

    public static void scheduleJob(Context context) {
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(Jobs.ADDRESS_VALIDATOR_JOB) != null) {
            return;
        }

        JobInfo.Builder builder = new JobInfo.Builder(Jobs.ADDRESS_VALIDATOR_JOB,
                new ComponentName(context, AddressValidatorJob.class));
        builder.setMinimumLatency(0L);
        builder.setOverrideDeadline(0L);
        builder.setPersisted(true);
        builder.setBackoffCriteria(TEN_SECONDS, JobInfo.BACKOFF_POLICY_LINEAR);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        jobScheduler.schedule(builder.build());
    }

    public static void scheduleMonthlyJob(Context context) {
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(Jobs.ADDRESS_VALIDATOR_MONTHLY_JOB) != null) {
            return;
        }

        JobInfo.Builder builder = new JobInfo.Builder(Jobs.ADDRESS_VALIDATOR_MONTHLY_JOB,
                new ComponentName(context, AddressValidatorJob.class));
        builder.setPeriodic(ONE_MONTH);
        builder.setPersisted(true);
        builder.setBackoffCriteria(ONE_HOUR, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Starting " + TAG);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putLong(Preferences.PREFERENCE_ADDRESS_VALIDATOR_LAST_STARTED, System.currentTimeMillis()).commit();

        AddressValidatorManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                if (!working) {
                    preferences.edit().putLong(Preferences.PREFERENCE_ADDRESS_VALIDATOR_LAST_FINISHED,
                            System.currentTimeMillis()).commit();
                    preferences.edit().putBoolean(Preferences.PREFERENCE_ON_DEVICE_LIMITS_VALIDATED,
                            true).commit();
                }
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Interrupting " + TAG);
        mIsCancelled.set(true);
        try {
            mFinishedLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBackgroundThread.quit();
        return true;
    }
}
