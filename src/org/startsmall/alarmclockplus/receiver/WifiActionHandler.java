/**
 * @file   WifiActionHandler.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Thu Oct 29 11:23:05 2009
 *
 * @brief
 *
 *
 */

package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.preference.ListPreference;
import org.startsmall.alarmclockplus.Alarms;
import org.startsmall.alarmclockplus.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class WifiActionHandler extends ActionHandler {
    private static final String TAG = "WifiActionHandler";
    private static final String KEY = "wifi_state";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> My WifiActionHandler.onReceive() haha");

        final int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);
        final String label = intent.getStringExtra(Alarms.AlarmColumns.LABEL);

        // Parse extra settings out of combined value.
        final String extra = intent.getStringExtra(Alarms.AlarmColumns.EXTRA);
        if(!TextUtils.isEmpty(extra)) {
            String[] values = TextUtils.split(extra, ";");
            for(int i = 0; i < values.length; i++) {
                if(TextUtils.isEmpty(values[i])) {
                    continue;
                }

                String value = parseExtra(values[i], KEY);
                if(!TextUtils.isEmpty(value)) {
                    if(value.equals("On")) {
                        setWifiEnabled(context, true);
                    } else if(value.equals("Off")) {
                        setWifiEnabled(context, false);
                    }
                }
            }
        }
    }

    public void addMyPreferences(final Context context,
                                 final PreferenceCategory category,
                                 final String defaultValue) {
        class OnOffPreference extends ListPreference {
            public OnOffPreference(Context context) {
                super(context, null);
            }

            @Override
            protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
                builder
                    .setTitle("Turn Wi-Fi")
                    .setSingleChoiceItems(
                        getEntries(),
                        getDefaultCheckedIndex(defaultValue),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d,
                                                int which) {
                                setPreferenceValueIndex(which);
                                d.dismiss();
                            }
                        })
                    .setNegativeButton(android.R.string.cancel, null);
            }

            @Override
            protected void generateListItems(
                ArrayList<CharSequence> entries,
                ArrayList<CharSequence> entryValues) {
                Collections.addAll(entries, "On", "Off");
                Collections.addAll(entryValues, "On", "Off");
            }

            @Override
            protected void onClick() {
                Dialog dialog = getDialog();
                if(dialog != null) {
                    dialog.show();
                }
            }

            int getDefaultCheckedIndex(String defaultValue) {
                if(!TextUtils.isEmpty(defaultValue) &&
                   parseExtra(defaultValue, KEY) == "Off") {
                    return 1;
                }
                return 0;
            }
        }

        OnOffPreference onOffPref = new OnOffPreference(context);
        onOffPref.setKey(KEY);
        onOffPref.setPersistent(true);
        onOffPref.setDefaultValue("On");
        onOffPref.setTitle(R.string.alarm_extra_settings_wifi_title);

        if(!TextUtils.isEmpty(defaultValue)) {
            String[] values = TextUtils.split(defaultValue, ";");
            for(int i = 0; i < values.length; i++) {
                if(TextUtils.isEmpty(values[i])) {
                    continue;
                }

                Pattern p = Pattern.compile(KEY + "\\s*=(\\w)+$");
                Matcher m = p.matcher(values[i]);
                if(m.matches()) {
                    if(m.group(0).equals("On")) {
                        onOffPref.setPreferenceValueIndex(0);
                    } else if(m.group(0).equals("Off")) {
                        onOffPref.setPreferenceValueIndex(1);
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

    // Generate patten: vibrate={true|false}
    private String parseExtra(String value, String key) {
        String result = "";
        Pattern p = Pattern.compile(key + "\\s*=(\\w)+$");
        Matcher m = p.matcher(value);
        if(m.matches()) {
            result = m.group(0);
        }
        return result;
    }
}
