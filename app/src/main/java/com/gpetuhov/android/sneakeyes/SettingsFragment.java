package com.gpetuhov.android.sneakeyes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
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
        // Inject UtilsPrefs instance into this fragment field.
        // This must be called before calling super.onCreate(),
        // because during super.onCreate() onCreatePreference() will be called,
        // where UtilsPrefs is used.
        SneakEyesApp.getAppComponent().inject(this);

        // If this is the first run after install
        if (mUtilsPrefs.isFirstRun()) {
            // Initialize sneaking
            SneakingService.setServiceAlarm(getActivity(), mUtilsPrefs);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.pref_general, s);

        // For list preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.

        // Get time interval preference key, find preference with this key
        // and bind this preference (time interval preference) summary to value
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_interval_key)));
    }

    // Attaches a listener so the summary is always updated with the preference value.
    // Also fires the listener once, to initialize the summary (so it shows up before the value
    // is changed.)
    private void bindPreferenceSummaryToValue(Preference preference) {

        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Display label according to current preference value in Summary
        // (get string with the key from the preference from SharedPreferences (value)
        // and update preference summary with label according to this value).
        displaySummary(preference,
                mUtilsPrefs.getStringFromSharedPreferences(preference.getKey(), ""));
    }


    // Display label according to the value of the preference in the preference Summary
    private void displaySummary(Preference preference, Object value) {

        // Cast new value of the preference to String
        String stringValue = value.toString();

        // If the preference is instance of ListPreference
        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).

            // Cast preference to ListPreference
            ListPreference listPreference = (ListPreference) preference;

            // Find index of the new value in values array
            // (ListPreference stores entries and values in 2 separate arrays)
            int prefIndex = listPreference.findIndexOfValue(stringValue);

            if (prefIndex >= 0) {
                // Get entry with the index of new value from entries array
                // and set this entry as a new summary of the preference.
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register this fragment as a listener to SharedPreferences changes
        mUtilsPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister this fragment as a listener to SharedPreferences changes
        mUtilsPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    // Method is called when a preference changes (in this fragment).
    // This method is called BEFORE changes are written to SharedPreferences.
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        // Display current preference value in Summary
        displaySummary(preference, value);

        return true;
    }

    // Method is called when SharedPreferences are changed
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        SneakingService.setServiceAlarm(getActivity(), mUtilsPrefs);
    }
}
