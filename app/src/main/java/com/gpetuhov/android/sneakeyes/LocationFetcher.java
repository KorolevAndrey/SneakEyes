package com.gpetuhov.android.sneakeyes;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

// Gets current device location.
// User of LocationFetcher must implement LocationFetchedListener to receive callbacks.

// Sequence of execution:
// 1. fetchLocation()
// 2. GoogleApiClient.connect()
// 3. onConnected() or onConnectionFailed() or onConnectionSuspended()
// 4. LocationServices.FusedLocationApi.requestLocationUpdates()
// 5. onLocationChanged()

public class LocationFetcher implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    // Needed by Google Location Services
    private Context mContext;

    // Keeps Google API Client for location fetching
    private GoogleApiClient mGoogleApiClient;

    // Keeps location request
    private LocationRequest mLocationRequest;

    // Keeps reference to the listener to LocationFetcher
    private LocationFetchedListener mLocationFetchedListener;

    // User of LocationFetcher must implement this interface to receive callbacks
    public interface LocationFetchedListener {
        void onLocationFetchSuccess(Location location);
        void onLocationFetchError();
    }

    // Return true if we have permission to access fine and coarse location
    public static boolean checkLocationPermission(Context context) {

        // Check permission to access fine location
        boolean hasLocationFinePermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        // Check permission to access coarse location
        boolean hasLocationCoarsePermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        // Return true if we have both permissions
        return hasLocationFinePermission && hasLocationCoarsePermission;
    }

    public LocationFetcher(Context context) {
        // Save context
        mContext = context;

        // Create Google API Client
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    // Fetches location
    public void fetchLocation(LocationFetchedListener listener) {

        // Save reference to the listener
        mLocationFetchedListener = listener;

        // Check permission to access location
        if (checkLocationPermission(mContext)) {
            // We have permission to access location

            // If Google Play Services are available
            if (isGooglePlayServicesAvailable()) {
                // Connect to GoogleApiClient to get location info
                mGoogleApiClient.connect();
            } else {
                // Google Play Services not available
                reportError();
            }
        } else {
            // No permission to access location
            reportError();
        }
    }

    // This method must be called when LocationFetcher is no longer needed.
    // (Disconnects LocationFetcher from GoogleApiClient)
    public void stopFetchingLocation() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    // Return true if Google Play Services available
    private boolean isGooglePlayServicesAvailable() {
        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
        return errorCode == ConnectionResult.SUCCESS;
    }

    // Method is called, when GoogleApiClient connection established
    @Override
    public void onConnected(Bundle bundle) {
        // Create request for current location
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setNumUpdates(1);  // We need only one update
        mLocationRequest.setInterval(0);    // We need it as soon as possible
        // The smallest displacement in meters the user must move between location updates
        // is by default set to 0, so we will receive onLocationChange() even if the user is not moving.

        // Send request
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    // Method is called, when GoogleApiClient connection suspended
    @Override
    public void onConnectionSuspended(int i) {
        // GoogleApiClient connection has been suspended.
        // Report error to listener
        reportError();
    }

    // Method is called, when GoogleApiClient connection failed
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // GoogleApiClient connection has failed.
        // Report error to listener
        reportError();
    }

    // Method is called, when location information received
    @Override
    public void onLocationChanged(Location location) {
        // Pass received location to the listener
        reportSuccess(location);
    }

    // Report success to the listener
    private void reportSuccess(Location location) {
        stopFetchingLocation();

        if (mLocationFetchedListener != null) {
            // Pass fetched location to the listener
            mLocationFetchedListener.onLocationFetchSuccess(location);
            unregisterListener();
        }
    }

    // Report error to the listener
    private void reportError() {
        stopFetchingLocation();

        if (mLocationFetchedListener != null) {
            mLocationFetchedListener.onLocationFetchError();
            unregisterListener();
        }
    }

    // Unregister listener
    private void unregisterListener() {
        mLocationFetchedListener = null;
    }
}
