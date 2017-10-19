package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

/**
 * Created by joseph on 10/18/17.
 */

public abstract class WatchZoneModelObserver<T> implements Observer<WatchZoneModelRepository> {
    private final WatchZoneModelObserverCallback mCallback;
    private boolean mIsLoaded;

    abstract boolean isValid(WatchZoneModelRepository watchZoneModelRepository);
    abstract void onRepositoryChanged(T data);
    abstract T getDataFromRepo(WatchZoneModelRepository watchZoneModelRepository);

    public WatchZoneModelObserver(WatchZoneModelObserverCallback callback) {
        mCallback = callback;
        mIsLoaded = false;
    }

    public interface WatchZoneModelObserverCallback<T> {
        void onDataLoaded(T data);
        void onDataInvalid();
    }

    @Override
    public void onChanged(@Nullable WatchZoneModelRepository watchZoneModelRepository) {
        if (!isValid(watchZoneModelRepository)) {
            mCallback.onDataInvalid();
        }
        if (!mIsLoaded) {
            T data = getDataFromRepo(watchZoneModelRepository);
            if (data != null) {
                mIsLoaded = true;
                mCallback.onDataLoaded(data);
            }
        } else if (mIsLoaded) {
            T data = getDataFromRepo(watchZoneModelRepository);
            if (data == null) {
                mCallback.onDataInvalid();
            } else {
                onRepositoryChanged(getDataFromRepo(watchZoneModelRepository));
            }
        }
    }

    public boolean isLoaded() {
        return mIsLoaded;
    }
}