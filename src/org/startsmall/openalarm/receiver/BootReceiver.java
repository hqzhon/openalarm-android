package org.startsmall.openalarm;

import android.app.Service;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.util.Calendar;

/**
 * All enabled alarms should be rescheduled if system time
 * changed, time zone changed or machine rebooted.
 *
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Alarms.is24HourMode = Alarms.is24HourMode(context);

        // Redirect time-consuming alarm scheduling to BootService.
        final String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            context.startService(new Intent(context, BootService.class));
            return;
        }

        // Iterate all alarms in database and bring them into alive.
        ScheduleEnabledAlarm scheduleAlarm = new ScheduleEnabledAlarm();
        Alarm.foreach(context, Alarms.getAlarmUri(-1), scheduleAlarm);
        Notification.getInstance().set(context);
    }

    static class ScheduleEnabledAlarm extends Alarm.AbsVisitor {
        @Override
        public void onVisit(final Context context, final Alarm alarm) {
            boolean enabled = alarm.getBooleanField(Alarm.FIELD_ENABLED);

            // If @p alarm was snoozed, unsnoozed it and disable its
            // previously set schedule if required.
            if (alarm.isSnoozed(context)) {
                alarm.unsnooze(context);
                if (!enabled) {
                    // If alarm is enabled, previous schedule
                    // will be override by the following
                    // alarm.set(). We don't need to cancel it
                    // here.
                    alarm.cancel(context);
                }
            }

            // In fact, we don't need to update TIME_IN_MILLIS
            // field of every alarm. But if TIME_SET triggers
            // this receiver, we probably need to reflect change
            // of is24HourMode in our AM/PM label of alarm list
            // item in main activity.
            long when = alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS);
            if (alarm.schedule()) {
                // Alarm is scheduled successfully. Chances are
                // an alarm has invalid settings.
                when = alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS);
                if (enabled) {
                    alarm.set(context);
                }
            }
            alarm.update(context, when);
        }
    }

    public static class BootService extends Service {
        public void onStart(Intent intent, int startId) {
            handleStart(intent, startId);
        }

        public int onStartCommand(Intent intent, int flags, int startId) {
            handleStart(intent, startId);
            return START_STICKY;
        }

        private void handleStart(Intent intent, int startId) {
            // Iterate all alarms in database and bring them into alive.
            ScheduleEnabledAlarm scheduleAlarm = new ScheduleEnabledAlarm();
            Alarm.foreach(this, Alarms.getAlarmUri(-1), scheduleAlarm);
            Notification.getInstance().set(this);
        }

        public IBinder onBind(Intent i) { return null; }
    }

}
