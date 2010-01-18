package org.startsmall.openalarm;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;
import java.util.Calendar;

/**
 * All enabled alarms should be rescheduled if system time
 * changed, time zone changed or machine rebooted.
 *
 */
public class BootReceiver extends BroadcastReceiver {
    private static class ScheduleEnabledAlarm implements Alarms.OnVisitListener {
        public static int numberOfEnabledAlarms = 0;
        public static long nearestAlarmTime = Long.MAX_VALUE;

        @Override
        public void onVisit(final Context context, Alarm alarm) {
            // If @p alarm was snoozed, unsnoozed it and disable
            // previously set schedule.
            if (alarm.isSnoozed(context)) {
                alarm.unsnooze(context);
                alarm.cancel(context);
            }

            // @note Update alarm in order for bindView() to
            // update am/pm label whether alarm is enabled.
            alarm.schedule();

            long timeInMillis = alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS);
            boolean enabled = alarm.getBooleanField(Alarm.FIELD_ENABLED);
            if (enabled) {
                if (timeInMillis < nearestAlarmTime) {
                    nearestAlarmTime = timeInMillis;
                }
                numberOfEnabledAlarms++;

                alarm.set(context);
            }

            alarm.update(context, timeInMillis);
        }
    }

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Calendar calendar = Alarms.getCalendarInstance();

        Log.d(TAG, "onReceive() Start " +
              DateUtils.formatDateTime(context,
                                       calendar.getTimeInMillis(),
                                       DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR));

        // Iterate all alarms in database and bring them into alive.
        ScheduleEnabledAlarm scheduleAlarm = new ScheduleEnabledAlarm();
        Alarms.forEachAlarm(context, Alarms.getAlarmUri(-1), scheduleAlarm);

        // If there is any enabled alarm, an notification should
        // be put on the status bar and update system settings by
        // nearest alarm.
        if (scheduleAlarm.numberOfEnabledAlarms > 0) {
            Alarms.setNotification(context, true);

            // Put the nearest alarm in the system settings.
            if (scheduleAlarm.nearestAlarmTime != Long.MAX_VALUE) {
                calendar.setTimeInMillis(scheduleAlarm.nearestAlarmTime);
                Alarms.postNextAlarmFormattedSetting(context, calendar);
            }
        } else {
            Alarms.setNotification(context, false);
            Alarms.postNextAlarmFormattedSetting(context, null);
        }

        calendar = Alarms.getCalendarInstance();
        Log.d(TAG, "onReceive() END " +
              DateUtils.formatDateTime(context,
                                       calendar.getTimeInMillis(),
                                       DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR));
    }
}
