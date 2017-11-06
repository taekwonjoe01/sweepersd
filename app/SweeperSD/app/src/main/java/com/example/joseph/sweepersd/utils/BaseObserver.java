package com.example.joseph.sweepersd.utils;

import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseObserver<T, U> implements Observer<U> {
    private final BaseObserverCallback mCallback;
    private boolean mIsLoaded;

    public abstract boolean isValid(U data);
    public abstract void onPossibleChangeDetected(T data);
    public abstract T getData(U data);

    public BaseObserver(BaseObserverCallback callback) {
        mCallback = callback;
        mIsLoaded = false;
    }

    public interface BaseObserverCallback<T> {
        void onDataLoaded(T data);
        void onDataInvalid();
    }

    public static class ChangeSet {
        public List<Long> addedLimits;
        public List<Long> changedLimits;
        public List<Long> removedLimits;

        public ChangeSet() {
            addedLimits = new ArrayList<>();
            changedLimits = new ArrayList<>();
            removedLimits = new ArrayList<>();
        }
    }

    @Override
    public void onChanged(@Nullable U data) {
        if (!isValid(data)) {
            mCallback.onDataInvalid();
        } else {
            if (!mIsLoaded) {
                mIsLoaded = true;
                mCallback.onDataLoaded(getData(data));
            } else if (mIsLoaded) {
                onPossibleChangeDetected(getData(data));
            }
        }
    }

    public boolean isLoaded() {
        return mIsLoaded;
    }
}