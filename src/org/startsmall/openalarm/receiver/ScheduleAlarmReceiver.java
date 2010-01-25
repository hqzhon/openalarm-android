package org.startsmall.openalarm;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;
import java.util.Calendar;

public class ScheduleAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "ScheduleAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "===> onReceive(" + intent.getAction() + ") start: " + Calendar.getInstance());

        final int alarmId = intent.getIntExtra(AlarmColumns._ID, -1);

        // It is not impossible that some task manager killed
        // OpenAlarm and an installed alarm calls this receiver.
        // In this case, we should get alarm from DB not from
        // cache because cache was gone with killed OpenAlarm.
        Alarm alarm = Alarm.getInstance(context, alarmId);

        // Try to schedule the alarm.
        if (alarm.schedule()) {
            alarm.set(context);
            Log.i(TAG, "===> scheduled alarm: " + Alarms.formatDateTime(context, alarm));
        }
    }
}
