package com.gpetuhov.android.sneakeyes;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.gpetuhov.android.sneakeyes.utils.UtilsPrefs;

import javax.inject.Inject;

// Service takes pictures, gets location info and posts them to VK
public class SneakingService extends IntentService {

    // Tag for logging
    private static final String LOG_TAG = SneakingService.class.getName();

    // Keeps instance of UtilPrefs. Injected by Dagger.
    @Inject UtilsPrefs mUtilsPrefs;

    // Create new intent to start this service
    public static Intent newIntent(Context context) {
        return new Intent(context, SneakingService.class);
    }

    // Set AlarmManager to start or stop this service depending on settings in SharedPreferences
    public static void setServiceAlarm() {

        // TODO: Set AlarmManager

    }

    public SneakingService() {
        super(LOG_TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // TODO: Handle intents

    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Inject UtilsPrefs instance into this service field
        SneakEyesApp.getAppComponent().inject(this);
    }
}
