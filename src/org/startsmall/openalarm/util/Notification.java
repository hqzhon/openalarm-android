package org.startsmall.openalarm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

class Notification {
    private static final String TAG = "Notification";
    private static Notification sInstance;
    private NotificationManager sNotificationManager;

    public static Notification getInstance() {
        if (sInstance == null) {
            sInstance = new Notification();
        }
        return sInstance;
    }

    private Notification() {}

    public void set(Context context) {
        if (sNotificationManager == null) {
            sNotificationManager =
                (NotificationManager)context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
        }
        sNotificationManager.cancelAll();

        Alarms.GetNextAlarm getNextAlarm = new Alarms.GetNextAlarm();
        Alarm.foreach(context, getNextAlarm);

        if (getNextAlarm.alarm != null) {
            Alarm alarm = getNextAlarm.alarm;

            Intent intent = new Intent();
            intent.setClass(context, OpenAlarm.class);
            PendingIntent intentSender =
                PendingIntent.getActivity(context, 0,
                                          intent,
                                          PendingIntent.FLAG_CANCEL_CURRENT);

            String tickerText =
                context.getString(R.string.alarm_set_notification_ticker,
                                  alarm.getStringField(Alarm.FIELD_LABEL));
            String contentText =
                context.getString(R.string.alarm_set_notification_content,
                                  alarm.formatSchedule(context));

            android.app.Notification notification =
                new android.app.Notification(R.drawable.stat_notify_alarm,
                                             tickerText, System.currentTimeMillis());
            notification.flags = android.app.Notification.FLAG_NO_CLEAR;
            notification.setLatestEventInfo(context,
                                            tickerText,
                                            contentText,
                                            intentSender);
            sNotificationManager.notify(0, notification);


            // Put next alarm in system settings,
            String timeString =
                DateUtils.formatDateTime(
                    context,
                    alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS),
                    DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_CAP_AMPM|DateUtils.FORMAT_SHOW_WEEKDAY|DateUtils.FORMAT_SHOW_YEAR);
            Settings.System.putString(context.getContentResolver(),
                                      Settings.System.NEXT_ALARM_FORMATTED,
                                      timeString);
        }
    }
}
