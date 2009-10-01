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


import android.provider.BaseColumns;
import android.net.Uri;


/**
 *
 *
 */
public class Alarms {

    /// Alarm alert action string.
    static final String ALARM_ACTION = "org.startsmall.alarmclockplus.ALARM_ALERT";

    /**
     *
     *
     */
    public static class AlarmColumns implements BaseColumns {
        public static final String CONTENT_URI = "content://org.startsmall.alarmclockplus";

        /// Default sort order
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        /**
         * Inherited fields : _ID
         *
         */

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
        static final String[] ALARM_QUERY_COLUMNS = {
            _ID, LABEL, HOUR, MINUTES, DAYS_OF_WEEK,
            ENABLED, VIBRATE, ALERT_URI};

        public static final int ALARM_PROJECTION_ID_INDEX = 0;
        public static final int ALARM_PROJECTION_LABEL_INDEX = 1;
        public static final int ALARM_PROJECTION_HOUR_INDEX = 2;
        public static final int ALARM_PROJECTION_MINUTES_INDEX = 3;
        public static final int ALARM_PROJECTION_DAYS_OF_WEEK_INDEX = 4;
        public static final int ALARM_PROJECTION_ENABLED_INDEX = 5;
        public static final int ALARM_PROJECTION_VIBRATE_INDEX = 6;
        public static final int ALARM_PROJECTION_ALERT_URI_INDEX = 7;
    }

    /**
     * TODO: Alarms convenient methods
     *
     */












}


