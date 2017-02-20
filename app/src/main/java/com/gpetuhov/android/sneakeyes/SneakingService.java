package com.gpetuhov.android.sneakeyes;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
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

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

// Service takes pictures, gets location info and posts them to VK.
// Implements PhotoTaker.PhotoResultListener to receive callbacks from PhotoTaker.
// Service runs on the application MAIN thread!
public class SneakingService extends Service implements
        PhotoTaker.PhotoResultListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    // Tag for logging
    private static final String LOG_TAG = SneakingService.class.getName();

    // Sneak interval in minutes (for testing)
    private static final int SNEAK_INTERVAL = 1;

    // One minute in milliseconds
    private static final int SNEAK_INTERVAL_MINUTE = 60 * 1000;

    // Hashtag for VK wall posts
    private static final String VK_HASHTAG = "#SneakEyesApp";

    // Location request wait interval in milliseconds
    private static final long LOCATION_REQUEST_WAIT_INTERVAL = 5000;

    // Keeps instance of PhotoTaker. Injected by Dagger.
    @Inject PhotoTaker mPhotoTaker;

    private Bitmap mPhotoBitmap;

    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest;

    private Location mLocation;

    // Task will be run, if no location updates received during LOCATION_REQUEST_WAIT_INTERVAL
    private TimerTask mLocationRequestTimeoutTask = new TimerTask() {
        @Override
        public void run() {
            // Received no location updates.

            Log.d(LOG_TAG, "Location request timeout");
            Log.d(LOG_TAG, "Start posting without location");

            if (mGoogleApiClient != null) {
                mGoogleApiClient.disconnect();
            }

            // Start uploading photo to VK wall (photo will be posted without location info).
            loadPhotoToVKWall();
        }
    };

    // Timer will make TimerTask run
    private Timer mTimer;

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

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    // Method is called, when Service is started by incoming intent
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // IMPORTANT: This runs on the application MAIN thread!

        // If the user is logged in to VK
        if (VKSdk.isLoggedIn()) {
            // Take photo from the camera
            Log.d(LOG_TAG, "User is logged in. Sneaking...");
            Log.d(LOG_TAG, "Taking photo");
            mPhotoTaker.takePhoto(this);
        } else {
            // Otherwise (user is not logged in), do nothing and stop service
            Log.d(LOG_TAG, "User is NOT logged in. Stopping...");
            stopSelf();
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

    // --- PHOTOTAKER CALLBACKS ----------

    // Method is called by PhotoTaker, when photo is taken.
    @Override
    public void onPhotoTaken(Bitmap photoBitmap) {

        Log.d(LOG_TAG, "Photo received");

        // Save photo
        mPhotoBitmap = photoBitmap;

        // If Google Play Services are available
        if (isGooglePlayServicesAvailable()) {
            Log.d(LOG_TAG, "Connecting to GoogleApiClient");

            // Connect to GoogleApiClient to get location info
            mGoogleApiClient.connect();
        } else {
            // Google Play Services not available
            Log.d(LOG_TAG, "Google Play Services not available");
            Log.d(LOG_TAG, "Start posting without location");

            // Start uploading photo to VK wall (photo will be posted without location info).
            loadPhotoToVKWall();
        }
    }

    // Return true if Google Play Services available
    private boolean isGooglePlayServicesAvailable() {
        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        return errorCode == ConnectionResult.SUCCESS;
    }

    // Method is called by PhotoTaker, when error while taking photo occurs
    @Override
    public void onPhotoError() {
        Log.d(LOG_TAG, "Error taking photo. Stopping...");
        stopSelf();
    }

    // --- GOOGLE LOCATION SERVICE CALLBACKS ----------

    // Method is called, when GoogleApiClient connection established
    @Override
    public void onConnected(Bundle bundle) {

        Log.d(LOG_TAG, "Connected to GoogleApiClient");

        // Create request for current location
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setNumUpdates(1);  // We need only one update
        mLocationRequest.setInterval(0);    // We need it as soon as possible
        // The smallest displacement in meters the user must move between location updates
        // is by default set to 0, so we will receive onLocationChange() even if the user is not moving.

        Log.d(LOG_TAG, "Sending location request");

        // Send request
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        // Create new timer
        mTimer = new Timer();

        // Schedule timer to run mLocationRequestTimeoutTask,
        // if no location updates are received for LOCATION_REQUEST_WAIT_INTERVAL.
        mTimer.schedule(mLocationRequestTimeoutTask, LOCATION_REQUEST_WAIT_INTERVAL);
    }

    // Method is called, when GoogleApiClient connection suspended
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "GoogleApiClient connection has been suspended");

        mGoogleApiClient.disconnect();

        Log.d(LOG_TAG, "Start posting without location");

        // GoogleApiClient connection has been suspend.
        // Start uploading photo to VK wall (photo will be posted without location info).
        loadPhotoToVKWall();
    }

    // Method is called, when GoogleApiClient connection failed
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "GoogleApiClient connection has failed");

        mGoogleApiClient.disconnect();

        Log.d(LOG_TAG, "Start posting without location");

        // GoogleApiClient connection has failed.
        // Start uploading photo to VK wall (photo will be posted without location info).
        loadPhotoToVKWall();
    }

    // Method is called, when location information received
    @Override
    public void onLocationChanged(Location location) {

        //  Location update received, so cancel timer
        mTimer.cancel();

        Log.d(LOG_TAG, "Received location: " + location.toString());

        mGoogleApiClient.disconnect();

        // Save location info
        mLocation = location;

        Log.d(LOG_TAG, "Start posting with location");

        // Start uploading photo to VK wall (photo will be posted with location info).
        loadPhotoToVKWall();
    }

    // --- VK LOGIC ----------

    // Loading photo to VK wall is done in 2 steps:
    // 1. Upload photo to the server
    // 2. Make wall post with this uploaded photo
    private void loadPhotoToVKWall() {
        // Check if photo is available
        if (mPhotoBitmap != null) {
            // Photo is available. Start uploading to the server.

            Log.d(LOG_TAG, "Uploading photo to VK server");

            // Create VK request
            VKRequest request =
                    VKApi.uploadWallPhotoRequest(
                            new VKUploadImage(mPhotoBitmap, VKImageParameters.jpgImage(0.9f)),
                            getUserVKId(), 0);

            // Execute request and attach a listener for results
            request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    // Photo is uploaded to the server.
                    // Ready to make wall post with it.

                    Log.d(LOG_TAG, "Photo uploaded");

                    // Clear photo
                    mPhotoBitmap = null;

                    // Get uploaded photo ID from server response
                    VKApiPhoto photoModel = ((VKPhotoArray) response.parsedModel).get(0);

                    // Make wall post with attached photo
                    makePostToVKWall(new VKAttachments(photoModel), createWallPostMessage(), getUserVKId());
                }
                @Override
                public void onError(VKError error) {
                    // Error uploading photo to server.
                    // Stop service.
                    Log.d(LOG_TAG, "Error uploading photo. Stopping...");
                    stopSelf();
                }
            });
        } else {
            // Photo is not available. Stop service
            Log.d(LOG_TAG, "No photo available. Stopping...");
            stopSelf();
        }
    }

    // Return VK user ID
    private int getUserVKId() {
        // Get current VK access token
        final VKAccessToken vkAccessToken = VKAccessToken.currentToken();

        // Get user ID from access token
        return vkAccessToken != null ? Integer.parseInt(vkAccessToken.userId) : 0;
    }

    // Make post to the user's VK wall with provided attachments and message
    private void makePostToVKWall(VKAttachments att, String msg, final int ownerId) {

        Log.d(LOG_TAG, "Making VK wall post");

        // Create parameters for the wall post request
        VKParameters parameters = new VKParameters();
        parameters.put(VKApiConst.OWNER_ID, String.valueOf(ownerId));
        parameters.put(VKApiConst.ATTACHMENTS, att);
        parameters.put(VKApiConst.MESSAGE, msg);

        // Create wall post request
        VKRequest post = VKApi.wall().post(parameters);
        post.setModelClass(VKWallPostResult.class);

        // Execute wall post request and attach a listener for results
        post.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                // Post was added
                // All work is done. Service can be stopped.
                // (Services must be stopped manually)
                Log.d(LOG_TAG, "Successfully posted. Stopping...");
                stopSelf();
            }
            @Override
            public void onError(VKError error) {
                // Error
                // Stop Service anyway, because
                // Services must be stopped manually.
                Log.d(LOG_TAG, "Error making post. Stopping...");
                stopSelf();
            }
        });
    }

    // Return message for VK wall post
    private String createWallPostMessage() {
        String message;

        // If location info is available
        if (mLocation != null) {
            // Convert latitude and longitude to string
            String latitude = Double.toString(mLocation.getLatitude());
            String longitude = Double.toString(mLocation.getLongitude());
            // Construct message
            message = "Current location: " + latitude + ", " + longitude + " " + VK_HASHTAG;
        } else {
            // If location info is not available, include only hashtag into message
            message = VK_HASHTAG;
        }

        return message;
    }
}
