package com.example.joseph.sweepersd.limit;

import com.example.joseph.sweepersd.utils.BaseObserver;

import java.util.List;

public class LimitModelObserver extends BaseObserver<List<LimitModel>, List<LimitModel>> {
    private List<LimitModel> mLimitModels;
    private final LimitModelCallback mCallback;

    public interface LimitModelCallback extends BaseObserverCallback<List<LimitModel>> {
        void onLimitModelsChanged(List<LimitModel> limitModels);
    }

    public LimitModelObserver(LimitModelCallback callback) {
        super(callback);
        mCallback = callback;
    }

    @Override
    public void onPossibleChangeDetected(List<LimitModel> data) {
        mCallback.onLimitModelsChanged(mLimitModels);
    }

    @Override
    public List<LimitModel> getData(List<LimitModel> data) {
        if (mLimitModels == null) {
            mLimitModels = data;
        }
        return data;
    }

    @Override
    public boolean isValid(List<LimitModel> data) {
        return data != null;
    }

    public List<LimitModel> getLimitModels() {
        return mLimitModels;
    }
}
