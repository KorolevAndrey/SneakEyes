package com.gpetuhov.android.sneakeyes.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

// Network utilities
public class UtilsNet {
    // Return "true" if network is available and connected
    public static boolean isNetworkAvailableAndConnected(Context context) {

        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        boolean isThereNetworkInfo = networkInfo != null;

        boolean isNetworkConnected = false;

        if (isThereNetworkInfo) {
            isNetworkConnected = networkInfo.isConnected();
        }

        return isThereNetworkInfo && isNetworkConnected;
    }
}
