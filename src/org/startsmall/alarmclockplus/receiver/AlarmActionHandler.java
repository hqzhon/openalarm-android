package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.R;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.util.Log;

public class AlarmActionHandler extends ActionHandler {
    private static final String TAG = "AlarmActionHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> My AlarmActionHandler.onReceive() haha");
    }

    @Override
    public void addMyPreferences(Context context, PreferenceCategory category) {
        Log.d(TAG, "===>addMyPreferences");

        CheckBoxPreference vibratePref = new CheckBoxPreference(context);
        vibratePref.setPersistent(true);
        vibratePref.setTitle(R.string.alarm_extra_settings_vibrate_title);
        category.addPreference(vibratePref);
    }
}
