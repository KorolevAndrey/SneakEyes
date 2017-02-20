package com.gpetuhov.android.sneakeyes.dagger;

import android.app.Application;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.gpetuhov.android.sneakeyes.LocationFetcher;
import com.gpetuhov.android.sneakeyes.PhotoTaker;
import com.gpetuhov.android.sneakeyes.PhotoUploader;
import com.gpetuhov.android.sneakeyes.utils.UtilsPrefs;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

// Dagger module tells, what instances will be instantiated
@Module
public class AppModule {

    Application mApplication;

    public AppModule(Application application) {
        mApplication = application;
    }

    // Returns instance of Application class
    @Provides
    @Singleton
    Application providesApplication() {
        return mApplication;
    }

    // Returns instance of default SharedPreferences.
    // This instance will be instantiated only once and will exist during entire application lifecycle.
    @Provides
    @Singleton
    SharedPreferences providesDefaultSharedPreferences(Application application) {
        return PreferenceManager.getDefaultSharedPreferences(application);
    }

    // Returns instance of UtilsPrefs
    @Provides
    @Singleton
    UtilsPrefs providesUtilsPrefs(Application application, SharedPreferences sharedPreferences) {
        UtilsPrefs utilsPrefs = new UtilsPrefs(application, sharedPreferences);
        return utilsPrefs;
    }

    // Returns instance of PhotoTaker
    @Provides
    @Singleton
    PhotoTaker providesPhotoTaker(Application application) {
        PhotoTaker photoTaker = new PhotoTaker(application);
        return photoTaker;
    }

    // Returns instance of PhotoUploader
    @Provides
    @Singleton
    PhotoUploader providesPhotoUploader() {
        PhotoUploader photoUploader = new PhotoUploader();
        return photoUploader;
    }

    // Returns instance of LocationFetcher
    @Provides
    @Singleton
    LocationFetcher providesLocationFetcher(Application application) {
        LocationFetcher locationFetcher = new LocationFetcher(application);
        return locationFetcher;
    }
}
