package com.example.joseph.sweepersd;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean mIsBound = false;

    private Button mLaunchMapsButton;
    private Button mSimulateButton;
    private TextView mVehicleText;
    private TextView mBicycleText;
    private TextView mWalkingText;
    private TextView mRunningText;
    private TextView mOnFootText;
    private TextView mStillText;
    private TextView mTiltingText;
    private TextView mUnknownText;
    private TextView mIsParkedText;
    private TextView mIsDrivingText;

    private TextView mAddresses;

    private Handler mActivityHandler;
    private Runnable mRunnable;

    private SweeperService.SweeperBinder mServiceBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLaunchMapsButton = (Button) findViewById(R.id.launch_maps);
        mSimulateButton = (Button) findViewById(R.id.simulate);
        mVehicleText = (TextView) findViewById(R.id.vehicleConfidenceText);
        mBicycleText = (TextView) findViewById(R.id.bicycleConfidenceText);
        mWalkingText = (TextView) findViewById(R.id.walkingConfidenceText);
        mRunningText = (TextView) findViewById(R.id.runningConfidenceText);
        mOnFootText = (TextView) findViewById(R.id.footConfidenceText);
        mStillText = (TextView) findViewById(R.id.stillConfidenceText);
        mTiltingText = (TextView) findViewById(R.id.tiltingConfidenceText);
        mUnknownText = (TextView) findViewById(R.id.unknownConfidenceText);
        mIsParkedText = (TextView) findViewById(R.id.isParkedText);
        mIsDrivingText = (TextView) findViewById(R.id.isDrivingText);
        mAddresses = (TextView) findViewById(R.id.addresses);

        mLaunchMapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsBound) {
                    Intent activityIntent = new Intent(MainActivity.this, MapsActivity.class);
                    activityIntent.putExtra("location", mServiceBinder.getLastKnownParkingLocation());
                    startActivity(activityIntent);
                }
                // TODO what if we're not bound?
            }
        });

        mSimulateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivityHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendSimulatedData(100);
                        sendSimulatedData(10);
                    }
                }, 5000);
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
            mServiceBinder = (SweeperService.SweeperBinder) service;
            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
        }
    };

    private void sendSimulatedData(int vehicleConf) {
        Bundle bundle = new Bundle();
        bundle.putInt(SweeperService.CONFIDENCE_BICYCLE, 0);
        bundle.putInt(SweeperService.CONFIDENCE_VEHICLE, vehicleConf);
        bundle.putInt(SweeperService.CONFIDENCE_WALKING, 0);
        bundle.putInt(SweeperService.CONFIDENCE_FOOT, 0);
        bundle.putInt(SweeperService.CONFIDENCE_RUNNING, 0);
        bundle.putInt(SweeperService.CONFIDENCE_STILL, 0);
        bundle.putInt(SweeperService.CONFIDENCE_UNKNOWN, 0);
        bundle.putInt(SweeperService.CONFIDENCE_TILTING, 0);

        Intent intent = new Intent(ActivityDetectionService.ACTION_ACTIVITY_UPDATE);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    private class UpdateActivityRunnable implements Runnable {
        @Override
        public void run() {
            if (mIsBound) {
                mVehicleText.setText("" + mServiceBinder.getConfidence(SweeperService.CONFIDENCE_VEHICLE));
                mBicycleText.setText("" + mServiceBinder.getConfidence(SweeperService.CONFIDENCE_BICYCLE));
                mWalkingText.setText("" + mServiceBinder.getConfidence(SweeperService.CONFIDENCE_WALKING));
                mRunningText.setText("" + mServiceBinder.getConfidence(SweeperService.CONFIDENCE_RUNNING));
                mOnFootText.setText("" + mServiceBinder.getConfidence(SweeperService.CONFIDENCE_FOOT));
                mStillText.setText("" + mServiceBinder.getConfidence(SweeperService.CONFIDENCE_STILL));
                mTiltingText.setText("" + mServiceBinder.getConfidence(SweeperService.CONFIDENCE_TILTING));
                mUnknownText.setText("" + mServiceBinder.getConfidence(SweeperService.CONFIDENCE_UNKNOWN));
                mIsParkedText.setText("" + mServiceBinder.isParked());
                mIsDrivingText.setText("" + mServiceBinder.isDriving());

                String a = "";
                for (Address address : mServiceBinder.getLastKnownParkingAddresses()) {
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        a += address.getAddressLine(i) + ", ";
                    }
                    a += "\n";
                    Log.d(TAG, address.toString());
                    String street = address.getThoroughfare();
                    String housenumber = address.getFeatureName();
                    String city = address.getLocality();
                }
                mAddresses.setText(a);
            }
            mActivityHandler.postDelayed(mRunnable, 33);
        }
    }
}
