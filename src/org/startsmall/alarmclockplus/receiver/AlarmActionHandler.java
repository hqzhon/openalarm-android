package org.startsmall.alarmclockplus.receiver;

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

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> My AlarmActionHandler.onReceive() haha");
    }

    @Override
    public void addMyPreferences(Context context,
                                 PreferenceCategory category,
                                 String defaultValue) {
        CheckBoxPreference vibratePref = new CheckBoxPreference(context);
        vibratePref.setKey("vibrate");
        vibratePref.setPersistent(true);
        vibratePref.setTitle(R.string.alarm_extra_settings_vibrate_title);

        if(!TextUtils.isEmpty(defaultValue)) {
            String[] values = TextUtils.split(defaultValue, ";");
            for(int i = 0; i < values.length; i++) {
                if(TextUtils.isEmpty(values[i])) {
                    continue;
                }

                Pattern p = Pattern.compile("vibrate\\s*=(\\w)+$");
                Matcher m = p.matcher(values[i]);
                if(m.matches()) {
                    vibratePref.setChecked(
                        Boolean.parseBoolean(m.group(0)));
                }
            }
        }
        category.addPreference(vibratePref);
    }
}
