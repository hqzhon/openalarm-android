package org.startsmall.openalarm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
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

    /**
     * Notify user the next scheduled alarm on the status bar
     *
     */
    public void set(Context context) {
        if (sNotificationManager == null) {
            sNotificationManager =
                (NotificationManager)context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
        }
        sNotificationManager.cancel(0);

        // Iterate all enabled alarms and find out which one is
        // the next.
        GetNextAlarm getNextAlarm = new GetNextAlarm();
        Alarm.foreach(getNextAlarm);

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

            // Put schedule of next alarm in system settings,
            Settings.System.putString(context.getContentResolver(),
                                      Settings.System.NEXT_ALARM_FORMATTED,
                                      alarm.formatSchedule(context));
        }
    }

    private Notification() {}

    /**
     * A alarm visitor that finds the next schedule alarm from
     * internal alarm cache map.
     *
     */
    private static class GetNextAlarm extends Alarm.AbsVisitor {
        public Alarm alarm;

        private long mWhen = Long.MAX_VALUE;

        public void onVisit(final Alarm alarm) {
            boolean enabled = alarm.getBooleanField(Alarm.FIELD_ENABLED);
            long timeInMillis = alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS);
            if (enabled && timeInMillis < mWhen) {
                mWhen = timeInMillis;
                this.alarm = alarm;
            }
        }
    }
}
