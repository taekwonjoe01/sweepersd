package com.example.joseph.sweepersd.revision3.utils;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPreferencesLiveData extends LiveData<SharedPreferences> {
    private final Context mContext;

    private SharedPreferences mSharedPreferences;

    private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferencesListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            mSharedPreferences = sharedPreferences;
            postValue(mSharedPreferences);
        }
    };

    public SharedPreferencesLiveData(Context context) {
        mContext = context;
    }

    @Override
    protected void onActive() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferencesListener);
        setValue(mSharedPreferences);
    }

    @Override
    protected void onInactive() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferencesListener);
    }
}
