package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.R;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.util.Log;

public class WifiActionHandler extends ActionHandler {
    private static final String TAG = "WifiActionHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> My WifiActionHandler.onReceive() haha");
    }

    public void addMyPreferences(Context context,
                                 PreferenceCategory category) {
        class MyPreference extends ListPreference {
            public MyPreference(Context context) {
                super(context);
            }

            protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
                builder
                    .setSingleChoiceItems(
                        super.getEntries(),
                        0,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                MyPreference.this.setSummary(
                                    MyPreference.this.getValue());
                            }
                        });
            }
        }

        MyPreference onOffPref = new MyPreference(context);
        onOffPref.setPersistent(true);
        onOffPref.setTitle(R.string.alarm_extra_settings_wifi_title);
        CharSequence[] entries = new CharSequence[]{"On", "Off"};
        CharSequence[] entryValues = new CharSequence[]{"On", "Off"};
        onOffPref.setEntries(entries);
        onOffPref.setEntryValues(entryValues);
        onOffPref.setValueIndex(0);
        onOffPref.setSummary(onOffPref.getValue());

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
