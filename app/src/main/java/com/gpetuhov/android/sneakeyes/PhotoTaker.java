package com.gpetuhov.android.sneakeyes;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Takes photos from phone camera.
// Implements Camera.PictureCallback to handle photos taken by the camera.
// Implements Camera.PreviewCallback to take pictures when preview is ready only.
// User of PhotoTaker must implement PhotoTaker.PhotoResultListener to receive callback,
// when photo is taken and ready.

// Sequence of execution: 1. takePhoto(), 2. onPreviewFrame(), 3. onPictureTaken() or onPictureError()

public class PhotoTaker implements Camera.PictureCallback, Camera.PreviewCallback {

    // Output photo dimensions
    // PhotoTaker will scale photo from the camera to fit these dimensions
    public static final int OUTPUT_PHOTO_WIDTH = 800;
    public static final int OUTPUT_PHOTO_HEIGHT = 600;

    // Context is needed for checking camera availability and saving photos
    private Context mContext;

    // Keeps camera instance
    private Camera mCamera;

    // Number of cameras available
    private int mNumberOfCameras;

    // ID of the current camera in use
    private int mCurrentCamera;

    private List<Bitmap> mPhotos;

    // Keeps reference to the listener to PhotoTaker
    private PhotoResultListener mPhotoResultListener;

    // User of PhotoTaker must implement this interface to receive callbacks
    public interface PhotoResultListener {
        void onPhotoTaken(List<Bitmap> photos);
        void onPhotoError();
    }

    // Return true if we have permission to access camera
    public static boolean checkCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public PhotoTaker(Context context) {
        mContext = context;
    }

    // Check camera availability, initialize camera and start image capture.
    // Call this method to take photos.
    public void takePhoto(PhotoResultListener photoResultListener) {

        // Save reference to the listener
        mPhotoResultListener = photoResultListener;

        // Check permission to access camera
        if (checkCameraPermission(mContext)) {
            // We have permission to access camera

            // CHeck if any camera is available
            if (isCameraAvailable()) {
                // Cameras are available

                // Get number of cameras available
                int numberOfCameras = Camera.getNumberOfCameras();

                if (numberOfCameras > 0) {
                    // Device has one or more cameras

                    // If device has more than 1 camera, use only 2 of them
                    if (numberOfCameras > 1) {
                        numberOfCameras = 2;
                    }

                    // Save number of cameras
                    mNumberOfCameras = numberOfCameras;

                    // Set current camera to 0 (back-facing)
                    mCurrentCamera = 0;

                    // Create new empty list of photos
                    mPhotos = new ArrayList<>();

                    takePhotoFromCamera(mCurrentCamera);
                } else {
                    // No cameras on device
                    reportError();
                }
            } else {
                // No camera is available
                reportError();
            }
        } else {
            // No permission to access camera
            reportError();
        }
    }

    // Takes photo from the camera with provided ID
    private void takePhotoFromCamera(int cameraId) {

        // Release camera (because camera may be in use by previous operations)
        releaseCamera();

        // Try to get camera instance
        if (getCameraInstance(cameraId)) {
            // Camera instance acquired

            // Try to initialize camera and start preview
            boolean success = initCameraAndStartPreview();

            // If not success, report error to the listener
            if (!success) {
                reportError();
            }

            // Photo will be taken in callback method, when preview is ready.
        } else {
            // Camera instance not acquired
            reportError();
        }
    }

    // Check if this device has a camera
    private boolean isCameraAvailable() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    // Camera must be released to be used later by this or other apps
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    // A safe way to get an instance of the Camera object
    private boolean getCameraInstance(int cameraId) {
        try {
            // Attempt to get a Camera instance.
            mCamera = Camera.open(cameraId);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            // Do nothing, because we will return false in this case
        }

        // Return true, if camera opened successfully
        return mCamera != null;
    }

    // Initialize camera and start preview
    private boolean initCameraAndStartPreview() {
        // True if operation completed successfully
        boolean successFlag = true;

        // If camera instance is available
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
                // Error while initializing camera
                successFlag = false;
            }
        } else {
            // No camera instance available
            successFlag = false;
        }

        return successFlag;
    }

    // Method is called, when preview is ready (camera is ready to take pictures).
    // This method will be called only once,
    // because we set PhotoTaker as listener by setOneShotPreviewCallback().
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
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

        // Convert byte array, received from the camera,
        // to Bitmap, scaled to output width and height.
        Bitmap photoBitmap = getScaledBitmap(data, OUTPUT_PHOTO_WIDTH, OUTPUT_PHOTO_HEIGHT);

        // Save taken photo
        mPhotos.add(photoBitmap);

        // Switch to next camera
        mCurrentCamera++;

        if (mCurrentCamera < mNumberOfCameras) {
            // Didn't use all available cameras.
            // Take photo from this camera.
            takePhotoFromCamera(mCurrentCamera);
        } else {
            // Used all available cameras.
            // Send taken photos to listener.
            reportSuccess();
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

    // Report success to the listener
    private void reportSuccess() {
        if (mPhotoResultListener != null) {
            // Pass photo Bitmap to the listener
            mPhotoResultListener.onPhotoTaken(mPhotos);
            unregisterListener();
        }
    }

    // Report error to the listener
    private void reportError() {
        if (mPhotoResultListener != null) {
            mPhotoResultListener.onPhotoError();
            unregisterListener();
        }
    }

    // Unregister listener
    private void unregisterListener() {
        mPhotoResultListener = null;
    }
}
