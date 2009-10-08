package org.startsmall.alarmclockplus;

import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.BaseColumns;
import android.util.Log;

public class WifiOnReceiver extends BroadcastReceiver {
    public static final String TITLE = "Wi-Fi On";
    private static final String TAG = "WifiOnReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> My WifiOnReceiver.onReceive() haha");
    }

    private void setWifiEnabled(Context context, boolean toggle) {
        WifiManager wm =
            (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if(wm.isWifiEnabled() != toggle) {
            wm.setWifiEnabled(toggle);
        }
    }
}
