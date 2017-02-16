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

    public boolean isSneakingEnabled() {
        return mSharedPreferences.getBoolean(mContext.getString(R.string.pref_onoff_key), true);
    }
}
