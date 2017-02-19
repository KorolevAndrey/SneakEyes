package com.gpetuhov.android.sneakeyes;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;

// Takes photos from phone camera.
// Implements Camera.PictureCallback to handle photos taken by the camera.
// Implements Camera.PreviewCallback to take pictures when preview is ready only.
// Host of PhotoTaker must implement PhotoTaker.Callback to receive callback,
// when photo is taken and ready.

// Sequence of execution: 1. takePhoto(), 2. onPreviewFrame(), 3. onPictureTaken()

public class PhotoTaker implements Camera.PictureCallback, Camera.PreviewCallback {

    // Tag for logging
    private static final String LOG_TAG = PhotoTaker.class.getName();

    public static final int OUTPUT_PHOTO_WIDTH = 800;

    public static final int OUTPUT_PHOTO_HEIGHT = 600;

    // Context is needed for checking camera availability and saving photos
    private Context mContext;

    // Keeps camera instance
    private Camera mCamera;

    // Keeps reference to the host of PhotoTaker
    private Callback mCallback;

    // Host of PhotoTaker must implement this interface to receive callbacks
    public interface Callback {
        void onPhotoTaken(Bitmap photoBitmap);
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

        // Photo is taken, we should release the camera.
        releaseCamera();

        Log.d(LOG_TAG, "Processing photo");

        // Convert byte array, received from the camera,
        // to Bitmap, scaled to output width and height.
        Bitmap photoBitmap = getScaledBitmap(data, OUTPUT_PHOTO_WIDTH, OUTPUT_PHOTO_HEIGHT);

        // Pass photo Bitmap to the host of PhotoTaker
        if (mCallback != null) {
            mCallback.onPhotoTaken(photoBitmap);
        }
    }

    // Get scaled Bitmap from byte array
    private Bitmap getScaledBitmap(byte[] data, int destWidth, int destHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;

        int inSampleSize = 1;
        if (srcHeight > destHeight || srcWidth > destWidth) {
            if (srcWidth > srcHeight) {
                inSampleSize = Math.round(srcHeight / destHeight);
            } else {
                inSampleSize = Math.round(srcWidth / destWidth);
            }
        }

        options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;

        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }
}
