package com.gpetuhov.android.sneakeyes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gpetuhov.android.sneakeyes.utils.UtilsPrefs;

import javax.inject.Inject;

// Broadcast receiver receives broadcast intent on system startup
// and activates sneaking service.
public class StartupReceiver extends BroadcastReceiver {

    // Keeps instance of UtilsPrefs. Injected by Dagger.
    @Inject UtilsPrefs mUtilsPrefs;

    // Method is called when broadcast receiver receives broadcast intent
    @Override
    public void onReceive(Context context, Intent intent) {

        // Inject UtilsPrefs instance
        SneakEyesApp.getAppComponent().inject(this);

        // Activate sneaking service depending on settings
        SneakingService.setServiceAlarm(context, mUtilsPrefs);
    }
}