package com.gpetuhov.android.sneakeyes;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

// Activity with application settings
public class SettingsActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {

        // If the user is not logged in to VK
        if (!VKSdk.isLoggedIn()) {
            // Start VK login procedure (start VK login activity).
            // Request rights to access user wall and photos.
            // IMPORTANT: VKSdk.login() does NOT support fragments from support library.
            // That's why we call it here, not in SettingsFragment.
            VKSdk.login(this, VKScope.WALL, VKScope.PHOTOS);
        }

        return new SettingsFragment();
    }

    // Method is called after VK login activity finishes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Needed to process results of user VK authorization
        VKCallback<VKAccessToken> callback = new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                // User passed Authorization

                // Initialize sneaking
                SneakingService.setServiceAlarm(SettingsActivity.this);
            }

            @Override
            public void onError(VKError error) {
                // User didn't pass Authorization
                // TODO: Handle this
            }
        };

        if (!VKSdk.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
