package com.example.joseph.sweepersd.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class BooleanPreferenceLiveData extends PreferenceLiveData<Boolean> {

    public BooleanPreferenceLiveData(Context context, String key) {
        super(context, key);
    }

    @Override
    public Boolean getValueFromPreferences(SharedPreferences sharedPreferences, String key) {
        return sharedPreferences.getBoolean(key, false);
    }
}
