package com.example.joseph.sweepersd.archived.model;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by joseph on 9/11/16.
 */
public class AddressValidatorManager {
    public static final int INVALID_PROGRESS = -1;
    private static AddressValidatorManager sInstance;

    private final Context mContext;
    private AddressValidator mAddressValidator;

    private Set<WeakReference<ValidatorProgressListener>> mListeners = new HashSet<>();

    public interface AddressValidator {
        void validateAddresses(ValidatorProgressListener listener);
        int getProgress();
    }

    public interface ValidatorProgressListener {
        void onValidatorProgress(int progress);
        void onValidatorComplete();
    }

    private AddressValidatorManager(Context context) {
        mContext = context;

        mAddressValidator = new ServiceAddressValidator(mContext);
    }

    /**
     * For test purposes only
     */
    public void setAddressValidator(AddressValidator validator) {
        if (validator == null) {
            mAddressValidator = new ServiceAddressValidator(mContext);
        } else {
            mAddressValidator = validator;
        }
    }

    public void addListener(ValidatorProgressListener listener) {
        if (listener != null) {
            mListeners.add(new WeakReference<>(listener));
        }
    }

    public void removeListener(ValidatorProgressListener listener) {
        if (listener != null) {
            WeakReference<ValidatorProgressListener> toRemove = null;
            for (WeakReference<ValidatorProgressListener> weakRef : mListeners) {
                ValidatorProgressListener curListener = weakRef.get();
                if (curListener == listener) {
                    toRemove = weakRef;
                    break;
                }
            }
            mListeners.remove(toRemove);
        }
    }

    public static AddressValidatorManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AddressValidatorManager(context.getApplicationContext());
        }
        return sInstance;
    }

    public boolean validateAddresses() {
        if (mAddressValidator.getProgress() != INVALID_PROGRESS) {
            return false;
        } else {
            mAddressValidator.validateAddresses(mProgressListener);
            return true;
        }
    }

    public int getValidationProgress() {
        return mAddressValidator.getProgress();
    }

    private void notifyProgress(int progress) {
        List<WeakReference> toRemove = new ArrayList<>();
        for (WeakReference<ValidatorProgressListener> weakRef : mListeners) {
            ValidatorProgressListener curListener = weakRef.get();
            if (curListener == null) {
                toRemove.add(weakRef);
            } else {
                curListener.onValidatorProgress(progress);
            }
        }

        for (WeakReference removeMe : toRemove) {
            mListeners.remove(removeMe);
        }

        mListeners.remove(toRemove);
    }

    private void notifyComplete() {
        List<WeakReference> toRemove = new ArrayList<>();
        for (WeakReference<ValidatorProgressListener> weakRef : mListeners) {
            ValidatorProgressListener curListener = weakRef.get();
            if (curListener == null) {
                toRemove.add(weakRef);
            } else {
                curListener.onValidatorComplete();
            }
        }

        for (WeakReference removeMe : toRemove) {
            mListeners.remove(removeMe);
        }

        mListeners.remove(toRemove);
    }

    private final ValidatorProgressListener mProgressListener = new ValidatorProgressListener() {
        @Override
        public void onValidatorProgress(int progress) {
            notifyProgress(progress);
        }

        @Override
        public void onValidatorComplete() {
            notifyComplete();
        }
    };
}
