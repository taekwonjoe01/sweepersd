package com.example.joseph.sweepersd.limit;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.example.joseph.sweepersd.AppDatabase;
import com.example.joseph.sweepersd.utils.Jobs;
import com.example.joseph.sweepersd.utils.LocationUtils;

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
    private static final long ONE_MONTH = 1000L * 60L * 60L * 24L * 30L;
    private static final long ONE_HOUR = 1000L * 60L * 60L;

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
        builder.setPeriodic(ONE_MONTH);
        builder.setPersisted(true);
        builder.setBackoffCriteria(ONE_HOUR, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Starting " + TAG);
        mFinishedLatch = new CountDownLatch(1);

        mMainHandler = new Handler(Looper.getMainLooper());

        mBackgroundThread = new HandlerThread("AddressValidatorThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mIsCancelled = new AtomicBoolean(false);
        mJobParameters = jobParameters;

        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                LimitDao limitDao = AppDatabase.getInstance(AddressValidatorJob.this).limitDao();

                Map<String, String> foundAddresses = new HashMap<>();

                List<Limit> limits = limitDao.getAllLimits();
                List<Limit> updatedLimits = new ArrayList<>();
                for (Limit limit : limits) {
                    if (mIsCancelled.get()) {
                        break;
                    }
                    long timePassed = System.currentTimeMillis() - limit.getAddressValidatedTimestamp();
                    if (timePassed > ONE_MONTH) {
                        String streetBeingValidated = limit.getStreet();
                        String validatedAddress = null;
                        if (foundAddresses.containsKey(streetBeingValidated)) {
                            validatedAddress = foundAddresses.get(streetBeingValidated);
                        } else {
                            validatedAddress = LocationUtils.validateStreet(
                                    AddressValidatorJob.this, streetBeingValidated);
                        }

                        if (validatedAddress != null) {
                            String[] parsings = validatedAddress.split(",");
                            if (parsings.length > 0) {
                                String validatedStreet = parsings[0].trim();
                                limit.setStreet(validatedStreet);

                                updatedLimits.add(limit);
                                foundAddresses.put(streetBeingValidated, validatedStreet);
                            }

                            Log.d(TAG, "Validated address " + validatedAddress);
                        }
                    }
                }

                long timestamp = System.currentTimeMillis();
                for (Limit updatedLimit : updatedLimits) {
                    updatedLimit.setAddressValidatedTimestamp(timestamp);
                }
                limitDao.updateLimits(updatedLimits);
                Log.d(TAG, "Updated limit database.");

                if (!mIsCancelled.get()) {
                    postFinished();
                }
                mFinishedLatch.countDown();
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

    private void postFinished() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mIsCancelled.get()) {
                    Log.d(TAG, "Finishing " + TAG);
                    jobFinished(mJobParameters, false);

                    mBackgroundThread.quit();
                }
            }
        });
    }
}
