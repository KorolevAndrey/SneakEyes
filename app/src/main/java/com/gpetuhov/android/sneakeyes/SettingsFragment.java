package com.gpetuhov.android.sneakeyes;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;


// Fragment for application settings.
// Extends PreferenceFragmentCompat from the support library.
// Compatible with other fragments from the support library.
// Implements Preference.OnPreferenceChangeListener to update UI summary when preferences change.
public class SettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.pref_general, s);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        return false;
    }
}
