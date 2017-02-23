package com.gpetuhov.android.sneakeyes.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.gpetuhov.android.sneakeyes.R;

// Utilities for SharedPreferences
public class UtilsPrefs {

    private Context mContext;
    private SharedPreferences mSharedPreferences;

    public UtilsPrefs(Context context, SharedPreferences sharedPreferences) {
        mContext = context;
        mSharedPreferences = sharedPreferences;
    }

    // Register listener to SharedPreferences changes
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    // Unregister listener to SharedPreferences changes
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    // Return true if sneaking is enabled
    public boolean isSneakingEnabled() {
        // Get Enabled/Disabled setting value from SharedPreferences
        return mSharedPreferences.getBoolean(mContext.getString(R.string.pref_onoff_key), true);
    }

    // Get sneaking interval from SharedPreferences
    public int getSneakInterval() {
        String intervalString = mSharedPreferences.getString(
                mContext.getString(R.string.pref_interval_key),
                mContext.getString(R.string.pref_interval_value_2));

        return Integer.parseInt(intervalString);
    }

    // Run true if this is the first run after install
    public boolean isFirstRun() {
        boolean firstRun =
                mSharedPreferences.getBoolean(mContext.getString(R.string.pref_firstrun_key), true);

        if (firstRun) {
            // Set first run to false in shared prefs
            putBooleanToSharedPreferences(mContext.getString(R.string.pref_firstrun_key), false);
        }

        return firstRun;
    }

    public void putBooleanToSharedPreferences(String key, boolean value) {
        mSharedPreferences
                .edit()
                .putBoolean(key, value)
                .apply();
    }

    public String getStringFromSharedPreferences(String key, String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }
}
