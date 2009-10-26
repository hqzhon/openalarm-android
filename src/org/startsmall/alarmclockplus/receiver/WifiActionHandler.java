package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.R;
import org.startsmall.alarmclockplus.preference.ToggleButtonPreference;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class WifiActionHandler extends ActionHandler {
    private static final String TAG = "WifiActionHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> My WifiActionHandler.onReceive() haha");

    }

    public void addMyPreferences(Context context,
                                 PreferenceCategory category,
                                 String defaultValue) {
        ToggleButtonPreference onOffPref =
            new ToggleButtonPreference(context, null);
        onOffPref.setKey("state");
        onOffPref.setPersistent(true);
        onOffPref.setTitle(R.string.alarm_extra_settings_wifi_title);

        if(!TextUtils.isEmpty(defaultValue)) {
            String[] values = TextUtils.split(defaultValue, ";");
            for(int i = 0; i < values.length; i++) {
                if(TextUtils.isEmpty(values[i])) {
                    continue;
                }

                Pattern p = Pattern.compile("state\\s*=(\\w)+$");
                Matcher m = p.matcher(values[i]);
                if(m.matches()) {
                    if(m.group(0).equals("true")) {

                        Log.d(TAG, "====> setChecked(true)");

                        onOffPref.setChecked(true);
                    } else if(m.group(0).equals("false")) {

                        Log.d(TAG, "====> setChecked(false)");

                        onOffPref.setChecked(false);
                    }
                }
            }
        }

        category.addPreference(onOffPref);
    }

    private void setWifiEnabled(Context context, boolean toggle) {
        WifiManager wm =
            (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if(wm.isWifiEnabled() != toggle) {
            wm.setWifiEnabled(toggle);
        }
    }
}
