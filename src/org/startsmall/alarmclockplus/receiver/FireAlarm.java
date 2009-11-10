/**
 * @file   FireAlarm.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Tue Nov  3 20:33:08 2009
 *
 * @brief An activity that is launched when an alarm goes off.
 *
 *
 */
package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.R;
import org.startsmall.alarmclockplus.Alarms;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.util.Log;
import android.widget.Button;
import android.view.View;

public class FireAlarm extends Activity {
    private static final String TAG = "FireAlarm";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fire_alarm);

        // Snooze this alarm makes the alarm to be postponded and
        // saved as a SharedPreferences.
        Button snoozeButton = (Button)findViewById(R.id.snooze);
        snoozeButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Alarms.snoozeAlarm(FireAlarm.this, getIntent(), 2);

                    // Stop the playback of ringtone.

                    finish();
                }
            });

        Button dismissButton = (Button)findViewById(R.id.dismiss);
        dismissButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissAlarm();

                    // stop the playback of ringtone;

                    finish();
                }
            });
    }

    private void dismissAlarm() {
        final Intent intent = getIntent();
        final int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);

        Log.d(TAG, "===> dismissAlarm(): alarm id=" + alarmId);

        // The snoozing alarm is enabled during this lifetime of
        // this activity. Its settings remain the same. Dimiss
        // this alarm means we should try to calculate the new
        // time of the alarm again.

        // Deactivate the old alarm. The explicit class field of
        // the Intent was set to this activity when setting alarm
        // in AlarmManager..
        Alarms.setAlarm(this, intent, false);

        // Activate the alarm according to the new time.
        intent.setClassName(this,
                            getPackageName() + ".receiver.ActionDispatcher");

        final int hourOfDay = intent.getIntExtra(Alarms.AlarmColumns.HOUR, -1);
        final int minutes = intent.getIntExtra(Alarms.AlarmColumns.MINUTES, -1);
        final int repeatOnDaysCode = intent.getIntExtra(Alarms.AlarmColumns.REPEAT_DAYS, -1);
        long atTimeInMillis =
            Alarms.calculateAlarmAtTimeInMillis(hourOfDay, minutes,
                                                repeatOnDaysCode);
        intent.putExtra(Alarms.AlarmColumns.AT_TIME_IN_MILLIS, atTimeInMillis);
        Alarms.setAlarm(this, intent, true);

        ContentValues newValues = new ContentValues();
        newValues.put(Alarms.AlarmColumns.AT_TIME_IN_MILLIS, atTimeInMillis);
        Alarms.updateAlarm(this, Alarms.getAlarmUri(alarmId), newValues);
        Alarms.setNotification(this, intent, true);
    }
}
