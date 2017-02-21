package com.gpetuhov.android.sneakeyes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.gpetuhov.android.sneakeyes.utils.UtilsPrefs;

import javax.inject.Inject;


// Fragment for application settings.
// Extends PreferenceFragmentCompat from the support library.
// Compatible with other fragments from the support library.
// Implements Preference.OnPreferenceChangeListener to update UI summary when preferences change.
// Implements SharedPreferences.OnSharedPreferenceChangeListener to initialize sneaking service
// when SharedPreferences change.
public class SettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Keeps instance of UtilsPrefs. Injected by Dagger.
    @Inject UtilsPrefs mUtilsPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inject UtilsPrefs instance into this fragment field
        SneakEyesApp.getAppComponent().inject(this);

        // If this is the first run after install
        if (mUtilsPrefs.isFirstRun()) {
            // Initialize sneaking
            SneakingService.setServiceAlarm(getActivity(), mUtilsPrefs);
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.pref_general, s);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register this fragment as a listener to SharedPreferences changes
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister this fragment as a listener to SharedPreferences changes
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    // Method is called when a preference changes (in this fragment).
    // This method is called BEFORE changes are written to SharedPreferences.
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        // TODO: Update UI for ListPreferences here
        return true;
    }

    // Method is called when SharedPreferences are changed
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        SneakingService.setServiceAlarm(getActivity(), mUtilsPrefs);
    }
}
