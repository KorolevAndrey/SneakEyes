package com.gpetuhov.android.sneakeyes;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

// Activity with application settings
public class SettingsActivity extends SingleFragmentActivity {

    // Keeps reference to running activity instance
    private static SettingsActivity sSettingsActivity;

    // True if activity is running in foreground
    private static boolean sResumed = false;

    // Return true, if activity is running in foreground
    public static boolean isResumed() {
        return sResumed;
    }

    // Return new intent to start this activity
    public static Intent newIntent(Context context) {
        // Create explicit intent to start this activity
        return new Intent(context, SettingsActivity.class);
    }

    // Log user to VK
    public static void loginToVK() {
        if (sSettingsActivity != null) {
            // If the user is not logged in to VK
            if (!VKSdk.isLoggedIn()) {
                // Start VK login procedure (start VK login activity).
                // Request rights to access user wall and photos.
                // Request token, that never expires (offline).
                // IMPORTANT: VKSdk.login() does NOT support fragments from support library.
                // That's why we call it here, not in SettingsFragment.
                VKSdk.login(sSettingsActivity, VKScope.WALL, VKScope.PHOTOS, VKScope.OFFLINE);
            }
        }
    }

    @Override
    protected Fragment createFragment() {

        // Save reference to running activity instance
        sSettingsActivity = SettingsActivity.this;

        // Initialize sneaking
        SneakingService.setServiceAlarm(SettingsActivity.this);

        // Create and return SettingsFragment instance
        return new SettingsFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Activity goes into foreground
        sResumed = true;

        loginToVK();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Activity goes into background
        sResumed = false;
    }

    // Method is called after VK login activity finishes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Needed to process results of user VK authorization
        VKCallback<VKAccessToken> callback = new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                // User passed Authorization.
                // Do nothing, because sneaking already initialized.
            }

            @Override
            public void onError(VKError error) {
                // User didn't pass Authorization.
                // Do nothing, because we always check if the user is logged in in activity's onResume()
            }
        };

        if (!VKSdk.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
