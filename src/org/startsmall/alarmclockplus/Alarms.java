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

import android.content.ContentResolver;
import android.content.ContentValues;
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

        public RepeatWeekdays() {}
        public RepeatWeekdays(int daysCode) {
            mDays = daysCode;
        }

        public void reset() {
            mDays = 0;
        }

        public boolean hasDay(int day) {
            return (mDays & getCode(day)) > 0;
        }

        public void addDay(int day) {
            mDays = mDays | getCode(day);
        }

        public void removeDay(int day) {
            mDays = mDays & ~getCode(day);
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
        return Uri.parse(CONTENT_URI_ALL_ALARMS + "/" + alarmId);
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
        public void onVisit(int id,
                            String label,
                            int hour, int minutes,
                            int repeatOnDaysCode,
                            boolean enabled,
                            boolean vibrate,
                            String alertUrl);
    }

    public static void visitAlarm(ContentResolver contentResolver,
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

    public static String formatDate(String pattern, Calendar calendar) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern);
        return dateFormatter.format(calendar.getTime());
    }














}
