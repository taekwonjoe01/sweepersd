package com.example.joseph.sweepersd.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class LongPreferenceLiveData extends PreferenceLiveData<Long> {
    public LongPreferenceLiveData(Context context, String key) {
        super(context, key);
    }

    @Override
    public Long getValueFromPreferences(SharedPreferences sharedPreferences, String key) {
        return sharedPreferences.getLong(key, 0L);
    }
}
