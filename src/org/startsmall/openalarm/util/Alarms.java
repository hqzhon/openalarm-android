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
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
        public static final int MONDAY    = 0;
        public static final int TUESDAY   = 1;
        public static final int WEDNESDAY = 2;
        public static final int THURSDAY  = 3;
        public static final int FRIDAY    = 4;
        public static final int SATURDAY  = 5;
        public static final int SUNDAY    = 6;

        // Suppress default constructor for noninstantiability.
        private RepeatWeekdays() {}

        public static boolean isSet(int code, int day) {
            return ((code >> day) & 1) == 1;
        }

        public static int set(int code, int day, boolean enabled) {
            return enabled ? (code | (1 << day)) : (code & ~(1 << day));
        }

        private static int getNumDaysSet(int code) {
            int count = 0;
            while (code > 0) {
                if ((code & 1) == 1) count++;
                code >>= 1;
            }
            return count;
        }

        private static final int EVERYDAY = 0x7F;
        public static String toString(Context context, int code) {
            if (code == 0) {
                return context.getString(R.string.never);
            } else if (code == EVERYDAY) {
                return context.getString(R.string.everyday);
            }

            Resources resources = context.getResources();
            int dayCount = getNumDaysSet(code);
            String[] dayList = dayCount > 1 ?
                               resources.getStringArray(R.array.days_of_week_short) :
                               resources.getStringArray(R.array.days_of_week);

            // Should we show long format or short format of weekdays,
            String dayConcat = context.getString(R.string.day_concat);
            String result;
            StringBuilder sb = new StringBuilder();
            for (int d = MONDAY; d <= SUNDAY; d++) {
                if (isSet(code, d)) {
                    sb.append(dayList[d]);
                    dayCount -= 1;
                    if (dayCount > 0) {
                        sb.append(dayConcat);
                    }
                }
            }

            return sb.toString();
        }

        public static ArrayList<String> toStringList(Context context, int code) {
            String dayConcat = context.getString(R.string.day_concat);
            String[] dayArray = toString(context, code).split(dayConcat);
            ArrayList<String> result = new ArrayList<String>(dayArray.length);
            Collections.addAll(result, dayArray);
            return result;
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
            return Alarm.getInstance(context, snoozedAlarmId);
        }

        return null;
    }

    public static void dismissAlarm(Context context, int alarmId) {
        Alarm alarm = Alarm.getInstance(context, alarmId);

        // Try to schedule the alarm.
        if (alarm.schedule()) {
            alarm.set(context);
            Notification.getInstance().set(context);
        }
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
