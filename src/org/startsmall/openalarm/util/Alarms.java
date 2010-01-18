/**
 *  OpenAlarm - an extensible alarm for Android
 *  Copyright (C) 2010 Liu Yen-Liang (Josh)
 *
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @file   Alarms.java
 * @author  <yenliangl@gmail.com>
 * @date   Wed Sep 30 16:47:42 2009
 *
 * @brief  Utility class.
 *
 *
 */
package org.startsmall.openalarm;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.provider.Settings;
import android.net.Uri;
import android.util.Log;
import android.text.format.DateUtils;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.List;
import java.text.SimpleDateFormat;

public class Alarms {
    private static final String TAG = "Alarms";

    /**
     * Authority of this application.
     * <p>Value: org.startsmall.openalarm</p>
     */
    public static final String CONTENT_URI_AUTH = "org.startsmall.openalarm";

    /**
     * Content URI of this application.
     *
     * <p>Value: content://org.startsmall.openalarm</p>
     */
    public static final String CONTENT_URI = "content://" + CONTENT_URI_AUTH;

    /**
     * Content URI for all alarms
     *
     * <p>Value: content://org.startsmall.openalarm/alarms</p>
     */
    public static final String CONTENT_URI_PATH = "alarms";
    public static final String CONTENT_URI_ALL_ALARMS =
        CONTENT_URI + "/" + CONTENT_URI_PATH;

    /**
     * Content URI for a single alarm
     *
     * <p>Value: content://org.startsmall.openalarm/alarms/#</p>
     */
    public static final String CONTENT_URI_SINGLE_ALARM =
        CONTENT_URI_ALL_ALARMS + "/#";

    /**
     * Convenient java.util.Calendar instance.
     *
     */
    private static final Calendar CALENDAR = Calendar.getInstance();
    public static Calendar getCalendarInstance() {
        CALENDAR.setTimeInMillis(System.currentTimeMillis());
        return CALENDAR;
    }

    private static final String PREFERENCE_FILE_FOR_SNOOZED_ALARM = "snoozed_alarm";

    private static final String USER_APK_DIR = "/data/app";

    private static Alarm sSnoozedAlarm;

    /**
     * Constants used in content provider and SQLiteDatabase
     *
     */

    /**
     * Suppress default constructor for noninstantiability.
     */
    private Alarms() {}

    /**
     * Encoder/Decode repeat days.
     *
     */
    public static class RepeatWeekdays {
        //
        // 0x00 No days
        // 0x01 Calendar.SUNDAY
        // 0x02 Calendar.MONDAY
        // 0x04 Calendar.TUESDAY
        // 0x08 Calendar.WEDNESDAY
        // 0x10 Calendar.THURSDAY
        // 0x20 Calendar.FRIDAY
        // 0x40 Calendar.SATURDAY
        // 0x7F Everyday
        //

        // Suppress default constructor for noninstantiability.
        private RepeatWeekdays() {}

        public static boolean isSet(int code, int day) {
            return (code & encode(day)) > 0;
        }

        public static int set(int code, int day, boolean enabled) {
            if(enabled) {
                code = code | encode(day);
            } else {
                code = code & ~encode(day);
            }
            return code;
        }

        private static int encode(int day) {
            if(day < Calendar.SUNDAY || day > Calendar.SATURDAY) {
                throw new IllegalArgumentException(
                    "Weekday must be among SUNDAY to SATURDAY");
            }
            return (1 << (day - 1));
        }

        public static String toString(int code, String everyday, String notSet) {
            String result = "";
            if(code > 0) {
                if(code == 0x7F) { // b1111111
                    result = everyday;
                } else {
                    for(int i = 1; i < 8; i++) { // From SUNDAY to SATURDAY
                        if(isSet(code, i)) {
                            result =
                                result +
                                DateUtils.getDayOfWeekString(
                                    i,
                                    DateUtils.LENGTH_MEDIUM) +
                                " ";
                        }
                    }
                }
            } else {
                result = notSet;
            }
            return result;
        }

        public static List<String> toStringList(int code, String everyday, String notSet) {
            return Arrays.asList(
                toString(code, everyday, notSet).split(" "));
        }
    }

    /**
     * Get uri of an alarm with its ID given.
     *
     */
    public static Uri getAlarmUri(final int alarmId) {
        if(alarmId == -1) {
            return Uri.parse(CONTENT_URI_ALL_ALARMS);
        } else {
            return Uri.parse(CONTENT_URI_ALL_ALARMS + "/" + alarmId);
        }
    }

