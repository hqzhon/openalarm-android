package org.startsmall.openalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.Settings;
import android.net.Uri;
import android.text.format.DateUtils;
import android.text.TextUtils;
import android.util.Log;
import java.util.Calendar;
import java.util.HashMap;

class Alarm {
    /**
     * Alarm alert action string.
     * <p>Value: org.startsmall.openalarm.HANDLE_ALARM</p>
     */
    public static final String ACTION_HANDLE = Alarms.CONTENT_URI_AUTH + ".action.HANDLE_ALARM";

    public static final String ACTION_SCHEDULE = Alarms.CONTENT_URI_AUTH + ".action.SCHEDULE_ALARM";

    public static final int FIELD_ID = AlarmColumns.PROJECTION_ID_INDEX;
    public static final int FIELD_LABEL = AlarmColumns.PROJECTION_LABEL_INDEX;
    public static final int FIELD_HOUR_OF_DAY = AlarmColumns.PROJECTION_HOUR_OF_DAY_INDEX;
    public static final int FIELD_MINUTES = AlarmColumns.PROJECTION_MINUTES_INDEX;
    public static final int FIELD_TIME_IN_MILLIS = AlarmColumns.PROJECTION_TIME_IN_MILLIS_INDEX;
    public static final int FIELD_REPEAT_DAYS = AlarmColumns.PROJECTION_REPEAT_DAYS_INDEX;
    public static final int FIELD_ENABLED = AlarmColumns.PROJECTION_ENABLED_INDEX;
    public static final int FIELD_HANDLER = AlarmColumns.PROJECTION_HANDLER_INDEX;
    public static final int FIELD_EXTRA = AlarmColumns.PROJECTION_EXTRA_INDEX;

    public static final int ERROR_NONE = 0;
    public static final int ERROR_NO_HANDLER = 1;
    public static final int ERROR_NO_REPEAT_DAYS = 2;

    private static final String TAG = "Alarm";

    private static final HashMap<Integer, Alarm> sMap = new HashMap<Integer, Alarm>();

    // Properties of an alarm.
    private final int mId;
    private String mLabel;
    private int mHourOfDay;
    private int mMinutes;
    private long mTimeInMillis;
    private int mRepeatDays;
    private boolean mEnabled;
    private String mHandler;
    private String mExtra;

    /**
     * Get an brandnew alarm instance from database.
     *
     */
    public static Alarm getInstance(Context context) {
        Uri uri;
        ContentResolver cr = context.getContentResolver();
        synchronized (cr) {
            uri = cr.insert(Uri.parse(Alarms.CONTENT_URI_ALL_ALARMS), null);
        }
        final int id = Integer.parseInt(uri.getLastPathSegment());
        return getInstance(context, id);
    }

    /**
     * Get an alarm instance with its settings obtained from
     * database. If no such alarm with id exists in the database,
     * create a new one.
     *
     */
    public static Alarm getInstance(Context context, final int id) {
        Cursor cursor =
            Alarms.getAlarmCursor(context, Alarms.getAlarmUri(id));
        Alarm alarm;
        if (cursor.moveToFirst()) {
            alarm = getInstance(cursor);
        } else {
            alarm = getInstance(id);
        }
        cursor.close();
        return alarm;
    }

    /**
     * Get an alarm instance from internal cache. If no alarm
     * with such id exists in the cache, create a new one.
     *
     */
    public static Alarm getInstance(final int id) {
        if (id > -1) {
            Alarm alarm;
            if (sMap.containsKey(id)) {
                alarm = sMap.get(id);
            } else {
                alarm = new Alarm(id);
                sMap.put(id, alarm);
            }
            return alarm;
        }
        throw new IllegalArgumentException(
            "Alarm ID must be greater than zero");
    }

