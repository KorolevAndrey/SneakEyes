package com.gpetuhov.android.sneakeyes;

import android.app.Application;

// SneakEyes application class.
// Builds and keeps instance of AppComponent,
// which is used to inject fields into application activities and fragments
public class SneakEyesApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize sneaking service
        SneakingService.setServiceAlarm(this);
    }
}
