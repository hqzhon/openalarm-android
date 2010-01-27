package org.startsmall.openalarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
// import android.util.Log;
// import java.util.Calendar;

public class BootService extends Service {
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

            // @note Update alarm in order for bindView() to
            // update am/pm label whether alarm is enabled.
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

    // private static final String TAG = "BootService";

    @Override
    public void onStart(Intent intent, int startId) {
        // Iterate all alarms in content DB and bring them into alive.
        ScheduleEnabledAlarm scheduleAlarm = new ScheduleEnabledAlarm();
        Alarm.foreach(this, Alarms.getAlarmUri(-1), scheduleAlarm);

        Notification.getInstance().set(this);

        // Stop this service.
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent i) {return null;}
}
