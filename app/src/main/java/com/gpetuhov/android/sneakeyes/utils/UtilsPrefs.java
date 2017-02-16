package com.gpetuhov.android.sneakeyes.utils;

import android.content.Context;
import android.content.SharedPreferences;

// Utilities for SharedPreferences
public class UtilsPrefs {

    private Context mContext;
    private SharedPreferences mSharedPreferences;

    public UtilsPrefs(Context context, SharedPreferences sharedPreferences) {
        mContext = context;
        mSharedPreferences = sharedPreferences;
    }
}
