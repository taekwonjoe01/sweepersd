package com.example.joseph.sweepersd;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity
        implements SweeperService.SweeperServiceListener {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    /*
    These need to be kept up to date with the pref_keys in strings.xml
     */
    public static final String PREF_KEY_DRIVING_DRIVING_THRESHOLD = "STATUS_DRIVING_DRIVING_THRESHOLD";
    public static final String PREF_KEY_DRIVING_PARKED_THRESHOLD = "STATUS_DRIVING_PARKED_THRESHOLD";
    public static final String PREF_KEY_DRIVING_DECIDING_DRIVING_THRESHOLD = "STATUS_DRIVING_DECIDING_DRIVING_THRESHOLD";
    public static final String PREF_KEY_DRIVING_DECIDING_PARKED_THRESHOLD = "STATUS_DRIVING_DECIDING_PARKED_THRESHOLD";
    public static final String PREF_KEY_AGE_VALID = "AGE_OF_VALID_STATUS";
    public static final String PREF_KEY_PARKED_DRIVING_THRESHOLD = "STATUS_PARKED_DRIVING_THRESHOLD";
    public static final String PREF_KEY_PARKED_PARKED_THRESHOLD = "STATUS_PARKED_PARKED_THRESHOLD";
    public static final String PREF_KEY_PARKED_DECIDING_DRIVING_THRESHOLD = "STATUS_PARKED_DECIDING_DRIVING_THRESHOLD";
    public static final String PREF_KEY_PARKED_DECIDING_PARKED_THRESHOLD = "STATUS_PARKED_DECIDING_PARKED_THRESHOLD";
    public static final String PREF_KEY_ACTIVITY_UPDATE_RATE = "ACTIVITY_UPDATE_PERIOD";

    public static final String PREF_KEY_RECEIVE_PARK_NOTIFICATIONS = "NOTIFY_PARKED";
    public static final String PREF_KEY_REDZONE_WARNING_TIME = "REDZONE_WARNING_TIME";

    private boolean mIsBound = false;

    private SweeperService mService;

    SharedPreferences.OnSharedPreferenceChangeListener spChanged = new
            SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {
                    ParkDetectionManager.ParkDetectionSettings settings =
                            mService.getParkDetectionSettings();

                    switch(key) {
                        case PREF_KEY_DRIVING_DRIVING_THRESHOLD:
                            Log.d(TAG, "preference changed! " + key + " " + sharedPreferences.getInt(key, 0));
                            settings.STATUS_DRIVING_DRIVING_THRESHOLD = sharedPreferences.getInt(key, 70);
                            break;
                        case PREF_KEY_DRIVING_PARKED_THRESHOLD:
                            Log.d(TAG, "preference changed! " + key + " " + sharedPreferences.getInt(key, 0));
                            settings.STATUS_DRIVING_PARKED_THRESHOLD = sharedPreferences.getInt(key, 20);
                            break;
                        case PREF_KEY_DRIVING_DECIDING_DRIVING_THRESHOLD:
                            Log.d(TAG, "preference changed! " + key + " " + sharedPreferences.getInt(key, 0));
                            settings.STATUS_DRIVING_DECIDING_DRIVING_THRESHOLD = sharedPreferences.getInt(key, 70);
                            break;
                        case PREF_KEY_DRIVING_DECIDING_PARKED_THRESHOLD:
                            Log.d(TAG, "preference changed! " + key + " " + sharedPreferences.getInt(key, 0));
                            settings.STATUS_DRIVING_DECIDING_PARKED_THRESHOLD = sharedPreferences.getInt(key, 30);
                            break;
                        case PREF_KEY_AGE_VALID:
                            Log.d(TAG, "preference changed! " + key + " " + sharedPreferences.getString(key, "40000"));
                            settings.AGE_OF_VALID_STATUS = Integer.parseInt(sharedPreferences.getString(key, "40000"));
                            break;
                        case PREF_KEY_PARKED_DRIVING_THRESHOLD:
                            Log.d(TAG, "preference changed! " + key + " " + sharedPreferences.getInt(key, 0));
                            settings.STATUS_PARKED_DRIVING_THRESHOLD = sharedPreferences.getInt(key, 100);
                            break;
                        case PREF_KEY_PARKED_PARKED_THRESHOLD:
                            Log.d(TAG, "preference changed! " + key + " " + sharedPreferences.getInt(key, 0));
                            settings.STATUS_PARKED_PARKED_THRESHOLD = sharedPreferences.getInt(key, 40);
                            break;
                        case PREF_KEY_PARKED_DECIDING_DRIVING_THRESHOLD:
                            Log.d(TAG, "preference changed! " + key + " " + sharedPreferences.getInt(key, 0));
                            settings.STATUS_PARKED_DECIDING_DRIVING_THRESHOLD = sharedPreferences.getInt(key, 90);
                            break;
                        case PREF_KEY_PARKED_DECIDING_PARKED_THRESHOLD:
                            Log.d(TAG, "preference changed! " + key + " " + sharedPreferences.getInt(key, 0));
                            settings.STATUS_PARKED_DECIDING_PARKED_THRESHOLD = sharedPreferences.getInt(key, 35);
                            break;
                        case PREF_KEY_ACTIVITY_UPDATE_RATE:
                            Log.d(TAG, "preference changed! " + key + " " + sharedPreferences.getString(key, "5000"));
                            settings.ACTIVITY_UPDATE_PERIOD = Integer.parseInt(sharedPreferences.getString(key, "5000"));
                            break;
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, SweeperService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(spChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.unregisterOnSharedPreferenceChangeListener(spChanged);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mIsBound) {
            unbindService(mServiceConnection);
            mIsBound = false;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service is connected!");
            mService = ((SweeperService.SweeperBinder)service).getService(SettingsActivity.this);
            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
        }
    };

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return GeneralPreferenceFragment.class.getName().equals(fragmentName) ||
                NotificationPreferenceFragment.class.getName().equals(fragmentName) ||
                AdvancedSettingsPreferenceFragment.class.getName().equals(fragmentName);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof SeekbarPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getInt(preference.getKey(), 0));
        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }

    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public static class AdvancedSettingsPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_advanced_settings);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_driving_driving_threshold)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_driving_parked_threshold)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_driving_deciding_driving_threshold)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_driving_deciding_parked_threshold)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_parked_driving_threshold)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_parked_parked_threshold)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_parked_deciding_driving_threshold)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_parked_deciding_parked_threshold)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_age_valid)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_activity_update_rate)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onGooglePlayConnectionStatusUpdated(SweeperService.GooglePlayConnectionStatus status) {
        // TODO
    }

    @Override
    public void onDriving() {

    }

    @Override
    public void onParked(List<SweepingPosition> results) {

    }
}
