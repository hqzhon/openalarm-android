package org.startsmall.openalarm;

import android.provider.BaseColumns;

class AlarmColumns implements BaseColumns {

    // Default sort order
    public static final String DEFAULT_SORT_ORDER = "_id ASC";

    /// Inherited fields : _ID

    // Label of this alarm
    public static final String LABEL = "label";

    // Hour in 24-hour (0 - 23)
    public static final String HOUR_OF_DAY = "hour";

    // Minutes (0 - 59)
    public static final String MINUTES = "minutes";

    // Go off time in milliseconds
    public static final String TIME_IN_MILLIS = "time";

    // Days this alarm works in this week.
    public static final String REPEAT_DAYS = "repeat_days";

    // Whether or not this alarm is active currently
    public static final String ENABLED = "enabled";

    // Audio to play when alarm triggers.
    public static final String HANDLER = "handler";

    // Audio to play when alarm triggers.
    public static final String EXTRA = "extra";

    // Columns that will be pulled from a row. These should
    // be in sync with PROJECTION indexes.
    public static final String[] QUERY_COLUMNS = {
        _ID, LABEL, HOUR_OF_DAY, MINUTES, TIME_IN_MILLIS, REPEAT_DAYS,
        ENABLED, HANDLER, EXTRA};

    public static final int PROJECTION_ID_INDEX = 0;
    public static final int PROJECTION_LABEL_INDEX = 1;
    public static final int PROJECTION_HOUR_OF_DAY_INDEX = 2;
    public static final int PROJECTION_MINUTES_INDEX = 3;
    public static final int PROJECTION_TIME_IN_MILLIS_INDEX = 4;
    public static final int PROJECTION_REPEAT_DAYS_INDEX = 5;
    public static final int PROJECTION_ENABLED_INDEX = 6;
    public static final int PROJECTION_HANDLER_INDEX = 7;
    public static final int PROJECTION_EXTRA_INDEX = 8;
}
