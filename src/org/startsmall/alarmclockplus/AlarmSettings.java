/**
 * @file   AlarmSettings.java
 * @author  <josh@alchip.com>
 * @date   Wed Oct  7 16:05:00 2009
 *
 * @brief  Settings for an alarm.
 *
 *
 */
package org.startsmall.alarmclockplus;

import android.os.Bundle;
import android.content.Intent;
import android.database.Cursor;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.TextView;

public class AlarmSettings extends PreferenceActivity {
    private static final String TAG = "AlarmSettings";

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.alarm_settings);

        // If this is setting for new alarm
        Intent intent = getIntent();
        int alarmId =
            intent.getIntExtra(Alarms.INTENT_EXTRA_ALARM_ID_KEY, -1);
        if(alarmId == -1) {
            // Settings for new alarm. Populate fields with
            // default values.

        } else {
            Cursor cursor = Alarms.getAlarm(getContentResolver(), alarmId);
            if(cursor.moveToFirst() == false) {
                // empty cursor;
                return;
            }
            populateFields(cursor);
        }
    }

    private void populateFields(Cursor cursor) {
        final String label =
            cursor.getString(Alarms.AlarmColumns.PROJECTION_LABEL_INDEX);
        final int hour =
            cursor.getInt(Alarms.AlarmColumns.PROJECTION_HOUR_INDEX);
        final int minutes =
            cursor.getInt(Alarms.AlarmColumns.PROJECTION_MINUTES_INDEX);

        final boolean vibrate =
            cursor.getInt(Alarms.AlarmColumns.PROJECTION_VIBRATE_INDEX) == 1;

        setTitle("Settings for " + label);

        PreferenceManager prefManager = getPreferenceManager();
        Preference labelPref = prefManager.findPreference(
            getResources().getString(R.string.alarm_settings_label_key));
        labelPref.setSummary(label);

        Preference timePref = prefManager.findPreference(
            getResources().getString(R.string.alarm_settings_time_key));
        ((AlarmTimePreference)timePref).setTime(hour * 100 + minutes);
    }
}
