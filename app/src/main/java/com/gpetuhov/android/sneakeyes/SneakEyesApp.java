package com.gpetuhov.android.sneakeyes;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.gpetuhov.android.sneakeyes.dagger.AppComponent;
import com.gpetuhov.android.sneakeyes.dagger.AppModule;
import com.gpetuhov.android.sneakeyes.dagger.DaggerAppComponent;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;

// SneakEyes application class.
// Builds and keeps instance of AppComponent,
// which is used to inject fields into application activities and fragments.
public class SneakEyesApp extends Application {

    // ID of new user logged out notification. Older notifications are replaced by new with the same ID.
    public static final int LOGOUT_NOTIFICATION_ID = 0;

    // Keeps instance of AppComponent
    private static AppComponent mAppComponent;

    public static AppComponent getAppComponent() {
        return mAppComponent;
    }

    // Needed to detect if VK access token has expired
    VKAccessTokenTracker mVKAccessTokenTracker = new VKAccessTokenTracker() {
        @Override
        public void onVKAccessTokenChanged(VKAccessToken oldToken, VKAccessToken newToken) {
            if (newToken == null) {
                // VKAccessToken is invalid

                // If SettingsActivity is running in foreground
                if (SettingsActivity.isResumed()) {
                    // Start VK log in procedure
                    SettingsActivity.loginToVK();
                } else {
                    // Otherwise show user logged out notification
                    showLogOutNotification();
                }
            }
        }
    };

    // Show user logged out notification
    private void showLogOutNotification() {
        // Get reference to resources
        Resources resources = getResources();

        // Create new intent to start SettingsActivity
        Intent i = SettingsActivity.newIntent(SneakEyesApp.this);

        // Create new pending intent with this intent to start new activity
        PendingIntent pi = PendingIntent.getActivity(SneakEyesApp.this, 0, i, 0);

        // Build new notification
        Notification notification = new NotificationCompat.Builder(SneakEyesApp.this)
                .setTicker(resources.getString(R.string.logoff_notification_ticker))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(resources.getString(R.string.logoff_notification_title))
                .setContentText(resources.getString(R.string.logoff_notification_text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        // Get notification manager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(SneakEyesApp.this);

        // Display notification
        notificationManager.notify(LOGOUT_NOTIFICATION_ID, notification);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Build and keep AppComponent instance.
        // DaggerAppComponent is generated by Dagger.
        mAppComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();

        // Start tracking VK access token
        mVKAccessTokenTracker.startTracking();

        // Initialize VKontakte SDK
        VKSdk.initialize(this);
    }
}

// Application icon created with the Android Asset Studio: https://romannurik.github.io/AndroidAssetStudio/
