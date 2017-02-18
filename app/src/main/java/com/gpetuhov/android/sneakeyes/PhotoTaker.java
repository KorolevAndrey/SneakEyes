package com.gpetuhov.android.sneakeyes;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

// Takes photos from phone camera.
// Implements Camera.PictureCallback to handle photos taken by the camera.
// Implements Camera.PreviewCallback to take pictures when preview is ready only.
// Host of PhotoTaker must implement PhotoTaker.Callback to receive callback,
// when photo is taken and saved.
public class PhotoTaker implements Camera.PictureCallback, Camera.PreviewCallback {

    // Tag for logging
    private static final String LOG_TAG = PhotoTaker.class.getName();

    // Name of the file for saving photos
    private static final String PHOTO_FILE_NAME = "photo.jpg";

    // Context is needed for checking camera availability and saving photos
    private Context mContext;

    // Keeps camera instance
    private Camera mCamera;

    // Keeps reference to the host of PhotoTaker
    private Callback mCallback;

    // Host of PhotoTaker must implement this interface to receive callbacks
    public interface Callback {
        void onPhotoTaken();
    }

    public PhotoTaker(Context context) {
        mContext = context;
    }

    // Check camera availability, initialize camera and start image capture.
    // Call this method to take photos.
    public void takePhoto(Callback callback) {
        // Save reference to the host of PhotoTaker
        mCallback = callback;

        if (isCameraAvailable()) {
            releaseCamera();
            if (getCameraInstance()) {
                initCameraAndStartPreview();
                // Photo will be taken in callback method, when preview is ready.
            }
        } else {
            Log.e(LOG_TAG, "No camera on this device");
        }
    }

    // Check if this device has a camera
    private boolean isCameraAvailable() {

        Log.d(LOG_TAG, "Checking camera availability");

        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    // Camera must be released to be used later by this or other apps
    private void releaseCamera() {
        if (mCamera != null) {
            Log.d(LOG_TAG, "Releasing camera");
            mCamera.release();
            mCamera = null;
        }
    }

    // A safe way to get an instance of the Camera object
    private boolean getCameraInstance() {

        Log.d(LOG_TAG, "Getting camera instance");

        try {
            // Attempt to get a Camera instance.
            // Accesses the first, back-facing (main) camera.
            // TODO: Change this to open front or back camera depending on the passed ID
            mCamera = Camera.open(0);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.e(LOG_TAG, "Getting camera instance failed");
        }

        // Return true, if camera opened successfully
        return mCamera != null;
    }

    // Initialize camera and start preview
    private void initCameraAndStartPreview() {

        Log.d(LOG_TAG, "Initializing camera");

        if (mCamera != null) {
            try {
                // We don't need to show preview to user,
                // so just set new SurfaceTexture as a preview.
                mCamera.setPreviewTexture(new SurfaceTexture(10));

                // Set PhotoTaker as a listener to preview callbacks.
                // After one invocation, the callback is cleared.
                mCamera.setOneShotPreviewCallback(this);

                // Start preview. When preview is ready, onPreviewFrame() will be called once.
                mCamera.startPreview();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error while initializing camera");
            }
        }
    }

    // Method is called, when preview is ready (camera is ready to take pictures).
    // This method will be called only once,
    // because we set PhotoTaker as listener by setOneShotPreviewCallback().
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        Log.d(LOG_TAG, "Taking photo");

        // Start asynchronous image capture.
        // When image is taken, onPictureTaken() will be called.
        mCamera.takePicture(null, null, this);
    }

    // Method is called, when photo is taken.
    // Save photo in internal storage.
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        Log.d(LOG_TAG, "Saving photo");

        String filename = PHOTO_FILE_NAME;
        FileOutputStream outputStream;

        try {
            outputStream = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(data);
            outputStream.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while saving photo");
        }

        releaseCamera();

        // Tell the host of PhotoTaker, that photo is taken and saved.
        if (mCallback != null) {
            mCallback.onPhotoTaken();
        }
    }
}
