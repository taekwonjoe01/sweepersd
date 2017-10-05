package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.example.joseph.sweepersd.revision3.limit.Limit;
import com.example.joseph.sweepersd.revision3.limit.LimitRepository;

public class WatchZoneLimitModel extends LiveData<WatchZoneLimitModel> {
    private final Context mApplicationContext;
    private final Handler mHandler;
    private final Long mLimitUid;
    private final WatchZoneLimitSchedulesModel mSchedulesModel;

    private Limit mLimit;

    private Observer<Limit> mLimitDatabaseObserver = new Observer<Limit>() {
        @Override
        public void onChanged(@Nullable final Limit limit) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZoneLimitModel.this) {
                        if (limit == null) {
                            // Invalid value for this LiveData. Notify observers by setting self to
                            // null.
                            postValue(null);
                        } else {
                            if (mLimit == null) {
                                mSchedulesModel.observeForever(mSchedulesObserver);
                            }
                            mLimit = limit;
                        }
                    }
                }
            });
        }
    };

    private Observer<WatchZoneLimitSchedulesModel> mSchedulesObserver = new Observer<WatchZoneLimitSchedulesModel>() {
        @Override
        public void onChanged(@Nullable final WatchZoneLimitSchedulesModel limitSchedulesModel) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZoneLimitModel.this) {
                        if (limitSchedulesModel == null) {
                            // Invalid value for this LiveData. Notify observers by setting self to
                            // null.
                            postValue(null);
                        } else {
                            postValue(WatchZoneLimitModel.this);
                        }
                    }
                }
            });
        }
    };

    public WatchZoneLimitModel(Context context, Handler handler, Long limitUid) {
        mApplicationContext = context.getApplicationContext();
        mHandler = handler;
        mLimitUid = limitUid;
        mSchedulesModel = new WatchZoneLimitSchedulesModel(mApplicationContext, mHandler, mLimitUid);
    }

    public synchronized Long getLimitUid() {
         return mLimitUid;
    }

    public synchronized Limit getLimit() {
        return getValue() == null ? null : mLimit;
    }

    public synchronized WatchZoneLimitSchedulesModel getLimitSchedulesModel() {
        return getValue() == null ? null : mSchedulesModel.getValue();
    }

    public synchronized boolean isChanged(WatchZoneLimitModel compareTo) {
        boolean result = false;

        if (this.mLimitUid == compareTo.getLimitUid()) {
            if (this.mLimit == null && compareTo.getLimit() != null) {
                result = true;
            } else if (this.mLimit != null && compareTo.getLimit() == null) {
                result = true;
            } else if (this.mLimit.isChanged(compareTo.getLimit())) {
                result = true;
            } else if (this.mSchedulesModel.isChanged(compareTo.getLimitSchedulesModel())) {
                result = true;
            }
        }

        return result;
    }

    @Override
    protected synchronized void onActive() {
        super.onActive();
        LimitRepository.getInstance(mApplicationContext).getLimitLiveData(mLimitUid)
                .observeForever(mLimitDatabaseObserver);
        if (mLimit != null) {
            mSchedulesModel.observeForever(mSchedulesObserver);
        }
    }

    @Override
    protected synchronized void onInactive() {
        super.onInactive();
        LimitRepository.getInstance(mApplicationContext).getLimitLiveData(mLimitUid)
                .removeObserver(mLimitDatabaseObserver);
        mSchedulesModel.removeObserver(mSchedulesObserver);
    }
}
