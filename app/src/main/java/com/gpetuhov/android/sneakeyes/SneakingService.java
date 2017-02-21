package com.gpetuhov.android.sneakeyes;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.gpetuhov.android.sneakeyes.utils.UtilsNet;
import com.gpetuhov.android.sneakeyes.utils.UtilsPrefs;
import com.vk.sdk.VKSdk;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

// Service takes pictures, gets location info and posts them to VK.
// Implements PhotoTaker.PhotoResultListener to receive callbacks from PhotoTaker.
// Service runs on the application MAIN thread!

// Sequence of execution:
// 1. onStartCommand()
// 2. PhotoTaker.takePhoto()
// 3. onPhotoTaken() or onPhotoError()
// 4. LocationFetcher.fetchLocation()
// 5. onLocationFetchSuccess() or onLocationFetchError() or TimerTask.run()
// 6. PhotoUploader.uploadPhoto()
// 7. onPhotoUploadSuccess() or onPhotoUploadError()
// 8. stopSelf()

public class SneakingService extends Service implements
        PhotoTaker.PhotoResultListener,
        LocationFetcher.LocationFetchedListener,
        PhotoUploader.PhotoUploadedListener {

    // Tag for logging
    private static final String LOG_TAG = SneakingService.class.getName();

    // Sneak interval in minutes (for testing)
    private static final int SNEAK_INTERVAL = 1;

    // One minute in milliseconds
    private static final int SNEAK_INTERVAL_MINUTE = 60 * 1000;

    // Location request wait interval in milliseconds
    private static final long LOCATION_REQUEST_WAIT_INTERVAL = 5000;

    // Keeps instance of PhotoTaker. Injected by Dagger.
    @Inject PhotoTaker mPhotoTaker;

    // Keeps instance of LocationFetcher. Injected by Dagger.
    @Inject LocationFetcher mLocationFetcher;

    // Keeps instance of PhotoUploader. Injected by Dagger.
    @Inject PhotoUploader mPhotoUploader;

    // Keeps taken photos
    private List<Bitmap> mPhotos;

    // Task will be run, if no location received during LOCATION_REQUEST_WAIT_INTERVAL
    private TimerTask mLocationRequestTimeoutTask = new TimerTask() {
        @Override
        public void run() {
            // Received no location info.

            Log.d(LOG_TAG, "Location request timeout");
            Log.d(LOG_TAG, "Start posting without location");

            // Stop LocationFetcher
            mLocationFetcher.stopFetchingLocation();

            // Start uploading photo to VK wall without location info.
            mPhotoUploader.uploadPhoto(mPhotos, null, SneakingService.this);
        }
    };

    // Timer will make TimerTask run
    private Timer mTimer;

    // Create new intent to start this service
    public static Intent newIntent(Context context) {
        return new Intent(context, SneakingService.class);
    }

    // Set AlarmManager to start or stop this service depending on settings in SharedPreferences
    public static void setServiceAlarm(Context context, UtilsPrefs utilsPrefs) {

        Log.d(LOG_TAG, "Setting AlarmManager");

        // Create new intent to start this service
        Intent i = SneakingService.newIntent(context);

        // Get pending intent with this intent.
        // If pending intent for such intent already exists,
        // getService returns reference to it.
        // Otherwise new pending intent is created.
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        // Get reference to AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // TODO: Get sneak interval from SharedPreferences
        int sneakInterval = SNEAK_INTERVAL;

        // If sneaking is enabled
        if (utilsPrefs.isSneakingEnabled()) {

            // Calculate sneak interval in milliseconds
            int sneakIntervalMillis = sneakInterval * SNEAK_INTERVAL_MINUTE;

            // Turn on AlarmManager for inexact repeating
            // (every sneak interval AlarmManager will send pending request to start this service).
            // Time base is set to elapsed time since last system startup.
            // First time AlarmManager will trigger after first sneak interval from current time.
            // AlarmManager will wake the device if it goes off.
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + sneakIntervalMillis,
                    sneakIntervalMillis, pi);

        } else {
            // Otherwise turn AlarmManager off

            // Cancel AlarmManager
            alarmManager.cancel(pi);

            // Cancel pending intent
            pi.cancel();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Inject PhotoTaker instance into this service field
        SneakEyesApp.getAppComponent().inject(this);
    }

    // Method is called, when Service is started by incoming intent
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // IMPORTANT: This runs on the application MAIN thread!

        // Check network status
        if (UtilsNet.isNetworkAvailableAndConnected(this)) {
            // Network available

            // If the user is logged in to VK
            if (VKSdk.isLoggedIn()) {
                // Take photo from the camera
                Log.d(LOG_TAG, "User is logged in. Sneaking...");
                Log.d(LOG_TAG, "Taking photo");

                mPhotoTaker.takePhoto(this);
            } else {
                // Otherwise (user is not logged in), do nothing and stop service
                Log.d(LOG_TAG, "User is NOT logged in. Stopping...");
                stopSneakingService();
            }
        } else {
            // Network not available
            // Do nothing and stop service
            Log.d(LOG_TAG, "Network not available. Stopping...");
            stopSneakingService();
        }

        // Don't restart service if killed
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We don't use binding.
        return null;
    }

    // Cancel timer
    private void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    // --- PHOTOTAKER CALLBACKS ----------

    // Method is called by PhotoTaker, when photos are taken.
    @Override
    public void onPhotoTaken(List<Bitmap> photos) {
        Log.d(LOG_TAG, "Photos received");

        // Save taken photo
        mPhotos = photos;

        Log.d(LOG_TAG, "Fetching location");

        // Fetch location
        mLocationFetcher.fetchLocation(this);

        // Create new timer
        mTimer = new Timer();

        // Schedule timer to run mLocationRequestTimeoutTask,
        // if no location received for LOCATION_REQUEST_WAIT_INTERVAL.
        mTimer.schedule(mLocationRequestTimeoutTask, LOCATION_REQUEST_WAIT_INTERVAL);
    }

    // Method is called by PhotoTaker, when error while taking photo occurs
    @Override
    public void onPhotoError() {
        Log.d(LOG_TAG, "Error taking photo. Stopping...");
        stopSneakingService();
    }

    // --- LOCATIONFETCHER CALLBACKS ----------

    // Method is called when LocationFetcher successfully fetches location
    @Override
    public void onLocationFetchSuccess(Location location) {
        Log.d(LOG_TAG, "Location received");
        Log.d(LOG_TAG, "Start posting with location");

        //  Location received, so cancel timer
        cancelTimer();

        // Stop LocationFetcher
        mLocationFetcher.stopFetchingLocation();

        // Start uploading photo to VK wall with location info.
        mPhotoUploader.uploadPhoto(mPhotos, location, this);
    }

    // Method is called when there is error in LocationFetcher fetching location
    @Override
    public void onLocationFetchError() {
        Log.d(LOG_TAG, "Error receiving location");
        Log.d(LOG_TAG, "Start posting without location");

        // Error receiving location, nothing to wait for, so cancel timer
        cancelTimer();

        // Stop LocationFetcher
        mLocationFetcher.stopFetchingLocation();

        // Start uploading photo to VK wall without location info.
        mPhotoUploader.uploadPhoto(mPhotos, null, this);
    }

    // --- PHOTOUPLOADER CALLBACKS ----------

    // Method is called, if PhotoUploader successfully posts photo to VK
    @Override
    public void onPhotoUploadSuccess() {
        Log.d(LOG_TAG, "Posted successfully. Stopping...");
        stopSneakingService();
    }

    // Method is called, if there is error in PhotoUploader posting photo to VK
    @Override
    public void onPhotoUploadError() {
        Log.d(LOG_TAG, "Error posting. Stopping...");
        stopSneakingService();
    }

    private void stopSneakingService() {
        // Cancel timer (just in case it isn't cancelled yet)
        cancelTimer();

        // Clear photos
        if (mPhotos != null) {
            mPhotos.clear();
            mPhotos = null;
        }

        // Stop service
        stopSelf();
    }
}
