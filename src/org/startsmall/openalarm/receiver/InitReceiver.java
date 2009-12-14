/**
 * @file   InitReceiver.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Tue Nov 10 17:47:36 2009
 *
 * @brief Receiver that will be called when user changes time, time format (12-hour or 24-hour format) or device has been rebooted.
 *
 *
 */

package org.startsmall.openalarm.receiver;

import org.startsmall.openalarm.Alarms;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class InitReceiver extends BroadcastReceiver {
    // All enabled alarms should be rescheduled after system's time
    // changed, time zone changed or machine rebooted.
    private class ScheduleEnabledAlarm implements Alarms.OnVisitListener {
        @Override
        public void onVisit(final Context context,
                            final int id,
                            final String label,
                            final int hour,
                            final int minutes,
                            final int oldAtTimeInMillis,
                            final int repeatOnDaysCode,
                            final boolean enabled,
                            final String handler,
                            final String extra) {
            // Note that we need to update every alarm in order
            // for AdapterView.bindView to update am/pm label
            // whether or not an alarm is enabled.
            if (enabled) {
                Log.d(TAG, "Alarm " + label
                      + ",id=" + id
                      + ",hour=" + hour + ", minutes=" + minutes
                      + ",handler=" + handler
                      + ",extra=" + extra);

                // Cancel old alarm because it might be incorrect due
                // to the change of system time.
                Alarms.disableAlarm(context, id, handler);
            }

            // Re-schedule new time.
            long atTimeInMillis = Alarms.calculateAlarmAtTimeInMillis(hour, minutes, repeatOnDaysCode);
            if (enabled) {
                Alarms.enableAlarm(context, id, label, handler,
                                   atTimeInMillis, extra);
            }

            ContentValues newValues = new ContentValues();
            newValues.put(Alarms.AlarmColumns.AT_TIME_IN_MILLIS,
                          atTimeInMillis);
            Alarms.updateAlarm(context, Alarms.getAlarmUri(id), newValues);
            if (enabled) {
                Alarms.setNotification(context, true);
            }
        }
    }

    private static final String TAG = "InitReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "===> InitReceiver.onReceive() at " +
              Alarms.formatTime("yyyy:HH:mm",
                                Alarms.getCalendarInstance()));

        // Cancel any alert that was snoozed into preference.
        Alarms.cancelSnoozedAlarm(context, -1);

        // Iterate all alarms and re-schedule all enabled alarms.
        ScheduleEnabledAlarm scheduleAlarm = new ScheduleEnabledAlarm();
        Alarms.forEachAlarm(context, Alarms.getAlarmUri(-1), scheduleAlarm);

    }
}
