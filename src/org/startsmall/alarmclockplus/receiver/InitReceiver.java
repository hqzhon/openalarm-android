/**
 * @file   InitReceiver.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Tue Nov 10 17:47:36 2009
 *
 * @brief Receiver that gets executed when user changes time, time zone or machine rebooted.
 *
 *
 */

package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.Alarms;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;

public class InitReceiver extends BroadcastReceiver {
    // All enabled alarms should be rescheduled after system's time
    // changed, time zone changed or machine rebooted.
    private class ScheduleEnabledAlarm implements Alarms.OnVisitListener {
        private Intent mIntent;

        public ScheduleEnabledAlarm(Intent i) {
            mIntent = i;
        }

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
            if(!enabled) {
                return;
            }

            // Cancel old alarm because it might be incorrect due
            // to the change of system time.
            mIntent.setData(Alarms.getAlarmUri(id));
            Alarms.setAlarm(context, mIntent, false);

            // Re-schedule new time.
            mIntent.putExtra(Alarms.AlarmColumns._ID, id);
            mIntent.putExtra(Alarms.AlarmColumns.LABEL, label);
            mIntent.putExtra(Alarms.AlarmColumns.HANDLER, handler);
            mIntent.putExtra(Alarms.AlarmColumns.EXTRA, extra);
            long atTimeInMillis = Alarms.calculateAlarmAtTimeInMillis(hour, minutes, repeatOnDaysCode);
            mIntent.putExtra(Alarms.AlarmColumns.AT_TIME_IN_MILLIS, atTimeInMillis);
            Alarms.setAlarm(context, mIntent, true);
        }
    }

    private static final String TAG = "InitReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "===> InitReceiver.onReceive() at " +
              Alarms.formatDate("YYYY:HH:mm",
                                Alarms.getCalendarInstance()));

        // Cancel any snoozed alarm.
        Alarms.cancelSnoozedAlarm(context, -1);

        // Iterate all alarms and re-schedule all enabled alarms.
        Intent i = new Intent(Alarms.DISPATCH_ACTION);
        ScheduleEnabledAlarm scheduleAlarm = new ScheduleEnabledAlarm(i);
        Alarms.forEachAlarm(context, Alarms.getAlarmUri(-1), scheduleAlarm);
    }
}
