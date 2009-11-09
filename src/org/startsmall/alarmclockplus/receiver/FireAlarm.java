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

        Log.d(TAG, "=================> FireAlarm.onCreate()");

        // Parse extras in the intent.
        Intent intent = getIntent();
        final int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);
        final String label = intent.getStringExtra(Alarms.AlarmColumns.LABEL);
        final long atTimeInMillis = intent.getLongExtra(Alarms.AlarmColumns.AT_TIME_IN_MILLIS, 0);

        // TODO:

        // Snooze this alarm makes the alarm to be postponded and
        // saved as a SharedPreferences.
        Button snoozeButton = (Button)findViewById(R.id.snooze);
        snoozeButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Save info to the SharedPreference to
                    // indicate that this alarm is snoozed.
                    snoozeAlarm(alarmId, atTimeInMillis);


                    // Stop the playback of ringtone.



                    finish();
                }
            });

        Button dismissButton = (Button)findViewById(R.id.dismiss);
        dismissButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissAlarm(alarmId);

                    // stop the playback of ringtone;
                }
            });
    }

    private void snoozeAlarm(final int alarmId,
                             final long atTimeInMillis) {
        Log.d(TAG, "=============> Snooze alarm with id=" + alarmId);

        SharedPreferences preferences =
            getSharedPreferences("SnoozedAlarm", 0);
        SharedPreferences.Editor preferenceEditor = preferences.edit();

        // Snooze this alarm
        preferenceEditor.putInt("id", alarmId);


        // // Start with current date and time.
        // Calendar calendar = getCalendarInstance();

        // // Snooze with default snooze period
        // calendar.add(Calendar.MINUTE, 7);

        // long time = calendar.getTimeInMillis();








        preferenceEditor.commit();

    }

    private void dismissAlarm(final int alarmId) {
        Log.d(TAG, "=============> Dismiss alarm id=" + alarmId
              + ", and re-arrange next alarm");

        // Clear any record about snoozing this alarm.
        getSharedPreferences("SnoozedAlarm", 0).edit().clear().commit();

        // Rearrange the next alarm.
        Alarms.setAlarm(this, Alarms.getAlarmUri(alarmId), true);
    }
}
