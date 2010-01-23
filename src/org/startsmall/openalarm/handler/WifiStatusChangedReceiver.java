package org.startsmall.openalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

public class WifiStatusChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiStatusChangedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "===> My WifiStatusChangedReceiver.onReceive()");

        final String label = intent.getStringExtra("label");
        WifiManager wm = (WifiManager)context.getSystemService(
            Context.WIFI_SERVICE);
        int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        String stateString = "Off";
        switch (state) {
        case WifiManager.WIFI_STATE_ENABLED:
            stateString = "On";
            break;
        case WifiManager.WIFI_STATE_DISABLED:
            break;
        default:
            return;
        }

        Notificator notificator = new Notificator(context);
        notificator.set(0,
                        R.drawable.ic_toggle_apn,
                        context.getString(R.string.apn_notification_ticker,
                                          stateString),
                        context.getString(R.string.apn_notification_content,
                                          label, stateString.toLowerCase()));
    }
}



