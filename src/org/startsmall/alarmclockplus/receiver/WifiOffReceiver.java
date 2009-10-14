package org.startsmall.alarmclockplus;

import android.content.Context;
// import android.content.ContentResolver;
// import android.content.ContentUris;
import android.content.BroadcastReceiver;
import android.content.Intent;
// import android.database.Cursor;
//import android.net.Uri;
import android.net.wifi.WifiManager;
// import android.provider.BaseColumns;
import android.util.Log;

public class WifiOffReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiOffReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> My WifiOffReceiver.onReceive() haha");
        /// Should check if there is any extra data in the intent.
        // setWifiEnabled(context, false);
    }

    private void setWifiEnabled(Context context, boolean toggle) {
        WifiManager wm =
            (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if(wm.isWifiEnabled() != toggle) {
            wm.setWifiEnabled(toggle);
        }
    }
}