    /**
     * Get cursor of an alarm uri.
     *
     */
    public synchronized static Cursor getAlarmCursor(Context context,
                                                     Uri alarmUri) {
        return
            context.getContentResolver().query(
                alarmUri,
                AlarmColumns.QUERY_COLUMNS,
                null,
                null,
                AlarmColumns.DEFAULT_SORT_ORDER);
    }

    /**
     * Listener interface that is used to report settings for every alarm
     * existed on the database.
     *
     */
    public static interface OnVisitListener {
        void onVisit(final Context context, Alarm alarm);
    }

    /**
     * Helper class that stores settings of an alarm in public
     * feilds for outsider to access.
     *
     */
    public static class GetAlarm implements OnVisitListener {
        public Alarm alarm;

        @Override
        public void onVisit(final Context context, Alarm alarm) {
            this.alarm = alarm;
        }
    }

    /**
     * Iterate alarms stored in the Uri path.
     *
     */
    public static void forEachAlarm(final Context context,
                                    final Uri alarmUri,
                                    final OnVisitListener listener) {
        Cursor cursor = getAlarmCursor(context, alarmUri);
        if(cursor.moveToFirst()) {
            do {
                Alarm alarm = Alarm.getInstance(cursor);
                if(listener != null) {
                    listener.onVisit(context, alarm);
                }
            } while(cursor.moveToNext());
        }
        cursor.close();
    }

    /**
     * Update alarm's settings in content database
     *
     */
    // public synchronized static int updateAlarm(final Context context,
    //                                            final Uri alarmUri,
    //                                            final ContentValues newValues) {
    //     if(newValues == null) {
    //         return -1;
    //     }

    //     return context.getContentResolver().update(
    //         alarmUri, newValues, null, null);
    // }

    public static SharedPreferences getSharedPreferencesOfSnoozedAlarm(Context context) {
        return context.getSharedPreferences(PREFERENCE_FILE_FOR_SNOOZED_ALARM, 0);
    }

    /**
     * Get snoozed alarm
     *
     */
    public static Alarm getSnoozedAlarm(Context context) {
        SharedPreferences sharedPreferences =
            getSharedPreferencesOfSnoozedAlarm(context);
        final int snoozedAlarmId =
            sharedPreferences.getInt(AlarmColumns._ID, -1);

        if (snoozedAlarmId != -1) {
            return Alarm.getInstance(snoozedAlarmId);
        }

        return null;
    }

