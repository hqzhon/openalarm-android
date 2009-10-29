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
            String state = parseStateFromExtra(extra, KEY);
            if(state.equals("On")) {
                setWifiEnabled(context, true);
            } else if(state.equals("Off")) {
                setWifiEnabled(context, false);
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
                        getDefaultCheckedIndex((String)getPreferenceValue()),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d,
                                                int which) {
                                setPreferenceValueIndex(which);
                                setSummary(null);
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

            int getDefaultCheckedIndex(String state) {
                if(!TextUtils.isEmpty(state)) {
                    if(state.equals("On")) {
                        return 0;
                    } else if(state.equals("Off")) {
                        return 1;
                    }
                }
                return -1;
            }
        }

        OnOffPreference onOffPref = new OnOffPreference(context);
        onOffPref.setKey(KEY);
        onOffPref.setPersistent(true);
        onOffPref.setDefaultValue("On");
        onOffPref.setTitle(R.string.alarm_extra_settings_wifi_title);

        if(!TextUtils.isEmpty(defaultValue)) {
            String state = parseStateFromExtra(defaultValue, KEY);
            if(state.equals("On")) {
                onOffPref.setPreferenceValueIndex(0);
            } else if(state.equals("Off")) {
                onOffPref.setPreferenceValueIndex(1);
            }
        }

        // If still no preference value set through defaultValue,
        // prompt to select in summary
        if(TextUtils.isEmpty((String)onOffPref.getPreferenceValue())) {
            onOffPref.setSummary("Do you want to set Wi-Fi on or off?");
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

    // Generate patten: key=value
    private String parseStateFromExtra(String appData, String key) {
        String[] values = TextUtils.split(appData, ";");
        for(int i = 0; i < values.length; i++) {
            String value = values[i];
            if(TextUtils.isEmpty(value) ||
               !value.matches(key + "=(\\w+)")) {
                continue;
            }

            Pattern p = Pattern.compile("=\\w+");
            Matcher m = p.matcher(value);
            if(m.find()) {
                return m.group().substring(1);
            }
        }
        return "";
    }
}
