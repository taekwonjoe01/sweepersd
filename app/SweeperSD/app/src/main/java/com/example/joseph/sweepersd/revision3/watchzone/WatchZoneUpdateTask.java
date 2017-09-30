package com.example.joseph.sweepersd.revision3.watchzone;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import com.example.joseph.sweepersd.revision3.LocationUtils;
import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class WatchZoneUpdateTask implements
        WatchZoneUpdater.Listener,
        WatchZoneUpdater.AddressProvider {
    public static final String ACTION_WATCH_ZONE_UPDATE_PROGRESS =
            "com.example.joseph.sweepersd.ACTION_WATCH_ZONE_UPDATE_PROGRESS";
    public static final String ACTION_WATCH_ZONE_UPDATE_COMPLETE =
            "com.example.joseph.sweepersd.ACTION_WATCH_ZONE_UPDATE_COMPLETE";
    public static final String PARAM_PROGRESS = "PARAM_PROGRESS";
    public static final String PARAM_WATCH_ZONE_ID = "PARAM_WATCH_ZONE_ID";

    private final Context mContext;
    private final long mWatchZoneUid;
    private final List<HandlerThread> mThreads;

    private WatchZoneUpdater mWatchZoneUpdater;

    public interface Listener {
        void on
    }

    public WatchZoneUpdateTask(Context context, long watchZoneUid) {
        mContext = context;
        mWatchZoneUid = watchZoneUid;
        mThreads = new ArrayList<>();
    }

    @Override
    public void onProgress(int progress) {
        publishProgress(progress);
    }

    @Override
    public void onUpdateComplete(WatchZoneUpdater.Result result) {
        for (HandlerThread thread : mThreads) {
            thread.quit();
        }

        switch (result) {
            case CORRUPT:
            case NETWORK_ERROR:
            case CANCELLED:
            case SUCCESS:
                default:
        }
        publishFinished();
    }

    @Override
    public String getAddressForLatLng(LatLng latLng) {
        return LocationUtils.getAddressForLatLnt(mContext, latLng);
    }

    public void execute() {
        int numThreads = Runtime.getRuntime().availableProcessors() * 2;
        List<Handler> handlers = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            HandlerThread thread = new HandlerThread("WatchZoneUpdateThread" + (i+1));
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            mThreads.add(thread);
            handlers.add(handler);
        }

        mWatchZoneUpdater = new WatchZoneUpdater(this, mWatchZoneUid, handlers,
                WatchZoneRepository.getInstance(mContext), LimitRepository.getInstance(mContext),
                this);
        // Blocks until finished.
        mWatchZoneUpdater.execute();
    }

    public void cancel() {
        mWatchZoneUpdater.cancel();
    }

    private void publishProgress(int progress) {
        Bundle bundle = new Bundle();
        bundle.putInt(PARAM_PROGRESS, progress);

        sendBroadcast(bundle, ACTION_WATCH_ZONE_UPDATE_PROGRESS);
    }

    private void publishFinished() {
        sendBroadcast(null, ACTION_WATCH_ZONE_UPDATE_COMPLETE);
    }

    private void sendBroadcast(Bundle bundle, String action) {
        bundle.putLong(PARAM_WATCH_ZONE_ID, mWatchZoneUid);

        Intent intent = new Intent(action);
        intent.putExtras(bundle);
        mContext.sendBroadcast(intent);
    }
}
