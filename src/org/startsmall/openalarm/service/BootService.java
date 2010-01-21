package org.startsmall.openalarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.util.Calendar;

public class BootService extends Service {

    private static class ScheduleEnabledAlarm implements Alarms.OnVisitListener {
        public static int numberOfEnabledAlarms = 0;
        public static long nearestAlarmTime = Long.MAX_VALUE;

        @Override
        public void onVisit(final Context context, Alarm alarm) {
            boolean enabled = alarm.getBooleanField(Alarm.FIELD_ENABLED);

            // If @p alarm was snoozed, unsnoozed it and disable its
            // previously set schedule if required.
            if (alarm.isSnoozed(context)) {
                alarm.unsnooze(context);
                if (!enabled) {
                    // If alarm is enabled, previous schedule
                    // will be override by alarm.set(). We don't
                    // need to cancel it right away.
                    alarm.cancel(context);
                }
            }

            // @note Update alarm in order for bindView() to
            // update am/pm label whether alarm is enabled.
            long timeInMillis = alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS);
            if (alarm.schedule()) {
                // Alarm is scheduled successfully. Chances are
                // an alarm has invalid settings.
                timeInMillis = alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS);
                if (enabled) {
                    if (timeInMillis < nearestAlarmTime) {
                        nearestAlarmTime = timeInMillis;
                    }
                    numberOfEnabledAlarms++;

                    alarm.set(context);
                }
            }

            alarm.update(context, timeInMillis);
        }

    }

    private static final String TAG = "BootService";

    @Override
    public void onStart(Intent intent, int startId) {
        Calendar calendar = Calendar.getInstance();
        // Log.d(TAG, "onReceive() Start " + calendar);

        // Iterate all alarms in database and bring them into alive.
        ScheduleEnabledAlarm scheduleAlarm = new ScheduleEnabledAlarm();
        Alarms.forEachAlarm(this, Alarms.getAlarmUri(-1), scheduleAlarm);

        // If there is any enabled alarm, an notification should
        // be put on the status bar and update system settings by
        // nearest alarm.
        if (scheduleAlarm.numberOfEnabledAlarms > 0) {
            Alarms.setNotification(this, true);

            // Put the nearest alarm in the system settings.
            if (scheduleAlarm.nearestAlarmTime != Long.MAX_VALUE) {
                calendar.setTimeInMillis(scheduleAlarm.nearestAlarmTime);
                Alarms.postNextAlarmFormattedSetting(this, calendar);
            }
        } else {
            Alarms.setNotification(this, false);
            Alarms.postNextAlarmFormattedSetting(this, null);
        }

        calendar = Calendar.getInstance();
        // Log.d(TAG, "onReceive() END " + calendar);

        // Stop this service.
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent i) {return null;}
}
