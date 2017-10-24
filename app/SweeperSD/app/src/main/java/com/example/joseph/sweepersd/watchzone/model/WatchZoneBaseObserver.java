package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by joseph on 10/18/17.
 */

public abstract class WatchZoneBaseObserver<T> implements Observer<WatchZoneModelRepository> {
    private final WatchZoneBaseObserverCallback mCallback;
    private boolean mIsLoaded;

    abstract boolean isValid(WatchZoneModelRepository watchZoneModelRepository);
    abstract void onRepositoryChanged(T data);
    abstract T getDataFromRepo(WatchZoneModelRepository watchZoneModelRepository);

    public WatchZoneBaseObserver(WatchZoneBaseObserverCallback callback) {
        mCallback = callback;
        mIsLoaded = false;
    }

    public interface WatchZoneBaseObserverCallback<T> {
        void onDataLoaded(T data);
        void onDataInvalid();
    }

    public static class ChangeSet {
        public List<Long> addedLimits;
        public List<Long> changedLimits;
        public List<Long> removedLimits;
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