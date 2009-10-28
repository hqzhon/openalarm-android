package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.*;
import org.startsmall.alarmclockplus.R;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlarmActionHandler extends ActionHandler {
    private static final String TAG = "AlarmActionHandler";
    private static final String KEY = "vibrate";

    @Override
    public void onReceive(Context context, Intent intent) {
        final int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);
        final String label =
            intent.getStringExtra(Alarms.AlarmColumns.LABEL);

        // Parse extra settings out of combined value.
        final String extra =
            intent.getStringExtra(Alarms.AlarmColumns.EXTRA);
        boolean vibrate = false;
        if(!TextUtils.isEmpty(extra)) {
            String[] values = TextUtils.split(extra, ";");
            for(int i = 0; i < values.length; i++) {
                if(TextUtils.isEmpty(values[i])) {
                    continue;
                }

                String value = parseExtra(values[i], KEY);
                if(!TextUtils.isEmpty(value)) {
                    vibrate = Boolean.parseBoolean(value);
                }
            }
        }

        Log.v(TAG, "=========> AlarmActionHandler.onReceive(): "
              + "vibrate=" + vibrate);

    }

    @Override
    public void addMyPreferences(final Context context,
                                 final PreferenceCategory category,
                                 final String defaultValue) {
        CheckBoxPreference vibratePref = new CheckBoxPreference(context);
        vibratePref.setKey(KEY);
        vibratePref.setPersistent(true);
        vibratePref.setTitle(R.string.alarm_extra_settings_vibrate_title);

        if(!TextUtils.isEmpty(defaultValue)) {
            String[] values = TextUtils.split(defaultValue, ";");
            for(int i = 0; i < values.length; i++) {
                if(TextUtils.isEmpty(values[i])) {
                    continue;
                }

                String value = parseExtra(values[i], KEY);
                if(!TextUtils.isEmpty(value)) {
                    vibratePref.setChecked(Boolean.parseBoolean(value));
                }
            }
        }
        category.addPreference(vibratePref);
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
