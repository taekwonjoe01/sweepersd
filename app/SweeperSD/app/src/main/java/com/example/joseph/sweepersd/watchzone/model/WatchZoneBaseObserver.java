package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import java.util.List;

public abstract class WatchZoneBaseObserver<T, U> implements Observer<U> {
    private final WatchZoneBaseObserverCallback mCallback;
    private boolean mIsLoaded;

    abstract boolean isValid(U data);
    abstract void onPossibleChangeDetected(T data);
    abstract T getData(U data);

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
    public void onChanged(@Nullable U data) {
        if (!isValid(data)) {
            mCallback.onDataInvalid();
        }
        if (!mIsLoaded) {
            mIsLoaded = true;
            mCallback.onDataLoaded(getData(data));
        } else if (mIsLoaded) {
            onPossibleChangeDetected(getData(data));
        }
    }

    public boolean isLoaded() {
        return mIsLoaded;
    }
}