package org.startsmall.openalarm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

class Notification {
    private static final String TAG = "Notification";
    private static Notification sInstance;

    private NotificationManager mNotificationManager;
    private long mNextSchedule = Long.MAX_VALUE;

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
    public Alarm set(Context context) {
        if (mNotificationManager == null) {
            mNotificationManager =
                (NotificationManager)context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
        }

        // Iterate all enabled alarms and find out which one is
        // the next.
        GetNextAlarm getNextAlarm = new GetNextAlarm();
        Alarm.foreach(context, getNextAlarm);
        Alarm nextAlarm = getNextAlarm.result;

        if (nextAlarm == null) {
            if (!getNextAlarm.hasEnabledAlarms) {
                mNotificationManager.cancel(0);
                setStatusBarIcon(context, false);
                Settings.System.putString(context.getContentResolver(),
                                          Settings.System.NEXT_ALARM_FORMATTED, "");
            }
        } else {
            long nextSchedule = nextAlarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS);
            if (nextSchedule != mNextSchedule) {
                // Intent intent = new Intent();
                // intent.setClass(context, OpenAlarm.class);
                // PendingIntent intentSender =
                //     PendingIntent.getActivity(context, 0,
                //                               intent,
                //                               PendingIntent.FLAG_CANCEL_CURRENT);

                // String tickerText =
                //     context.getString(R.string.alarm_set_notification_ticker,
                //                       nextAlarm.getStringField(Alarm.FIELD_LABEL));
                // String contentText =
                //     context.getString(R.string.alarm_set_notification_content,
                //                       nextAlarm.formatSchedule(context));

                // android.app.Notification notification =
                //     new android.app.Notification(R.drawable.stat_notify_alarm,
                //                                  tickerText, System.currentTimeMillis());
                // notification.flags = android.app.Notification.FLAG_NO_CLEAR;
                // notification.setLatestEventInfo(context,
                //                                 tickerText,
                //                                 contentText,
                //                                 intentSender);
                mNotificationManager.cancel(0);
                // mNotificationManager.notify(0, notification);

                mNextSchedule = nextSchedule;

                setStatusBarIcon(context, true);

                // Put schedule of next alarm in system settings,
                Settings.System.putString(context.getContentResolver(),
                                          Settings.System.NEXT_ALARM_FORMATTED,
                                          nextAlarm.formatSchedule(context));
            }
        }

        return nextAlarm;
    }

    private Notification() {}

    /**
     * A alarm visitor that finds the next schedule alarm from
     * internal alarm cache map.
     *
     */
    private static class GetNextAlarm extends Alarm.AbsVisitor {
        Alarm result;
        boolean hasEnabledAlarms;
        private long mSchedule;

        public GetNextAlarm() {
            result = null;
            hasEnabledAlarms = false;
            mSchedule = Long.MAX_VALUE;
        }

        public void onVisit(final Alarm alarm) {
            boolean enabled = alarm.getBooleanField(Alarm.FIELD_ENABLED);
            long timeInMillis = alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS);
            if (enabled) {
                hasEnabledAlarms = true;
                if (timeInMillis < mSchedule) {
                    result = alarm;
                    mSchedule = timeInMillis;
                }
            }
        }
    }

    private static void setStatusBarIcon(Context context, boolean enabled) {
        String ACTION_ALARM_CHANGED = "android.intent.action.ALARM_CHANGED";
        Intent alarmChanged = new Intent(ACTION_ALARM_CHANGED);
        alarmChanged.putExtra("alarmSet", enabled);
        context.sendBroadcast(alarmChanged);
    }
}
