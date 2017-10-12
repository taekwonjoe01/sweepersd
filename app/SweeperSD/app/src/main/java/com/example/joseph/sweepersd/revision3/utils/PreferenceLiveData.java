package com.example.joseph.sweepersd.revision3.utils;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public abstract class PreferenceLiveData<T> extends LiveData<T> {
    private final Context mContext;
    private final String mSharedPreferenceKey;

    private SharedPreferences mSharedPreferences;

    private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferencesListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            mSharedPreferences = sharedPreferences;
            if (mSharedPreferenceKey.equals(s)) {
                postValue(getValueFromPreferences(mSharedPreferences, mSharedPreferenceKey));
            }
        }
    };

    public abstract T getValueFromPreferences(SharedPreferences sharedPreferences, String key);

    public PreferenceLiveData(Context context, String key) {
        mContext = context;
        mSharedPreferenceKey = key;
    }

    @Override
    protected void onActive() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferencesListener);
        setValue(getValueFromPreferences(mSharedPreferences, mSharedPreferenceKey));
    }

    @Override
    protected void onInactive() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferencesListener);
    }
}
