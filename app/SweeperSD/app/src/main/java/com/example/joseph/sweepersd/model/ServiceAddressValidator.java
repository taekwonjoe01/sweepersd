package com.example.joseph.sweepersd.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.example.joseph.sweepersd.model.watchzone.WatchZoneUpdateService;

/**
 * Created by joseph on 9/11/16.
 */
public class ServiceAddressValidator implements AddressValidatorManager.AddressValidator {
    public static final String TAG = ServiceAddressValidator.class.getSimpleName();

    private final Context mContext;
    private int mProgress = AddressValidatorManager.INVALID_PROGRESS;
    private AddressValidatorManager.ValidatorProgressListener mListener;

    public ServiceAddressValidator(Context context) {
        mContext = context;
    }

    @Override
    public void validateAddresses(AddressValidatorManager.ValidatorProgressListener listener) {
        mListener = listener;

        IntentFilter filter = new IntentFilter();
        filter.addAction(AddressValidatorService.ACTION_VALIDATOR_PROGRESS);
        filter.addAction(AddressValidatorService.ACTION_VALIDATOR_FINISHED);
        mContext.registerReceiver(mValidatorListener, filter);

        Intent msgIntent = new Intent(mContext, AddressValidatorService.class);
        mContext.startService(msgIntent);

        mProgress = 0;
    }

    @Override
    public int getProgress() {
        return mProgress;
    }

    private final BroadcastReceiver mValidatorListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case AddressValidatorService.ACTION_VALIDATOR_PROGRESS:
                    int progress = intent.getIntExtra(WatchZoneUpdateService.PARAM_PROGRESS, 0);
                    mProgress = progress;
                    mListener.onValidatorProgress(progress);
                    break;
                case AddressValidatorService.ACTION_VALIDATOR_FINISHED:
                    mListener.onValidatorComplete();
                    mProgress = AddressValidatorManager.INVALID_PROGRESS;
                    mContext.unregisterReceiver(this);
                    break;
                default:
            }
        }
    };
}