   /**
     * @return true if clock is set to 24-hour mode
     */
    public static boolean is24HourMode(final Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    /**
     * Format date and time
     *
     */
    public static String formatTime(String pattern,
                                    Calendar calendar) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern);
        return dateFormatter.format(calendar.getTime());
    }

    public static String formatDateTime(Context context, Alarm alarm) {
        StringBuilder sb = new StringBuilder();
        sb.append("id=").append(alarm.getIntField(Alarm.FIELD_ID))
            .append(", enabled=").append(alarm.getBooleanField(Alarm.FIELD_ENABLED))
            .append(", label=").append(alarm.getStringField(Alarm.FIELD_LABEL))
            .append(", schedule=").append(
                DateUtils.formatDateTime(
                    context,
                    alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS),
                    DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_CAP_AMPM|
                    DateUtils.FORMAT_SHOW_WEEKDAY|DateUtils.FORMAT_SHOW_YEAR))
            .append(", handler=").append(alarm.getStringField(Alarm.FIELD_HANDLER))
            .append(", extra=").append(alarm.getStringField(Alarm.FIELD_EXTRA));
        return sb.toString();
    }

    public static String formatSchedule(Context context, Alarm alarm) {
        return DateUtils.formatDateTime(
            context,
            alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS),
            DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_CAP_AMPM|
            DateUtils.FORMAT_SHOW_WEEKDAY|DateUtils.FORMAT_SHOW_YEAR);
    }

    /**
     * Put an notification on the status bar or remove it from
     * status bar if no enabled alarms.
     *
     */
    public static void setNotification(final Context context,
                                       final boolean enabled) {
        if (enabled) {
            broadcastAlarmChanged(context, true);
        } else {
            // If there are more than 2 alarms are still enabled,
            // we shouldn't remove notification from status bar.
            final int numberOfEnabledAlarms = getNumberOfEnabledAlarms(context);
            if (numberOfEnabledAlarms == 0) {
                broadcastAlarmChanged(context, false);
            }
        }
    }

    /**
     * Return the class object of an alarm handler.
     *
     */
    public static Class<?> getHandlerClass(final String handlerClassName)
        throws ClassNotFoundException {
        final int lastDotPos = handlerClassName.lastIndexOf('.');
        final String apkPaths =
            USER_APK_DIR + "/" + "org.startsmall.openalarm.apk:" + // myself
            // handlers defined by other developers
            USER_APK_DIR + "/" + handlerClassName.substring(0, lastDotPos) + ".apk";

        dalvik.system.PathClassLoader classLoader =
            new dalvik.system.PathClassLoader(
                apkPaths,
                ClassLoader.getSystemClassLoader());

        return Class.forName(handlerClassName, true, classLoader);
    }

    /**
     * Get ActivityInfo object of an alarm handler from PackageManager
     *
     */
    public static ActivityInfo getHandlerInfo(final PackageManager pm,
                                              final String handlerClassName)
        throws PackageManager.NameNotFoundException {

        Intent i = new Intent(Alarm.ACTION_HANDLE);
        i.addCategory(Intent.CATEGORY_ALTERNATIVE);

        // Search all receivers that can handle my alarms.
        Iterator<ResolveInfo> infoObjs = queryAlarmHandlers(pm).iterator();
        while (infoObjs.hasNext()) {
            ActivityInfo activityInfo = infoObjs.next().activityInfo;
            if (activityInfo.name.equals(handlerClassName)) {
                return activityInfo;
            }
        }
        throw new PackageManager.NameNotFoundException(
            "BroadcastReceiver " + handlerClassName + " not found");
    }

    public static List<ResolveInfo> queryAlarmHandlers(final PackageManager pm) {
        Intent i = new Intent(Alarm.ACTION_HANDLE);
        i.addCategory(Intent.CATEGORY_ALTERNATIVE);
        return pm.queryBroadcastReceivers(i, 0);
    }

    private synchronized static int getNumberOfEnabledAlarms(Context context) {




        Cursor c =
            context.getContentResolver().query(
                getAlarmUri(-1),
                new String[]{AlarmColumns._ID, AlarmColumns.ENABLED},
                AlarmColumns.ENABLED + "=1",
                null,
                AlarmColumns.DEFAULT_SORT_ORDER);
        final int count = c.getCount();
        c.close();
        return count;
    }

    private static void broadcastAlarmChanged(Context context,
                                              boolean enabled) {
        final String ACTION_ALARM_CHANGED = "android.intent.action.ALARM_CHANGED";
        Intent i = new Intent(ACTION_ALARM_CHANGED);
        i.putExtra("alarmSet", enabled);
        context.sendBroadcast(i);
    }

    // Iterate all alarms and find out the nearest alarm.
    // private static void updateSystemSetting(final Context context) {
    //     class GetNearestAlarmTime implements OnVisitListener {
    //         public long timeInMillis = Long.MAX_VALUE;

    //         @Override
    //         public void onVisit(final Context context, Alarm alarm) {
    //             long temp = alarm.getLongField(Alarm.FIELD_TIME_IN_MILLIS);
    //             boolean enabled = alarm.getBooleanField(Alarm.FIELD_ENABLED);
    //             if (enabled && temp < timeInMillis) {
    //                 timeInMillis = temp;
    //             }
    //         }
    //     }

    //     GetNearestAlarmTime getNearestAlarm = new GetNearestAlarmTime();
    //     forEachAlarm(context, getAlarmUri(-1), getNearestAlarm);

    //     if (getNearestAlarm.timeInMillis != Long.MAX_VALUE) {
    //         Calendar calendar = getCalendarInstance();
    //         calendar.setTimeInMillis(getNearestAlarm.timeInMillis);
    //         postNextAlarmFormattedSetting(context, calendar);
    //     } else {
    //         postNextAlarmFormattedSetting(context, null);
    //     }
    // }

    /**
     * Post next alarm in system settings provider.
     *
     */
    public static void postNextAlarmFormattedSetting(final Context context,
                                                     final Calendar calendar) {
        String timeString = "";
        if (calendar != null) {
            timeString =
                DateUtils.formatDateTime(
                    context,
                    calendar.getTimeInMillis(),
                    DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_CAP_AMPM|DateUtils.FORMAT_SHOW_WEEKDAY|DateUtils.FORMAT_SHOW_YEAR);
        }

        Settings.System.putString(context.getContentResolver(),
                                  Settings.System.NEXT_ALARM_FORMATTED,
                                  timeString);
    }
}
