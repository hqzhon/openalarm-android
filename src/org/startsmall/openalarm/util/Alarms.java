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

package org.startsmall.openalarm;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.text.format.DateUtils;
import android.text.TextUtils;

import java.util.*;
import java.text.SimpleDateFormat;

public class Alarms {

    public static boolean is24HourMode = false;

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

    // Cache of installed alarm action handler cache
    private static List<ResolveInfo> sHandlerList;

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
     * Return list of alarm handlers
     *
     */
    public static List<ResolveInfo> queryAlarmHandlers(final PackageManager pm, boolean requery) {
        Intent i = new Intent(Alarm.ACTION_HANDLE);
        i.addCategory(Intent.CATEGORY_ALTERNATIVE);
        if (sHandlerList == null || requery) {
            sHandlerList = pm.queryBroadcastReceivers(i, 0);
            Collections.sort(
                sHandlerList,
                new Comparator<ResolveInfo>() {
                    public int compare(ResolveInfo r1, ResolveInfo r2) {
                        String label1 = r1.loadLabel(pm).toString(),
                               label2 = r2.loadLabel(pm).toString();
                        return label1.compareTo(label2);
                    }
                });
        }
        return sHandlerList;
    }
}
