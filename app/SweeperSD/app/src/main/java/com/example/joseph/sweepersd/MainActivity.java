package com.example.joseph.sweepersd;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Button mLaunchMapsButton;
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

    private Handler mActivityHandler;
    private Runnable mRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLaunchMapsButton = (Button) findViewById(R.id.launch_maps);
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

        mLaunchMapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activityIntent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(activityIntent);
            }
        });

        Intent serviceIntent = new Intent(this, InitializeService.class);
        startService(serviceIntent);

        mActivityHandler = new Handler(getMainLooper());
        mRunnable = new UpdateActivityRunnable();
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

    private class UpdateActivityRunnable implements Runnable {
        @Override
        public void run() {
            mVehicleText.setText("" + SweeperSDApplication.getVehicleConfidence());
            mBicycleText.setText("" + SweeperSDApplication.getBicycleConfidence());
            mWalkingText.setText("" + SweeperSDApplication.getWalkingConfidence());
            mRunningText.setText("" + SweeperSDApplication.getRunningConfidence());
            mOnFootText.setText("" + SweeperSDApplication.getFootConfidence());
            mStillText.setText("" + SweeperSDApplication.getStillConfidence());
            mTiltingText.setText("" + SweeperSDApplication.getTiltingConfidence());
            mUnknownText.setText("" + SweeperSDApplication.getUnknownConfidence());
            mIsParkedText.setText("" + SweeperSDApplication.isParkingDetected());
            mIsDrivingText.setText("" + SweeperSDApplication.isDriving());

            mActivityHandler.postDelayed(mRunnable, 33);
        }
    }
}
