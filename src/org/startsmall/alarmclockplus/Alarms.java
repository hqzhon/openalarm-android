/**
 * @file   Alarms.java
 * @author  <yenliangl@gmail.com>
 * @date   Wed Sep 30 16:47:42 2009
 *
 * @brief  Convenient class for alarm settings.
 *
 *
 */
package org.startsmall.alarmclockplus;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.net.Uri;
import android.util.Log;
import android.text.format.DateUtils;

import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 *
 *
 */
public class Alarms {
    private static final String TAG = "Alarms";

    /******************************************************************
     * Convenient data that are used throughout the application       *
     ******************************************************************/

    /**
     * Authority of this application.
     * <p>Value: org.startsmall.alarmclockplus</p>
     */
    public static final String CONTENT_URI_AUTH = "org.startsmall.alarmclockplus";

    /**
     * Alarm alert action string.
     * <p>Value: org.startsmall.alarmclockplus.ALARM_ALERT</p>
     */
    public static final String ALARM_ACTION = CONTENT_URI_AUTH + ".ALARM_ACTION";

    /**
     * Action used to launch ActionDispatcher receiver.
     * <p>Value: org.startsmall.alarmclockplus.DISPATCH_ACTION</p>
     */
    private static final String DISPATCH_ACTION = CONTENT_URI_AUTH + ".DISPATCH_ACTION";

    /**
     * Content URI of this application.
     *
     * <p>Value: content://org.startsmall.alarmclockplus</p>
     */
    public static final String CONTENT_URI = "content://" + CONTENT_URI_AUTH;

    /**
     * Content URI for all alarms
     *
     * <p>Value: content://org.startsmall.alarmclockplus/alarms</p>
     */
    public static final String CONTENT_URI_PATH = "alarms";
    public static final String CONTENT_URI_ALL_ALARMS =
        CONTENT_URI + "/" + CONTENT_URI_PATH;

    /**
     * Content URI for a single alarm
     *
     * <p>Value: content://org.startsmall.alarmclockplus/alarms/#</p>
     */
    public static final String CONTENT_URI_SINGLE_ALARM =
        CONTENT_URI_ALL_ALARMS + "/#";


    public static long sBootWallTimeInMillis = 0;

    /*****************************************************************
     * For constants used in content provider and SQLiteDatabase.    *
     *****************************************************************/
    public static class AlarmColumns implements BaseColumns {

        /// Default sort order
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        //// Inherited fields : _ID

        /// Label of this alarm
        public static final String LABEL = "label";

        /// Hour in 24-hour (0 - 23)
        public static final String HOUR = "hour";

        /// Minutes (0 - 59)
        public static final String MINUTES = "minutes";

        /// Days this alarm works in this week.
        public static final String REPEAT_DAYS = "repeat_days";

        /// Whether or not this alarm is active currently
        public static final String ENABLED = "enabled";

        /// Whether or not this alarm vibrate
        public static final String VIBRATE = "vibrate";

        /// Audio to play when alarm triggers.
        public static final String ALERT_URI = "alert_uri";

        /**
         * Columns that will be pulled from a row. These should
         * be in sync with PROJECTION indexes.
         *
         */
        private static final String[] QUERY_COLUMNS = {
            _ID, LABEL, HOUR, MINUTES, REPEAT_DAYS,
            ENABLED, VIBRATE, ALERT_URI};

        /**
         *
         */
        public static final int PROJECTION_ID_INDEX = 0;
        public static final int PROJECTION_LABEL_INDEX = 1;
        public static final int PROJECTION_HOUR_INDEX = 2;
        public static final int PROJECTION_MINUTES_INDEX = 3;
        public static final int PROJECTION_REPEAT_DAYS_INDEX = 4;
        public static final int PROJECTION_ENABLED_INDEX = 5;
        public static final int PROJECTION_VIBRATE_INDEX = 6;
        public static final int PROJECTION_ALERT_URI_INDEX = 7;
    }

    /******************************************************************
     * Key strings used in Intent data.                               *
     ******************************************************************/
    public static final String INTENT_EXTRA_ALARM_ID_KEY = "alarm_id";
    public static final String INTENT_EXTRA_ALARM_ACTION_KEY = "alarm_action";

    public static class RepeatWeekdays {
        /**
         * 0x01 Calendar.SUNDAY
         * 0x02 Calendar.MONDAY
         * 0x04 Calendar.TUESDAY
         * 0x08 Calendar.WEDNESDAY
         * 0x10 Calendar.THURSDAY
         * 0x20 Calendar.FRIDAY
         * 0x40 Calendar.SATURDAY
         *
         * 0x7F On everyday
         */
        private int mDays;

        public static RepeatWeekdays getInstance() {
            return new RepeatWeekdays();
        }
        public static RepeatWeekdays getInstance(int daysCode) {
            return new RepeatWeekdays(daysCode);
        }