    /**
     * Get an alarm instance from cursor.
     *
     */
    public static Alarm getInstance(final Cursor cursor) {
        final int id = cursor.getInt(AlarmColumns.PROJECTION_ID_INDEX);
        final String label = cursor.getString(AlarmColumns.PROJECTION_LABEL_INDEX);
        final int hourOfDay = cursor.getInt(AlarmColumns.PROJECTION_HOUR_OF_DAY_INDEX);
        final int minutes = cursor.getInt(AlarmColumns.PROJECTION_MINUTES_INDEX);
        final int repeatDays = cursor.getInt(AlarmColumns.PROJECTION_REPEAT_DAYS_INDEX);
        final long timeInMillis = cursor.getLong(AlarmColumns.PROJECTION_TIME_IN_MILLIS_INDEX);
        final boolean enabled = cursor.getInt(AlarmColumns.PROJECTION_ENABLED_INDEX) == 1;
        final String handler = cursor.getString(AlarmColumns.PROJECTION_HANDLER_INDEX);
        final String extra = cursor.getString(AlarmColumns.PROJECTION_EXTRA_INDEX);

        Alarm alarm;
        if (sMap.containsKey(id)) {
            alarm = sMap.get(id);
        } else {
            alarm = new Alarm(id, label, hourOfDay, minutes, repeatDays, timeInMillis, enabled, handler, extra);
            sMap.put(id, alarm);
        }
        return alarm;
    }

    public Uri getUri() {
        return Alarms.getAlarmUri(mId);
    }

    /**
     * Get ENABLED field of the alarm.
     *
     */
    public boolean getBooleanField(int field) {
        if (field == FIELD_ENABLED) {
            return mEnabled;
        }
        throw new IllegalArgumentException("illegal argument: " + field);
    }

    /**
     * Get HOUR_OF_DAY, MINUTES or REPEAT_DAYS field of the alarm.
     *
     */
    public int getIntField(int field) {
        if (field == FIELD_ID) {
            return mId;
        } else if (field == FIELD_HOUR_OF_DAY) {
            return mHourOfDay;
        } else if (field == FIELD_MINUTES) {
            return mMinutes;
        } else if (field == FIELD_REPEAT_DAYS) {
            return mRepeatDays;
        }
        throw new IllegalArgumentException("illegal argument: " + field);
    }

    /**
     * Get TIME_IN_MILLIS field of this alarm.
     *
     */
    public long getLongField(int field) {
        if (field == FIELD_TIME_IN_MILLIS) {
            return mTimeInMillis;
        }
        throw new IllegalArgumentException("illegal argument: " + field);
    }

    /**
     * Get HANDLER or EXTRA field of this alarm.
     *
     */
    public String getStringField(int field) {
        if (field == FIELD_LABEL) {
            return mLabel;
        } else if (field == FIELD_HANDLER) {
            return mHandler;
        } else if (field == FIELD_EXTRA) {
            return mExtra;
        }
        throw new IllegalArgumentException("illegal argument: " + field);
    }

    /**
     * Whether the settings of this alarm are valid.
     *
     */
    public boolean isValid() {
        // Check the integrity of the alarm's settings.
        return getErrorCode() == ERROR_NONE;
    }

    /**
     * Returns error code of this alarm.
     *
     */
    public int getErrorCode() {
        if (TextUtils.isEmpty(mHandler)) {
            return ERROR_NO_HANDLER;
        } else if (mRepeatDays == 0) {
            return ERROR_NO_REPEAT_DAYS;
        }

        return ERROR_NONE;
    }

    /**
     * Set this alarm in AlarmManager.
     *
     */
    public void set(Context context) {
        Intent i = new Intent(ACTION_HANDLE, getUri());
        try {
            // The context the handler class is in might be
            // different than the one OpenAlarm is in. For
            // instance, ApnHandler and AlarmHandler are in two
            // different context.
            Class<?> handlerClass = Alarms.getHandlerClass(mHandler);
            String contextPackageName = handlerClass.getPackage().getName();
            i.setClassName(contextPackageName, mHandler);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "===> Handler is not set for this alarm");
            return;
        }
        i.addCategory(Intent.CATEGORY_ALTERNATIVE);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        // Alarm ID is always necessary for its operations.
        i.putExtra(AlarmColumns._ID, mId);
        i.putExtra(AlarmColumns.LABEL, mLabel);

        // Extract hourOfDay and minutes
        Calendar c = Alarms.getCalendarInstance();
        c.setTimeInMillis(mTimeInMillis);
        final int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
        final int minutes = c.get(Calendar.MINUTE);
        i.putExtra(AlarmColumns.HOUR_OF_DAY, hourOfDay);
        i.putExtra(AlarmColumns.MINUTES, minutes);
        i.putExtra(AlarmColumns.REPEAT_DAYS, mRepeatDays);
        i.putExtra(AlarmColumns.HANDLER, mHandler);

        if (!TextUtils.isEmpty(mExtra)) {
            i.putExtra(AlarmColumns.EXTRA, mExtra);
        }

