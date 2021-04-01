package com.example.joseph.sweepersd.archived;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.joseph.sweepersd.archived.model.watchzone.SweepingAddress;
import com.example.joseph.sweepersd.experimental.ActivityDetectionService;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.hutchins.tbd.R;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class DebugActivity extends AppCompatActivity
        implements SweeperService.SweeperServiceListener  {
    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean mIsBound = false;

    private Button mLaunchMapsButton;
    private Button mSendParkedActivity;
    private Button mSendDrivingActivity;
    private TextView mVehicleText;
    private TextView mBicycleText;
    private TextView mWalkingText;
    private TextView mRunningText;
    private TextView mOnFootText;
    private TextView mStillText;
    private TextView mTiltingText;
    private TextView mUnknownText;
    private TextView mStatusText;
    private TextView mServiceStatusText;

    private TextView mAddresses;

    private Handler mActivityHandler;
    private Runnable mRunnable;

    private SweeperService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_advanced_settings, true);

        setContentView(R.layout.activity_debug);

        /*mLaunchMapsButton = (Button) findViewById(R.id.launch_maps);
        mSendParkedActivity = (Button) findViewById(R.id.sendParked);
        mSendDrivingActivity = (Button) findViewById(R.id.sendDriving);
        mVehicleText = (TextView) findViewById(R.id.vehicleConfidenceText);
        mBicycleText = (TextView) findViewById(R.id.bicycleConfidenceText);
        mWalkingText = (TextView) findViewById(R.id.walkingConfidenceText);
        mRunningText = (TextView) findViewById(R.id.runningConfidenceText);
        mOnFootText = (TextView) findViewById(R.id.footConfidenceText);
        mStillText = (TextView) findViewById(R.id.stillConfidenceText);
        mTiltingText = (TextView) findViewById(R.id.tiltingConfidenceText);
        mUnknownText = (TextView) findViewById(R.id.unknownConfidenceText);
        mStatusText = (TextView) findViewById(R.id.statusText);
        mServiceStatusText = (TextView) findViewById(R.id.serviceStatusText);
        mAddresses = (TextView) findViewById(R.id.addresses);*/

        mLaunchMapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsBound) {

                }
                // TODO what if we're not bound?
            }
        });

        mSendParkedActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSimulatedData(0);
            }
        });
        mSendDrivingActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSimulatedData(100);
            }
        });

        Intent serviceIntent = new Intent(this, SweeperService.class);
        startService(serviceIntent);

        mActivityHandler = new Handler(getMainLooper());
        mRunnable = new UpdateActivityRunnable();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, SweeperService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mIsBound) {
            unbindService(mServiceConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mActivityHandler.postDelayed(mRunnable, 33);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActivityHandler.removeCallbacks(mRunnable);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service is connected!");
            mService = ((SweeperService.SweeperBinder)service).getService(DebugActivity.this);
            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
        }
    };

    private void sendSimulatedDataDelayed(final int vehicleConf, int delay) {
        mActivityHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendSimulatedData(vehicleConf);
            }
        }, delay);
    }

    private void sendSimulatedData(int vehicleConf) {
        List<DetectedActivity> detectedActivities = new ArrayList<>();
        detectedActivities.add(new DetectedActivity(DetectedActivity.IN_VEHICLE, vehicleConf));
        ActivityRecognitionResult result = new ActivityRecognitionResult(detectedActivities,
                System.currentTimeMillis(), SystemClock.elapsedRealtime());
        Bundle bundle = new Bundle();
        //bundle.putParcelable(ParkDetectionManager.ACTIVITY_RECOGNITION_RESULT, result);
        //bundle.putBoolean(ParkDetectionManager.ACTIVITY_RECOGNITION_CLEAR, true);

        Intent intent = new Intent(ActivityDetectionService.ACTION_ACTIVITY_UPDATE);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    private class UpdateActivityRunnable implements Runnable {
        @Override
        public void run() {
            if (mIsBound) {
                /*mVehicleText.setText("" + mService.getConfidence(ParkDetectionManager.CONFIDENCE_VEHICLE));
                mBicycleText.setText("" + mService.getConfidence(ParkDetectionManager.CONFIDENCE_BICYCLE));
                mWalkingText.setText("" + mService.getConfidence(ParkDetectionManager.CONFIDENCE_WALKING));
                mRunningText.setText("" + mService.getConfidence(ParkDetectionManager.CONFIDENCE_RUNNING));
                mOnFootText.setText("" + mService.getConfidence(ParkDetectionManager.CONFIDENCE_FOOT));
                mStillText.setText("" + mService.getConfidence(ParkDetectionManager.CONFIDENCE_STILL));
                mTiltingText.setText("" + mService.getConfidence(ParkDetectionManager.CONFIDENCE_TILTING));
                mUnknownText.setText("" + mService.getConfidence(ParkDetectionManager.CONFIDENCE_UNKNOWN));
                mStatusText.setText("" + mService.getParkDetectionStatus().toString());
                mServiceStatusText.setText("" + (mService.isDriving() ? "Driving" : "Parked"));*/

                String a = "";
                /*for (Address address : mService.getCurrentAddresses()) {
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        a += address.getAddressLine(i) + ", ";
                    }
                    a += "\n";
                    String street = address.getThoroughfare();
                    String housenumber = address.getFeatureName();
                    String city = address.getLocality();
                }
                a += "\n";
                String schedule = mService.getCurrentParkedSchedule();
                if (schedule != null) {
                    a += schedule;
                }
                a += "\n";
                for (Address address : mService.getParkingAddressesForLimit()) {
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        a += address.getAddressLine(i) + ", ";
                    }
                    a += "\n";
                    String street = address.getThoroughfare();
                    String housenumber = address.getFeatureName();
                    String city = address.getLocality();
                }*/
                mAddresses.setText(a);
            }
            mActivityHandler.postDelayed(mRunnable, 33);
        }
    }

    @Override
    public void onGooglePlayConnectionStatusUpdated(SweeperService.GooglePlayConnectionStatus status) {

    }

    @Override
    public void onParked(List<SweepingAddress> results) {

    }

    @Override
    public void onDriving() {

    }
}