        private RepeatWeekdays() {}
        private RepeatWeekdays(int daysCode) {
            mDays = daysCode;
        }

        public boolean isEmpty() {
            return mDays == 0;
        }
        public void reset() {
            mDays = 0;
        }

        public boolean hasDay(int day) {
            return (mDays & getCode(day)) > 0;
        }

        public void set(int day, boolean enabled) {
            if(enabled) {
                mDays = mDays | getCode(day);
            } else {
                mDays = mDays & ~getCode(day);
            }
        }

        private int getCode(int day) {
            if(day < Calendar.SUNDAY || day > Calendar.SATURDAY) {
                throw new IllegalArgumentException("Weekday must be at SUNDAY to SATURDAY");
            }
            return (1 << (day - 1));
        }

        public int getCode() {
            return mDays;
        }

        public void setCode(int daysCode) {
            mDays = daysCode;
        }

        public String toString() {
            String result = "";
            if(mDays > 0) {
                if(mDays == 0x7F) { // b1111111
                    result = "On Everyday";
                } else {
                    for(int i = 1; i < 8; i++) { // From SUNDAY to SATURDAY
                        if(hasDay(i)) {
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
                result = "No days";
            }
            return result;
        }
    }

    /******************************************************************
     * Report alarms on the database
     ******************************************************************/
    public static Uri getAlarmUri(final long alarmId) {
        if(alarmId == -1) {
            return Uri.parse(CONTENT_URI_ALL_ALARMS);
        } else {
            return Uri.parse(CONTENT_URI_ALL_ALARMS + "/" + alarmId);
        }
    }

    public static Cursor getAlarmCursor(ContentResolver contentResolver,
                                        int alarmId) {
        Uri alarmUri;
        if(alarmId != -1) {
            alarmUri = getAlarmUri(alarmId);
        } else {
            alarmUri = Uri.parse(CONTENT_URI_ALL_ALARMS);
        }

        Log.d(TAG, "Get alarm " + alarmUri);

        return contentResolver.query(alarmUri,
                                     AlarmColumns.QUERY_COLUMNS,
                                     null,
                                     null,
                                     AlarmColumns.DEFAULT_SORT_ORDER);
    }

   /**
     * Listern interface that is used to report settings for every alarm
     * existed on the database.
     *
     */
    public static interface OnVisitListener {
        public void onVisit(final int id,
                            final String label,
                            final int hour,
                            final int minutes,
                            final int repeatOnDaysCode,
                            final boolean enabled,
                            final boolean vibrate,
                            final String alertUrl);
    }

    public static void forEachAlarm(ContentResolver contentResolver,
                                    Uri alarmUri,
                                    OnVisitListener listener) {
        Log.d(TAG, "vistAlarm(" + alarmUri.toString() + ")");
        Cursor cursor =
            contentResolver.query(alarmUri,
                                  AlarmColumns.QUERY_COLUMNS,
                                  null,
                                  null,
                                  AlarmColumns.DEFAULT_SORT_ORDER);
        if(cursor.moveToFirst()) {
            do {
                final int id =
                    cursor.getInt(AlarmColumns.PROJECTION_ID_INDEX);
                final String label =
                    cursor.getString(AlarmColumns.PROJECTION_LABEL_INDEX);
                final int hour =
                    cursor.getInt(AlarmColumns.PROJECTION_HOUR_INDEX);
                final int minutes =
                    cursor.getInt(AlarmColumns.PROJECTION_MINUTES_INDEX);
                final int repeatOnDaysCode =
                    cursor.getInt(AlarmColumns.PROJECTION_REPEAT_DAYS_INDEX);
                final boolean enabled =
                    cursor.getInt(AlarmColumns.PROJECTION_ENABLED_INDEX) == 1;
                final boolean vibrate =
                    cursor.getInt(AlarmColumns.PROJECTION_VIBRATE_INDEX) == 1;
                final String alertUrl =
                    cursor.getString(AlarmColumns.PROJECTION_ALERT_URI_INDEX);
                if(listener != null) {
                    listener.onVisit(id, label, hour, minutes,
                                     repeatOnDaysCode,
                                     enabled, vibrate, alertUrl);
                }
            }while(cursor.moveToNext());
        }
        cursor.close();
    }

    public static int deleteAlarm(ContentResolver contentResolver,
                                  int alarmId) {
        Uri alarmUri = Alarms.getAlarmUri(alarmId);

        Log.d(TAG, "Alarms.deleteAlarm(" + alarmUri + ")");

        return contentResolver.delete(alarmUri, null, null);
    }

    public static int updateAlarm(ContentResolver resolver,
                                  int id,
                                  String label,
                                  int hour,
                                  int minutes,
                                  int repeatOnDaysCode,
                                  boolean enabled,
                                  boolean vibrate,
                                  String alertUrl) {
        if(id < 0) {
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put(AlarmColumns._ID, id);
        values.put(AlarmColumns.LABEL, label);
        values.put(AlarmColumns.HOUR, hour);
        values.put(AlarmColumns.MINUTES, minutes);
        values.put(AlarmColumns.REPEAT_DAYS, repeatOnDaysCode);
        values.put(AlarmColumns.ENABLED, enabled);
        values.put(AlarmColumns.VIBRATE, vibrate);
        values.put(AlarmColumns.ALERT_URI, alertUrl);

        Uri uri = Alarms.getAlarmUri(id);
        return resolver.update(uri, values, null, null);
    }

    public static Uri newAlarm(ContentResolver resolver) {
        ContentValues values = new ContentValues();
        return resolver.insert(Uri.parse(CONTENT_URI_ALL_ALARMS),
                               values);
    }

    public static void scheduleAlarms(final Context context) {
        class Listener implements OnVisitListener {
            private Context mContext;
            Listener(Context context) {
                mContext = context;
            }

            public void onVisit(final int id,
                                final String label,
                                final int hour,
                                final int minutes,
                                final int repeatOnDaysCode,
                                final boolean enabled,
                                final boolean vibrate,
                                final String alertUrl) {
                if(!enabled) {
                    return;
                }
                activateAlarm(context, hour, minutes, repeatOnDaysCode, alertUrl);
            }
        }

        forEachAlarm(context.getContentResolver(),
                     getAlarmUri(-1),
                     new Listener(context));
    }

    public static void scheduleNextAlarm(Context context,
                                         final int alarmId) {
        Cursor cursor = getAlarmCursor(context.getContentResolver(),
                                       alarmId);
        if(!cursor.moveToFirst()) {
            return;
        }

        final boolean enabled =
            cursor.getInt(AlarmColumns.PROJECTION_ENABLED_INDEX) == 1;
        if(!enabled) {
            return;
        }

        final int hour =
            cursor.getInt(AlarmColumns.PROJECTION_HOUR_INDEX);
        final int minutes =
            cursor.getInt(AlarmColumns.PROJECTION_MINUTES_INDEX);
        final int repeatOnDaysCode =
            cursor.getInt(AlarmColumns.PROJECTION_REPEAT_DAYS_INDEX);
        final String alertUrl =
            cursor.getString(AlarmColumns.PROJECTION_ALERT_URI_INDEX);

        activateAlarm(context, hour, minutes, repeatOnDaysCode, alertUrl);
    }

    private static void activateAlarm(Context context,
                                      final int hour,
                                      final int minutes,
                                      final int repeatOnDaysCode,
                                      final String alertUrl) {
        Intent intent = new Intent(Alarms.DISPATCH_ACTION);
        intent.putExtra(INTENT_EXTRA_ALARM_ACTION_KEY, alertUrl);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        long alarmTimeInMillis = getNextAlarmInMillis(hour, minutes, repeatOnDaysCode);

        Log.d(TAG, "===> " + alarmTimeInMillis / 1000 + " seconds later to go off");

        // alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
        //                  alarmTimeInMillis,
        //                  PendingIntent.getBroadcast(
        //                      context, 0, intent,
        //                      PendingIntent.FLAG_CANCEL_CURRENT));
    }

    public static void disableAlarm(final Context context, final int alarmId) {
        class Listener implements OnVisitListener {
            public void onVisit(final int id,
                                final String label,
                                final int hour,
                                final int minutes,
                                final int repeatOnDaysCode,
                                final boolean enabled,
                                final boolean vibrate,
                                final String alertUrl) {
                updateAlarm(context.getContentResolver(),
                            id, label, hour, minutes,
                            repeatOnDaysCode, false,
                            vibrate, alertUrl);
            }
        }
        forEachAlarm(context.getContentResolver(), getAlarmUri(alarmId), new Listener());
    }

    private static long getNextAlarmInMillis(final int hourOfDay,
                                             final int minutes,
                                             final int repeatOnCode) {
        // Start with current date and time.
        Calendar calendar = Calendar.getInstance();

        int nowHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int nowMinutes = calendar.get(Calendar.MINUTE);

        if((hourOfDay < nowHourOfDay) ||
           ((hourOfDay == nowHourOfDay) && (minutes < nowMinutes))) {
            // If this alarm is behind current time. Move calendar to tomorrow.
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Align calendar's time with this alarm.
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Try to shift calendar by days in order to find the
        // nearest alarm.
        RepeatWeekdays repeatOn = RepeatWeekdays.getInstance(repeatOnCode);
        while(true) {
            if(repeatOn.hasDay(calendar.get(Calendar.DAY_OF_WEEK))) {
                break;
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long elapsedRTC = calendar.getTimeInMillis() - sBootWallTimeInMillis;

        Log.d(TAG, "===> next alarm " + calendar.getTimeInMillis() + " - " + sBootWallTimeInMillis);

        return elapsedRTC;
    }

    public static String formatDate(String pattern, Calendar calendar) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern);
        return dateFormatter.format(calendar.getTime());
    }














}
