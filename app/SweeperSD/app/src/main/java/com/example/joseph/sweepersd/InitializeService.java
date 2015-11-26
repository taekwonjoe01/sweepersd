package com.example.joseph.sweepersd;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

public class InitializeService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = InitializeService.class.getSimpleName();

    private GoogleApiClient mClient;

    private boolean mIsConnected = false;

    public InitializeService() {
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsConnected) {
            if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
                    == ConnectionResult.SUCCESS) {
                mClient = new GoogleApiClient.Builder(this)
                        .addApi(ActivityRecognition.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
                mClient.connect();
            }
        }
        // TODO: What if google play services is not available?
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        mIsConnected = true;
        Intent intent = new Intent(this, ActivityDetectionService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient, 0,
                pendingIntent);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
        mIsConnected = false;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
        mIsConnected = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
