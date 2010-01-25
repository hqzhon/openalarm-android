package org.startsmall.openalarm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import java.util.Iterator;

class Notification {
    private static final String TAG = "Notification";
    private static Notification sInstance;
    private NotificationManager sNotificationManager;
    private PackageManager sPackageManager;

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

        // if (sPackageManager == null) {
        //     sPackageManager = context.getPackageManager();
        // }

        Alarms.GetNextAlarm getNextAlarm = new Alarms.GetNextAlarm();
        Alarm.foreach(context, getNextAlarm);

        if (getNextAlarm.alarm != null) {
            Log.e(TAG, "===> should show notification");

            Alarm alarm = getNextAlarm.alarm;

            Intent intent = new Intent();
            intent.setClass(context, OpenAlarm.class);
            PendingIntent intentSender =
                PendingIntent.getActivity(context, 0,
                                          intent,
                                          PendingIntent.FLAG_CANCEL_CURRENT);

            // String handler = alarm.getStringField(Alarm.FIELD_HANDLER);
            // String pkgName = handler.substring(0, handler.lastIndexOf('.'));
            // ActivityInfo info;
            // try {
            //     info =
            //         sPackageManager.getReceiverInfo(
            //             new ComponentName(pkgName, handler), 0);
            // } catch (PackageManager.NameNotFoundException e) {
            //     Log.e(TAG, "===> unable to get handler's information");
            //     return;
            // }

            // String tickerText = "Next alarm " + info.loadLabel(sPackageManager).toString();
            String tickerText = "Next alarm " + alarm.getStringField(Alarm.FIELD_LABEL);
            String contentText = "Scheduled at " + Alarms.formatSchedule(context, alarm);

            android.app.Notification notification =
                new android.app.Notification(R.drawable.stat_notify_alarm,
                                             tickerText, System.currentTimeMillis());
            notification.flags = android.app.Notification.FLAG_NO_CLEAR;
            notification.setLatestEventInfo(context,
                                            tickerText,
                                            contentText,
                                            intentSender);
            sNotificationManager.notify(0, notification);
        }
    }

    // public void cancel() {
    //     mNotificationManager.cancel(alarmId);
    // }

}
