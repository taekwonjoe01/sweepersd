package com.example.joseph.sweepersd.utils;

import androidx.lifecycle.Observer;
import androidx.annotation.Nullable;

public abstract class BaseObserver<T, U> implements Observer<U> {
    private final BaseObserverCallback mCallback;
    private boolean mIsLoaded;

    public abstract boolean isValid(U data);
    //public abstract boolean isDataLoaded(U data);
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

    @Override
    public void onChanged(@Nullable U data) {
        if (!isValid(data)) {
            mCallback.onDataInvalid();
        } else {
            if (!mIsLoaded) {
                mIsLoaded = true;//isDataLoaded(data);
                if (mIsLoaded) {
                    mCallback.onDataLoaded(getData(data));
                }
            } else if (mIsLoaded) {
                onPossibleChangeDetected(getData(data));
            }
        }
    }

    public boolean isLoaded() {
        return mIsLoaded;
    }
}