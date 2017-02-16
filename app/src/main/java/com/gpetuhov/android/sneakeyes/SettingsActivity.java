package com.gpetuhov.android.sneakeyes;

import android.support.v4.app.Fragment;

// Activity with application settings
public class SettingsActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        // Initialize sneaking
        SneakingService.setServiceAlarm(this);

        return new SettingsFragment();
    }
}
