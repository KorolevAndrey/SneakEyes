package com.gpetuhov.android.sneakeyes;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.PreferenceManager;

// Service takes pictures, gets location info and posts them to VK
public class SneakingService extends IntentService {

    // Tag for logging
    private static final String LOG_TAG = SneakingService.class.getName();

    // Create new intent to start this service
    public static Intent newIntent(Context context) {
        return new Intent(context, SneakingService.class);
    }

    // Set AlarmManager to start or stop this service depending on settings in SharedPreferences
    public static void setServiceAlarm(Context context) {

        // Get Enabled/Disabled setting value from SharedPreferences
        boolean sneakingEnabled = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_onoff_key), true);

        // TODO: Set AlarmManager

    }

    public SneakingService() {
        super(LOG_TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // TODO: Handle intents

    }
}
