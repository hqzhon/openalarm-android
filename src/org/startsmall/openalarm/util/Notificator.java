package org.startsmall.openalarm;

import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

class Notificator {
    private final Context mContext;
    private final NotificationManager mNotificationManager;

    public Notificator(Context c) {
        mContext = c;
        mNotificationManager =
            (NotificationManager)mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    public void set(final int alarmId,
                    final int iconResId,
                    final String tickerText,
                    final String contentText) {
        Intent notificationIntent = new Intent();
        notificationIntent.setClassName(
            "org.startsmall.openalarm",
            "org.startsmall.openalarm.OpenAlarm");
        PendingIntent contentIntent =
            PendingIntent.getActivity(mContext, 0,
                                      notificationIntent,
                                      PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notification =
            new Notification(
                iconResId, tickerText, System.currentTimeMillis());
        notification.setLatestEventInfo(mContext,
                                        tickerText,
                                        contentText,
                                        contentIntent);
        mNotificationManager.notify(alarmId, notification);
    }

    public void cancel(final int alarmId) {
        mNotificationManager.cancel(alarmId);
    }

}