        AlarmManager alarmManager =
            (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                         mTimeInMillis,
                         PendingIntent.getBroadcast(
                             context, 0, i,
                             PendingIntent.FLAG_CANCEL_CURRENT));
        Log.d(TAG, "===> Alarm.set(): " + i);
    }

    /**
     * Cancel this alarm by removing it from AlarmManager.
     *
     */
    public void cancel(Context context) {
        Intent i = new Intent(ACTION_HANDLE, getUri());
        try {
            // The context the handler class is in might be
            // different than the one OpenAlarm is in. For
            // instance, ApnHandler and AlarmHandler are in two
            // different context.
            Class<?> handlerClass = Alarms.getHandlerClass(mHandler);
            String contextPackageName = handlerClass.getPackage().getName();
            i.setClassName(contextPackageName, mHandler);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "===> Handler is not set for this alarm");
            return;
        }
        i.addCategory(Intent.CATEGORY_ALTERNATIVE);

        AlarmManager alarmManager =
            (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(PendingIntent.getBroadcast(
                                context, 0, i,
                                PendingIntent.FLAG_CANCEL_CURRENT));
        Log.d(TAG, "===> Alarm.cancel(): " + i);
    }

    /**
     * Calculate the schedule of this alarm based on the current
     * settings.
     *
     */
    public void schedule() {
        mTimeInMillis = calculateNextSchedule();
    }

    /**
     * Update settings of this alarm with arguments. Changed
     * settings will cause alarm to be rescheduled.
     *
     */
    public void update(final Context context,
                       final boolean enabled,
                       final String label,
                       final int hourOfDay,
                       final int minutes,
                       final int repeatDays,
                       final String handler,
                       final String extra) {
        boolean scheduleRequired = false;
        ContentValues values = new ContentValues();
        if (enabled != mEnabled) {
            values.put(AlarmColumns.ENABLED, enabled);

            if (!enabled) {
                // Cancel already set alarm. It was set  which might be installed
                // by using the old handler.
                cancel(context);
            }
            mEnabled = enabled;
            scheduleRequired = true;
        }

        if (!label.equals(mLabel)) {
            values.put(AlarmColumns.LABEL, label);
            mLabel = label;
        }

        if (hourOfDay != mHourOfDay) {
            values.put(AlarmColumns.HOUR_OF_DAY, hourOfDay);
            mHourOfDay = hourOfDay;
            scheduleRequired = true;
        }

        if (minutes != mMinutes) {
            values.put(AlarmColumns.MINUTES, minutes);
            mMinutes = minutes;
            scheduleRequired = true;
        }

        if (repeatDays != mRepeatDays) {
            values.put(AlarmColumns.REPEAT_DAYS, repeatDays);
            mRepeatDays = repeatDays;
            scheduleRequired = true;
        }

        if (!TextUtils.isEmpty(handler) && !handler.equals(mHandler)) {
            values.put(AlarmColumns.HANDLER, handler);
            mHandler = handler;
            scheduleRequired = true;
        }

        if (!TextUtils.isEmpty(extra) && !extra.equals(mExtra)) {
            values.put(AlarmColumns.EXTRA, extra);
            mExtra = extra;
            scheduleRequired = true;
        }

        // If there is no change on settings, do nothing.
        if (values.size() == 0) {
            return;
        }

        // If alarm needs to be scheduled.
        if (mEnabled && scheduleRequired && isValid()) {
            Log.d(TAG, "===> alarm " + mId + " should be scheduled");
            schedule();
            values.put(AlarmColumns.TIME_IN_MILLIS, mTimeInMillis);

            set(context);
        }

        // Update the alarm database.
        if (values.size() > 0) {
            ContentResolver cr = context.getContentResolver();
            synchronized (cr) {
                cr.update(getUri(), values, null, null);
            }
            Log.d(TAG, "===> alarm " + mId + " is updated in DB");
        }
    }

    /**
     * Update the schedule of this alarm from outside. This is
     * ONLY used in ScheduleAlarmReceiver to force bindView() to
     * update every alarm view in the ListAdapter. Normally, we
     * should use long version of update() to manipulate alarm.
     *
     */
    public void update(final Context context, final long timeInMillis) {
        mTimeInMillis = timeInMillis;

        ContentValues values = new ContentValues();
        values.put(AlarmColumns.TIME_IN_MILLIS, timeInMillis);

        ContentResolver cr = context.getContentResolver();
        synchronized (cr) {
            cr.update(getUri(), values, null, null);
        }
    }

    /**
     * Snooze this alarm. Add _ID into shared preference.
     *
     */
    public void snooze(Context context, int minutesLater) {
        // Note that it is not required to cancle the old
        // alarm. It will be overridden by enableAlarm.

        // Arrange new time for snoozed alarm from current date
        // and time.
        Calendar calendar = Alarms.getCalendarInstance();
        calendar.add(Calendar.MINUTE, minutesLater);
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);

        // Schedule a new time and set alarm in AlarmManager.
        mTimeInMillis = calendar.getTimeInMillis();
        set(context);

        // Persit alarm id in SharedPreferences.
        SharedPreferences.Editor ed =
            Alarms.getSharedPreferencesOfSnoozedAlarm(context).edit();
        ed.putInt(AlarmColumns._ID, mId);
        ed.commit();
    }

    /**
     * Whether this alarm is snoozed.
     *
     */
    public boolean isSnoozed(Context context) {
        SharedPreferences sharedPreferences =
            Alarms.getSharedPreferencesOfSnoozedAlarm(context);
        final int snoozedAlarmId =
            sharedPreferences.getInt(AlarmColumns._ID, -1);
        if (mId == snoozedAlarmId) {
            return true;
        }
        return false;
    }

    /**
     * Unsnooze this alarm. Remove ID from preference file.
     *
     */
    public void unsnooze(Context context) {
        if (isSnoozed(context)) {
            SharedPreferences sharedPreferences =
                Alarms.getSharedPreferencesOfSnoozedAlarm(context);

            // Remove _ID to indicate that the snoozed alert is cancelled.
            sharedPreferences.edit().remove(AlarmColumns._ID).commit();
        }
    }

    /**
     * Delete this alarm from database and internal cache.
     *
     */
    public synchronized int delete(Context context) {
        // Remove itself from hash map
        sMap.remove(mId);

        // Delete from content.
        int count = 0;
        ContentResolver cr = context.getContentResolver();
        synchronized (cr) {
            count = cr.delete(getUri(), null, null);
        }
        return count;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id=").append(mId)
            .append(", enabled=").append(mEnabled)
            .append(", label=").append(mLabel)
            .append(", hourOfDay=").append(mHourOfDay)
            .append(", minutes=").append(mMinutes)
            .append(", repeatDays='").append(
                Alarms.RepeatWeekdays.toString(mRepeatDays, "everyday", "no days")).append("'")
            .append(", timeInMillis=").append(mTimeInMillis)
            .append(", handler=").append(mHandler)
            .append(", extra=").append(mExtra);
        return sb.toString();
    }

    private Alarm(final int id) {
        mId = id;
    }

    private Alarm(final int id,
                  final String label,
                  final int hourOfDay,
                  final int minutes,
                  final int repeatDays,
                  final long timeInMillis,
                  final boolean enabled,
                  final String handler,
                  final String extra) {
        mId = id;
        mLabel = label;
        mHourOfDay = hourOfDay;
        mMinutes = minutes;
        mRepeatDays = repeatDays;
        mTimeInMillis = timeInMillis;
        mEnabled = enabled;
        mHandler = handler;
        mExtra = extra;
    }

    private long calculateNextSchedule() {
        // Get alarm's time in milliseconds.
        Calendar calendar = Alarms.getCalendarInstance();
        calendar.set(Calendar.HOUR_OF_DAY, mHourOfDay);
        calendar.set(Calendar.MINUTE, mMinutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long timeInMillis = calendar.getTimeInMillis();

        // Start with current date and time.
        calendar = Alarms.getCalendarInstance();
        long nowTimeInMillis = calendar.getTimeInMillis();

        // If alarm's time is in the past, move calendar to the
        // (hourOfDay, minutes) tomorrow first and calculate next
        // time. The worst case is that (hourOfDay, minutes) is
        // the next time of the alarm.
        if (timeInMillis <= nowTimeInMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, mHourOfDay);
        calendar.set(Calendar.MINUTE, mMinutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Try to shift calendar day by day in order to find the
        // nearest alarm.
        while(true) {
            if(Alarms.RepeatWeekdays.isSet(mRepeatDays,
                                           calendar.get(Calendar.DAY_OF_WEEK))) {
                break;
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return calendar.getTimeInMillis();
    }
}