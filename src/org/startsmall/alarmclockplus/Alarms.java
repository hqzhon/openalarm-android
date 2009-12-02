/**
 * @file   Alarms.java
 * @author  <yenliangl@gmail.com>
 * @date   Wed Sep 30 16:47:42 2009
 *
 * @brief  Utility class.
 *
 *
 */
package org.startsmall.alarmclockplus;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.net.Uri;
import android.util.Log;
import android.text.format.DateUtils;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.text.SimpleDateFormat;

/**
 *
 *
 */
public class Alarms {
    private static final String TAG = "Alarms";

    /**
     * Authority of this application.
     * <p>Value: org.startsmall.alarmclockplus</p>
     */
    public static final String CONTENT_URI_AUTH = "org.startsmall.alarmclockplus";

    /**
     * Alarm alert action string.
     * <p>Value: org.startsmall.alarmclockplus.HANDLE_ALARM</p>
     */
    public static final String HANDLE_ALARM = CONTENT_URI_AUTH + ".HANDLE_ALARM";

    /**
     * Action used to launch ActionDispatcher receiver.
     * <p>Value: org.startsmall.alarmclockplus.DISPATCH_ACTION</p>
     */
    // public static final String DISPATCH_ACTION = CONTENT_URI_AUTH + ".DISPATCH_ACTION";

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

    /**
     * Convenient java.util.Calendar instance.
     *
     */
    private static final Calendar CALENDAR = Calendar.getInstance();
    public static Calendar getCalendarInstance() {
        CALENDAR.setTimeInMillis(System.currentTimeMillis());
        return CALENDAR;
    }

    public static final String PREFERENCE_FILE_FOR_SNOOZED_ALARM = "snoozed_alarm";

    private static final String USER_APK_DIR = "/data/app";

    /*****************************************************************
     * Constants used in content provider and SQLiteDatabase.    *
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

        /// Go off time in milliseconds
        public static final String AT_TIME_IN_MILLIS = "time";

        /// Days this alarm works in this week.
        public static final String REPEAT_DAYS = "repeat_days";

        /// Whether or not this alarm is active currently
        public static final String ENABLED = "enabled";

        /// Audio to play when alarm triggers.
        public static final String HANDLER = "handler";

        /// Audio to play when alarm triggers.
        public static final String EXTRA = "extra";

        /**
         * Columns that will be pulled from a row. These should
         * be in sync with PROJECTION indexes.
         *
         */
        public static final String[] QUERY_COLUMNS = {
            _ID, LABEL, HOUR, MINUTES, AT_TIME_IN_MILLIS, REPEAT_DAYS,
            ENABLED, HANDLER, EXTRA};

