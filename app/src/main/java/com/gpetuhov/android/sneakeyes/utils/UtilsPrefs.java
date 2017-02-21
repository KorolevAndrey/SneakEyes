package com.gpetuhov.android.sneakeyes.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.gpetuhov.android.sneakeyes.R;

// Utilities for SharedPreferences
public class UtilsPrefs {

    private Context mContext;
    private SharedPreferences mSharedPreferences;

    public UtilsPrefs(Context context, SharedPreferences sharedPreferences) {
        mContext = context;
        mSharedPreferences = sharedPreferences;
    }

    // Return true if sneaking is enabled
    public boolean isSneakingEnabled() {
        // Get Enabled/Disabled setting value from SharedPreferences
        return mSharedPreferences.getBoolean(mContext.getString(R.string.pref_onoff_key), true);
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

    private void putBooleanToSharedPreferences(String key, boolean value) {
        mSharedPreferences
                .edit()
                .putBoolean(key, value)
                .apply();
    }
}
