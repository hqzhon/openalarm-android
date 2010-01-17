/**
 * @file   BootReceiver.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Tue Nov 10 17:47:36 2009
 *
 * Receiver that will be called when user changes time, time
 * format (12-hour or 24-hour format) or device has been
 * rebooted.
 *
 * Don't move the package path from org.startsmall.openalarm to
 * any other directory.
 */

package org.startsmall.openalarm;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;
import java.util.Calendar;


public class BootReceiver extends BroadcastReceiver {
    // All enabled alarms should be rescheduled if system time
    // changed, time zone changed or machine rebooted.
    private static class ScheduleEnabledAlarm implements Alarms.OnVisitListener {
        public static int numberOfEnabledAlarms = 0;
        public static long nearestAlarmTime = Long.MAX_VALUE;

        @Override
        public void onVisit(final Context context,
                            final int id,
                            final String label,
                            final int hour,
                            final int minutes,
                            final long oldAtTimeInMillis,
                            final int repeatOnDaysCode,
                            final boolean enabled,
                            final String handler,
                            final String extra) {
            // @note We need to update every alarm in order
            // for AdapterView.bindView to update am/pm label
            // whether or not an alarm is enabled.
            // if (enabled) {
            //     // Cancel old alarm because it might be incorrect due
            //     // to the change of system time.
            //     Alarms.disableAlarm(context, id, handler);
            // }

            // Schedule a new time for this alarm.
            long atTimeInMillis =
                Alarms.calculateAlarmAtTimeInMillis(hour, minutes, repeatOnDaysCode);
            if (enabled) {
                Alarms.enableAlarm(context, id, label, atTimeInMillis,
                                   repeatOnDaysCode, handler, extra);

                if (atTimeInMillis < nearestAlarmTime) {
                    nearestAlarmTime = atTimeInMillis;
                }

                numberOfEnabledAlarms++;
            }

            ContentValues newValues = new ContentValues();
            newValues.put(Alarms.AlarmColumns.AT_TIME_IN_MILLIS, atTimeInMillis);
            Alarms.updateAlarm(context, Alarms.getAlarmUri(id), newValues);
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

        // Cancel any alarm that was snoozed into preference
        // because TIME is reset.
        Alarms.cancelSnoozedAlarm(context, -1);

        // Iterate all alarms and schedule enabled alarms.
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
                Alarms.setAlarmInSystemSettings(context, calendar);
            }
        } else {
            Alarms.setNotification(context, false);
            Alarms.setAlarmInSystemSettings(context, null);
        }

        calendar = Alarms.getCalendarInstance();
        Log.d(TAG, "onReceive() END " +
              DateUtils.formatDateTime(context,
                                       calendar.getTimeInMillis(),
                                       DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_SHOW_YEAR));
    }
}
