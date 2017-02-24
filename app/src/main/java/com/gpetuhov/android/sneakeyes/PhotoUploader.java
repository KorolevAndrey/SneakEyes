package com.gpetuhov.android.sneakeyes;

import android.graphics.Bitmap;
import android.location.Location;
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

import java.util.ArrayList;
import java.util.List;

// Posts photos to user's VK wall.
// User of PhotoUploader must implement PhotoUploadedListener to receive callbacks

// Sequence of execution: 1. uploadPhoto(), 2. loadPhotoToVKWall(), 3. makePostToVKWall()

public class PhotoUploader {

    // Hashtag for VK wall posts
    private static final String VK_HASHTAG = "#SneakEyesApp";

    // Keeps photos to upload
    private List<Bitmap> mPhotos;

    // Keeps location to attach to wall post
    private Location mLocation;

    // Keeps reference to the listener to PhotoUploader
    private PhotoUploadedListener mPhotoUploadedListener;

    // Keeps list of uploaded photos IDs ready to be attached to the wall post
    private List<VKApiPhoto> mVKApiPhotos;

    // Receives results of photo upload to VK server
    private VKRequest.VKRequestListener mVKUploadPhotoListener = new VKRequest.VKRequestListener() {
        @Override
        public void onComplete(VKResponse response) {
            // Photo is uploaded to the server.
            // Ready to make wall post with it.

            // Get uploaded photo ID from server response
            VKApiPhoto photoModel = ((VKPhotoArray) response.parsedModel).get(0);

            // Add uploaded photo ID to the list of attachments
            mVKApiPhotos.add(photoModel);

            // Remove uploaded photo from the list of photos
            mPhotos.remove(0);

            // Check if there are more photos to upload
            if (!mPhotos.isEmpty()) {
                // Still have photos to upload
                startUploadPhotoToServer();
            } else {
                // All photos uploaded
                // Make wall post with attached photos
                makePostToVKWall(new VKAttachments(mVKApiPhotos), createWallPostMessage(), getUserVKId());
            }
        }

        @Override
        public void onError(VKError error) {
            // Error uploading photo to server.
            reportError();
        }
    };

    // User of PhotoUploader must implement this interface to receive callbacks
    public interface PhotoUploadedListener {
        void onPhotoUploadSuccess();
        void onPhotoUploadError();
    }

    // Upload photo to VK wall.
    // Call this method to make VK wall post with photo attached.
    public void uploadPhoto(List<Bitmap> photos, Location location, PhotoUploadedListener listener) {
        if (photos != null) {
            // Photos provided

            // Save photos, location and listener
            mPhotos = photos;
            mLocation = location;
            mPhotoUploadedListener = listener;

            // Start uploading
            loadPhotoToVKWall();
        } else {
            // Photos not provided
            reportError();
        }
    }

    // Loading photo to VK wall is done in 2 steps:
    // 1. Upload photos to the server
    // 2. Make wall post with this uploaded photo
    private void loadPhotoToVKWall() {
        // Check if photos are available
        if (!mPhotos.isEmpty()) {
            // Photos are available. Start uploading to the server.

            // Create new empty list of attachments
            mVKApiPhotos = new ArrayList<>();

            startUploadPhotoToServer();
        } else {
            // Photos are not available.
            reportError();
        }
    }

    private void startUploadPhotoToServer() {
        // Create VK request
        VKRequest request =
                VKApi.uploadWallPhotoRequest(
                        new VKUploadImage(mPhotos.get(0), VKImageParameters.jpgImage(0.9f)),
                        getUserVKId(), 0);

        // Execute request and attach a listener for results
        request.executeWithListener(mVKUploadPhotoListener);
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
                reportSuccess();
            }
            @Override
            public void onError(VKError error) {
                // Error
                reportError();
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

    // Report success to the listener
    private void reportSuccess() {
        if (mPhotoUploadedListener != null) {
            mPhotoUploadedListener.onPhotoUploadSuccess();
            unregisterListener();
        }
    }

    // Report error to the listener
    private void reportError() {
        if (mPhotoUploadedListener != null) {
            mPhotoUploadedListener.onPhotoUploadError();
            unregisterListener();
        }
    }

    // Unregister listener
    private void unregisterListener() {
        mPhotoUploadedListener = null;
    }
}