        /**
         *
         */
        public static final int PROJECTION_ID_INDEX = 0;
        public static final int PROJECTION_LABEL_INDEX = 1;
        public static final int PROJECTION_HOUR_INDEX = 2;
        public static final int PROJECTION_MINUTES_INDEX = 3;
        public static final int PROJECTION_AT_TIME_IN_MILLIS_INDEX = 4;
        public static final int PROJECTION_REPEAT_DAYS_INDEX = 5;
        public static final int PROJECTION_ENABLED_INDEX = 6;
        public static final int PROJECTION_HANDLER_INDEX = 7;
        public static final int PROJECTION_EXTRA_INDEX = 8;
    }

    /**
     * Suppress default constructor for noninstantiability.
     */
    private Alarms() {}

    /******************************************************************
     * Encoder/Decode repeat days.
     ******************************************************************/
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

        /**
         * Suppress default constructor for noninstantiability.
         */
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

        public static String toString(int code) {
            String result = "";
            if(code > 0) {
                if(code == 0x7F) { // b1111111
                    result = "Everyday";
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
                result = "No days";
            }
            return result;
        }

        public static List<String> toStringList(int code) {
            List<String> result = new LinkedList<String>();
            if(code > 0) {
                if(code == 0x7F) { // b1111111
                    result.add("Everyday");
                } else {
                    for(int i = 1; i < 8; i++) { // From SUNDAY to SATURDAY
                        if(isSet(code, i)) {
                            result.add(
                                DateUtils.getDayOfWeekString(
                                    i,
                                    DateUtils.LENGTH_MEDIUM));
                        }
                    }
                }
            } else {
                result.add("No days");
            }
            return result;
        }
    }

    /**
     * Return Uri of an alarm with id given.
     *
     * @param alarmId ID of the alarm. If -1 is given, it returns Uri representing all alarms.
     *
     * @return Uri of the alarm.
     */
    public static Uri getAlarmUri(final long alarmId) {
        if(alarmId == -1) {
            return Uri.parse(CONTENT_URI_ALL_ALARMS);
        } else {
            return Uri.parse(CONTENT_URI_ALL_ALARMS + "/" + alarmId);
        }
    }

    /**
     *
     *
     * @param context
     * @param alarmUri
     *
     * @return
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
        void onVisit(final Context context,
                     final int id,
                     final String label,
                     final int hour,
                     final int minutes,
                     final int atTimeInMillis,
                     final int repeatOnDaysCode,
                     final boolean enabled,
                     final String handler,
                     final String extra);
    }

    public static class GetAlarmSettings implements OnVisitListener {
        public int id;
        public String label;
        public int hour;
        public int minutes;
        public int atTimeInMillis;
        public int repeatOnDaysCode;
        public boolean enabled;
        public String handler;
        public String extra;

        @Override
        public void onVisit(final Context context,
                            final int id,
                            final String label,
                            final int hour,
                            final int minutes,
                            final int atTimeInMillis,
                            final int repeatOnDaysCode,
                            final boolean enabled,
                            final String handler,
                            final String extra) {
            this.id = id;
            this.label = label;
            this.hour = hour;
            this.minutes = minutes;
            this.atTimeInMillis = atTimeInMillis;
            this.repeatOnDaysCode = repeatOnDaysCode;
            this.enabled = enabled;
            this.handler = handler;
            this.extra = extra;
        }
    }

    /**
     * Iterate alarms.
     *
     * @param context
     * @param alarmUri
     * @param listener
     */
    public static void forEachAlarm(final Context context,
                                    final Uri alarmUri,
                                    final OnVisitListener listener) {
        Cursor cursor = getAlarmCursor(context, alarmUri);
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
                final int atTimeInMillis =
                    cursor.getInt(AlarmColumns.PROJECTION_AT_TIME_IN_MILLIS_INDEX);
                final int repeatOnDaysCode =
                    cursor.getInt(AlarmColumns.PROJECTION_REPEAT_DAYS_INDEX);
                final boolean enabled =
                    cursor.getInt(AlarmColumns.PROJECTION_ENABLED_INDEX) == 1;
                final String handler =
                    cursor.getString(AlarmColumns.PROJECTION_HANDLER_INDEX);
                final String extra =
                    cursor.getString(AlarmColumns.PROJECTION_EXTRA_INDEX);

                if(listener != null) {
                    listener.onVisit(context, id, label, hour, minutes,
                                     atTimeInMillis, repeatOnDaysCode,
                                     enabled, handler, extra);
                }
            } while(cursor.moveToNext());
        }
        cursor.close();
    }

    /**
     * Insert a new alarm record into database.
     *
     * @param context Context this is calling from.
     *
     * @return Uri of the newly inserted alarm.
     */
    public synchronized static Uri newAlarm(Context context) {
        return context.getContentResolver().insert(
            Uri.parse(CONTENT_URI_ALL_ALARMS), null);
    }

    /**
     *
     *
     * @param context
     * @param alarmId
     *
     * @return
     */
    public synchronized static int deleteAlarm(Context context,
                                               int alarmId) {
        Uri alarmUri = Alarms.getAlarmUri(alarmId);
        return context.getContentResolver().delete(alarmUri, null, null);
    }

    /**
     *
     *
     * @param context
     * @param alarmId
     * @param newValues
     *
     * @return
     */
    public synchronized static int updateAlarm(final Context context,
                                               final Uri alarmUri,
                                               final ContentValues newValues) {
        if(newValues == null) {
            return -1;
        }
        return context.getContentResolver().update(
            alarmUri, newValues, null, null);
    }

    private static class EnableAlarm implements OnVisitListener {
        private final boolean mEnabled;
        public long mAtTimeInMillis;

        public EnableAlarm(boolean enabled) {
            mEnabled = enabled;
        }

        @Override
        public void onVisit(final Context context,
                            final int id,
                            final String label,
                            final int hour,
                            final int minutes,
                            final int oldAtTimeInMillis,
                            final int repeatOnDaysCode,
                            final boolean enabled,
                            final String handler,
                            final String extra) {
            if (TextUtils.isEmpty(handler)) {
                Log.d(TAG, "***** null alarm handler is not allowed");
                return;
            }

            Log.d(TAG, "Inside EnableAlarm, alarm " + label
                  + " handler=" + handler);

            if (mEnabled) {
                mAtTimeInMillis =
                    calculateAlarmAtTimeInMillis(hour, minutes,
                                                 repeatOnDaysCode);
                enableAlarm(context, id, label, handler, mAtTimeInMillis, extra);

                showToast(context, mAtTimeInMillis);

            } else {
                disableAlarm(context, id, handler);
            }

            // setNotification(context, id, handler, mEnabled);
            setNotification(context, mEnabled);
        }
    }

    /**
     * Enable/disable the alarm pointed by @c alarmUri.
     *
     * @param context Context this method is called.
     * @param alarmUri Alarm uri.
     * @param enabled Enable or disable this alarm.
     */
    public static synchronized void setAlarmEnabled(final Context context,
                                                    final Uri alarmUri,
                                                    final boolean enabled) {
        Log.d(TAG, "setAlarmEnabled(" + alarmUri + ", " + enabled + ")");

        ContentValues newValues = new ContentValues();
        newValues.put(AlarmColumns.ENABLED, enabled ? 1 : 0);

        // Activate or deactivate this alarm.
        EnableAlarm enabler = new EnableAlarm(enabled);
        forEachAlarm(context, alarmUri, enabler);

        if (enabled) {
            newValues.put(AlarmColumns.AT_TIME_IN_MILLIS,
                          enabler.mAtTimeInMillis);
        }
        updateAlarm(context, alarmUri, newValues);
    }

    /**
     *
     *
     * @param context
     * @param alarmId
     * @param handlerClassName
     * @param intent
     * @param extraData
     * @param minutesLater
     */
    public static void snoozeAlarm(final Context context,
                                   final int alarmId,
                                   final String label,
                                   final String handlerClassName,
                                   final String extraData,
                                   final int minutesLater) {
        // Cancel the old alert.
        disableAlarm(context, alarmId, handlerClassName);

        Log.d(TAG, "===> snoozeAlarm(): Cancel old alert with data=" + getAlarmUri(alarmId)
              + ", associated class=" + handlerClassName);

        // Arrange new time for snoozed alarm from current date
        // and time.
        Calendar calendar = getCalendarInstance();
        calendar.add(Calendar.MINUTE, minutesLater);
        long newAtTimeInMillis = calendar.getTimeInMillis();
        enableAlarm(context, alarmId, label, handlerClassName, newAtTimeInMillis, extraData);

        // Put info into SharedPreferences for the snoozed alarm.
        SharedPreferences preferences =
            context.getSharedPreferences(PREFERENCE_FILE_FOR_SNOOZED_ALARM,
                                         0);
        SharedPreferences.Editor preferenceEditor = preferences.edit();
        preferenceEditor.putLong(AlarmColumns.AT_TIME_IN_MILLIS,
                                 newAtTimeInMillis);
        preferenceEditor.putInt(AlarmColumns._ID, alarmId);
        preferenceEditor.putString(AlarmColumns.LABEL, label);
        // The handler is required to be persisted because it is
        // needed when we want to cancel the snoozed alarm.
        preferenceEditor.putString(AlarmColumns.HANDLER, handlerClassName);
        preferenceEditor.commit();
    }

    public static void cancelSnoozedAlarm(final Context context,
                                          final int alarmId) {
        Log.d(TAG, "===> Canceling snoozed alarm " + alarmId);

        SharedPreferences preferences =
            context.getSharedPreferences(
                PREFERENCE_FILE_FOR_SNOOZED_ALARM, 0);
        final int persistedAlarmId =
            preferences.getInt(AlarmColumns._ID, -1);
        if (alarmId != -1 &&     // no checking on alarmId
            persistedAlarmId != alarmId) {
            return;
        }

        final String handler =
            preferences.getString(AlarmColumns.HANDLER, null);
        if (!TextUtils.isEmpty(handler)) {
            disableAlarm(context, alarmId, handler);
            // Remove _ID to indicate that the snoozed alert is cancelled.
            preferences.edit().remove(AlarmColumns._ID).commit();
        }
    }

    public static void enableAlarm(final Context context,
                                   final int alarmId,
                                   final String label,
                                   final String handlerClassName,
                                   final long atTimeInMillis,
                                   final String extraData) {
        Intent i = new Intent(HANDLE_ALARM, getAlarmUri(alarmId));
        i.setClassName(context, handlerClassName);

        // Alarm ID is always necessary for its operations.
        i.putExtra(AlarmColumns._ID, alarmId);
        i.putExtra(AlarmColumns.LABEL, label);

        // Intent might be provided different class to associate,
        // like FireAlarm. We need to cache the handlerClass in
        // the Intent for latter use.
        i.putExtra(AlarmColumns.HANDLER, handlerClassName);

        if (!TextUtils.isEmpty(extraData)) {
            i.putExtra(AlarmColumns.EXTRA, extraData);
        }

        AlarmManager alarmManager =
            (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.set(AlarmManager.RTC_WAKEUP,
                         atTimeInMillis,
                         PendingIntent.getBroadcast(
                             context, 0, i,
                             PendingIntent.FLAG_CANCEL_CURRENT));

        Calendar calendar = getCalendarInstance();
        calendar.setTimeInMillis(atTimeInMillis);

        String timeString =
            formatDate(is24HourMode(context) ? "E k:mm" : "E K:mm aa",
                       calendar);
        setAlarmInSystemSettings(context, timeString);
    }

    /**
     * @return true if clock is set to 24-hour mode
     */
    static boolean is24HourMode(final Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    public static void disableAlarm(final Context context,
                                    final int alarmId,
                                    final String handlerClassName) {
        Uri alarmUri = getAlarmUri(alarmId);

        Intent i = new Intent(HANDLE_ALARM, alarmUri);
        i.setClassName(context, handlerClassName);

        AlarmManager alarmManager =
            (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.cancel(PendingIntent.getBroadcast(
                                    context, 0, i,
                                    PendingIntent.FLAG_CANCEL_CURRENT));

        setAlarmInSystemSettings(context, "");
    }

    private static void setAlarmInSystemSettings(final Context context,
                                                 String timeString) {

        // Make alarm alert shown on KeyguardManager
        Settings.System.putString(context.getContentResolver(),
                                  Settings.System.NEXT_ALARM_FORMATTED,
                                  timeString);
    }

    public static String formatDate(String pattern, Calendar calendar) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern);
        return dateFormatter.format(calendar.getTime());
    }

    public static String formatDate(Context context, final int hourOfDay, final int minutes) {
        Calendar calendar = getCalendarInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);

        final boolean is24HourFormat = is24HourMode(context);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("hh:mm a");
        if (is24HourFormat) {
            dateFormatter.applyPattern("HH:mm");
        }
        return dateFormatter.format(calendar.getTime());
    }

    public static long calculateAlarmAtTimeInMillis(final int hourOfDay,
                                                    final int minutes,
                                                    final int repeatOnCode) {
        // Start with current date and time.
        Calendar calendar = getCalendarInstance();

        int nowHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int nowMinutes = calendar.get(Calendar.MINUTE);

        // If alarm is set at the past, move calendar to the same
        // time tomorrow and then calculate the next time of
        // alarm's going off.
        if((hourOfDay < nowHourOfDay) ||
           ((hourOfDay == nowHourOfDay) && (minutes < nowMinutes))) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Align calendar's time with this alarm.
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Try to shift calendar by days in order to find the
        // nearest alarm.
        while(true) {
            if(RepeatWeekdays.isSet(repeatOnCode,
                                    calendar.get(Calendar.DAY_OF_WEEK))) {
                break;
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return calendar.getTimeInMillis();
    }

    public static void showToast(final Context context,
                                 final long atTimeInMillis) {
        DateFormat dateFormat =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        String text =
            String.format(
                context.getString(R.string.alarm_notification_toast_text),
                dateFormat.format(new Date(atTimeInMillis)));
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    public static void setNotification(final Context context,
                                       final boolean enabled) {
        // Set notification on status bar
        final String ACTION_ALARM_CHANGED = "android.intent.action.ALARM_CHANGED";
        Intent alarmChanged = new Intent(ACTION_ALARM_CHANGED);
        alarmChanged.putExtra("alarmSet", enabled);
        context.sendBroadcast(alarmChanged);
    }

    public static void setNotification(final Context context,
                                       final int alarmId,
                                       final String handlerClassName,
                                       boolean enabled) {
        Context appContext = context.getApplicationContext();
        NotificationManager nm =
            (NotificationManager)appContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (enabled) {
            String tickerText = appContext.getString(R.string.alarm_notification_ticker_text);
            Notification notification =
                new Notification(
                    R.drawable.stat_notify_alarm,
                    tickerText,
                    System.currentTimeMillis());
            notification.flags = Notification.FLAG_NO_CLEAR;

            Intent notificationIntent = new Intent(appContext,
                                                   AlarmClockPlus.class);
            PendingIntent contentIntent =
                PendingIntent.getActivity(appContext, 0, notificationIntent, 0);

            PackageManager pm = appContext.getPackageManager();
            String handlerLabel;
            try {
                ActivityInfo handlerInfo =
                    getHandlerInfo(pm, handlerClassName);
                handlerLabel = handlerInfo.loadLabel(pm).toString();
            } catch(PackageManager.NameNotFoundException e) {
                Log.d(TAG, e.getMessage());
                return;
            }

            String contentText =
                String.format(appContext.getString(R.string.alarm_notification_content_text), handlerLabel + "(" + handlerClassName + ")");
            notification.setLatestEventInfo(appContext,
                                            tickerText,
                                            contentText,
                                            contentIntent);
            nm.notify(alarmId, notification);
        } else {
            nm.cancel(alarmId);
        }
    }

    public static Class<?> getHandlerClass(final String handlerClassName)
        throws ClassNotFoundException {
        final int lastDotPos = handlerClassName.lastIndexOf('.');
        final String apkPaths =
            USER_APK_DIR + "/" + "org.startsmall.alarmclockplus.apk:" + // myself
            // handlers defined by other developers
            USER_APK_DIR + "/" + handlerClassName.substring(0, lastDotPos) + ".apk";

        dalvik.system.PathClassLoader classLoader =
            new dalvik.system.PathClassLoader(
                apkPaths,
                ClassLoader.getSystemClassLoader());

        return Class.forName(handlerClassName, true, classLoader);
    }

    public static ActivityInfo getHandlerInfo(final PackageManager pm,
                                              final String handlerClassName)
        throws PackageManager.NameNotFoundException {
        // Make sure the handlerClass really exists
        // Class<?> handlerClass = getHandlerClass(handlerClassName);

        Intent i = new Intent(HANDLE_ALARM);
        i.addCategory(Intent.CATEGORY_ALTERNATIVE);

        // Search all receivers that can handle my alarms.
        Iterator<ResolveInfo> infoObjs =
            pm.queryBroadcastReceivers(i, 0).iterator();
        while (infoObjs.hasNext()) {
            ActivityInfo activityInfo = infoObjs.next().activityInfo;
            if (activityInfo.name.equals(handlerClassName)) {
                return activityInfo;
            }
        }
        throw new PackageManager.NameNotFoundException(
            "BroadcastReceiver " + handlerClassName + " not found");
    }
}
