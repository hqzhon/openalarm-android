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
import android.database.Cursor;
import android.provider.BaseColumns;
import android.net.Uri;
import android.util.Log;

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
        public static final String DAYS_OF_WEEK = "daysofweek";

        /// Whether or not this alarm is active currently
        public static final String ENABLED = "enabled";

        /// Whether or not this alarm vibrate
        public static final String VIBRATE = "vibrate";

        /// Audio to play when alarm triggers.
        public static final String ALERT_URI = "alert_uri";

        /**
         * Projection indexes.
         *
         */
        private static final String[] QUERY_COLUMNS = {
            _ID, LABEL, HOUR, MINUTES, // TODO: DAYS_OF_WEEK,
            ENABLED, VIBRATE, ALERT_URI};

        /**
         *
         *
         */
        public static final int PROJECTION_ID_INDEX = 0;
        public static final int PROJECTION_LABEL_INDEX = 1;
        public static final int PROJECTION_HOUR_INDEX = 2;
        public static final int PROJECTION_MINUTES_INDEX = 3;
        // public static final int PROJECTION_DAYS_OF_WEEK_INDEX = 4;
        public static final int PROJECTION_ENABLED_INDEX = 4;
        public static final int PROJECTION_VIBRATE_INDEX = 5;
        public static final int PROJECTION_ALERT_URI_INDEX = 6;
    }

    /******************************************************************
     * Key strings used in Intent data.                               *
     ******************************************************************/
    public static final String INTENT_EXTRA_ALARM_ID_KEY = "alarm_id";


    /**
     *
     *
     * @param contentResolver
     *
     * @return database cursor
     */
    public static Cursor getAlarms(ContentResolver contentResolver) {
        return
            contentResolver.query(
                Uri.parse(CONTENT_URI_ALL_ALARMS),
                AlarmColumns.QUERY_COLUMNS,
                null,
                null,
                AlarmColumns.DEFAULT_SORT_ORDER);
    }

    public static Cursor getAlarm(ContentResolver contentResolver,
                                  int alarmId) {
        Uri alarmUrl =
            Uri.parse(CONTENT_URI_ALL_ALARMS + "/" + alarmId);

        Log.d(TAG, "getAlarm(" + alarmUrl.toString() + ")");
        return contentResolver.query(alarmUrl,
                                     AlarmColumns.QUERY_COLUMNS,
                                     null,
                                     null,
                                     AlarmColumns.DEFAULT_SORT_ORDER);
    }

    public static int deleteAlarm(ContentResolver contentResolver,
                                  final int alarmId) {
        Uri alarmUri =
            Uri.parse(CONTENT_URI_ALL_ALARMS + "/" + alarmId);

        return contentResolver.delete(alarmUri, null, null);
    }


    public static String formatDate(String pattern, Calendar calendar) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern);
        return dateFormatter.format(calendar.getTime());
    }
}
