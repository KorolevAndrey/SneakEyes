package com.gpetuhov.android.sneakeyes;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiPhoto;
import com.vk.sdk.api.model.VKAttachments;
import com.vk.sdk.api.model.VKPhotoArray;
import com.vk.sdk.api.model.VKWallPostResult;
import com.vk.sdk.api.photo.VKImageParameters;
import com.vk.sdk.api.photo.VKUploadImage;

import javax.inject.Inject;

// Service takes pictures, gets location info and posts them to VK.
// Implements PhotoTaker.Callback to receive callbacks from PhotoTaker, when photo is ready.
// Service runs on the application MAIN thread!
public class SneakingService extends Service implements PhotoTaker.Callback {

    // Tag for logging
    private static final String LOG_TAG = SneakingService.class.getName();

    // Sneak interval in minutes (for testing)
    private static final int SNEAK_INTERVAL = 1;

    // One minute in milliseconds
    private static final int SNEAK_INTERVAL_MINUTE = 60 * 1000;

    // Hashtag for VK wall posts
    public static final String VK_HASHTAG = "#SneakEyesApp";

    // Keeps instance of PhotoTaker. Injected by Dagger.
    @Inject PhotoTaker mPhotoTaker;

    // Create new intent to start this service
    public static Intent newIntent(Context context) {
        return new Intent(context, SneakingService.class);
    }

    // Set AlarmManager to start or stop this service depending on settings in SharedPreferences
    public static void setServiceAlarm(Context context) {

        // Create new intent to start this service
        Intent i = SneakingService.newIntent(context);

        // Get pending intent with this intent.
        // If pending intent for such intent already exists,
        // getService returns reference to it.
        // Otherwise new pending intent is created.
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        // Get reference to AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Get SharedPreferences
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);

        // Get Enabled/Disabled setting value from SharedPreferences
        boolean sneakingEnabled =
                sharedPreferences.getBoolean(context.getString(R.string.pref_onoff_key), true);

        // TODO: Get sneak interval from SharedPreferences
        int sneakInterval = SNEAK_INTERVAL;

        // If sneaking is enabled
        if (sneakingEnabled) {

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

        Log.d(LOG_TAG, "Sneaking");

        mPhotoTaker.takePhoto(this);

        // Don't restart service if killed
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We don't use binding.
        return null;
    }

    // Method is called by PhotoTaker, when photo is taken.
    @Override
    public void onPhotoTaken(Bitmap photoBitmap) {

        // Photo is ready.
        // Start uploading it to VK wall
        loadPhotoToVKWall(photoBitmap, VK_HASHTAG);
    }

    // Loading photo to VK wall is done in 2 steps:
    // 1. Upload photo to the server
    // 2. Make wall post with this uploaded photo
    void loadPhotoToVKWall(final Bitmap photo, final String message) {
        VKRequest request =
                VKApi.uploadWallPhotoRequest(
                        new VKUploadImage(photo, VKImageParameters.jpgImage(0.9f)),
                        getUserVKId(), 0);
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                // Photo is uploaded to the server.
                // Ready to make wall post with it.
                VKApiPhoto photoModel = ((VKPhotoArray) response.parsedModel).get(0);
                makePostToVKWall(new VKAttachments(photoModel), message, getUserVKId());
            }
            @Override
            public void onError(VKError error) {
                // Error
            }
        });
    }

    // Return VK user ID
    private int getUserVKId() {
        final VKAccessToken vkAccessToken = VKAccessToken.currentToken();
        return vkAccessToken != null ? Integer.parseInt(vkAccessToken.userId) : 0;
    }

    // Make post to the user's VK wall with provided attachments and message
    private void makePostToVKWall(VKAttachments att, String msg, final int ownerId) {
        VKParameters parameters = new VKParameters();
        parameters.put(VKApiConst.OWNER_ID, String.valueOf(ownerId));
        parameters.put(VKApiConst.ATTACHMENTS, att);
        parameters.put(VKApiConst.MESSAGE, msg);
        VKRequest post = VKApi.wall().post(parameters);
        post.setModelClass(VKWallPostResult.class);
        post.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                // Post was added
                // All work is done. Service can be stopped.
                // (Services must be stopped manually)
                stopSelf();
            }
            @Override
            public void onError(VKError error) {
                // Error
                // Stop Service anyway, because
                // Services must be stopped manually.
                stopSelf();
            }
        });
    }
}
