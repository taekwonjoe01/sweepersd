package com.example.joseph.sweepersd.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.joseph.sweepersd.SettingsActivity;

/**
 * Created by joseph on 4/7/16.
 */
public class SettingsUtils {
    public static long getRedzoneLimit(Context context) {
        return Long.parseLong(
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                        SettingsActivity.PREF_KEY_REDZONE_WARNING_TIME, "64800000"));
    }
    public static boolean isParkedNotificationEnabled(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(
                SettingsActivity.PREF_KEY_RECEIVE_PARK_NOTIFICATIONS, false);
    }
}
