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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.net.Uri;
import android.util.Log;
import android.text.format.DateUtils;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.text.DateFormat;
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

    /**
     * Convenient java.util.Calendar instance.
     *
     */
    private static final Calendar CALENDAR = Calendar.getInstance();
    public static Calendar getCalendarInstance() {
        CALENDAR.setTimeInMillis(System.currentTimeMillis());
        return CALENDAR;
    }

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
        public static final String ACTION = "action";

        /// Audio to play when alarm triggers.
        public static final String EXTRA = "extra";

        /**
         * Columns that will be pulled from a row. These should
         * be in sync with PROJECTION indexes.
         *
         */
        public static final String[] QUERY_COLUMNS = {
            _ID, LABEL, HOUR, MINUTES, AT_TIME_IN_MILLIS, REPEAT_DAYS,
            ENABLED, ACTION, EXTRA};

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
        public static final int PROJECTION_ACTION_INDEX = 7;
        public static final int PROJECTION_EXTRA_INDEX = 8;
    }

    /**
     * Suppress default constructor for noninstantiability.
     */
    private Alarms() {}

    /******************************************************************
     * Key strings used in Intent data.                               *
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
                    result = "On Everyday";
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
                    result.add("On Everyday");
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
        public void onVisit(Context context,
                            final int id,
                            final String label,
                            final int hour,
                            final int minutes,
                            final int atTimeInMillis,
                            final int repeatOnDaysCode,
                            final boolean enabled,
                            final String action,
                            final String extra);
    }

    /**
     *
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
                final String action =
                    cursor.getString(AlarmColumns.PROJECTION_ACTION_INDEX);
                final String extra =
                    cursor.getString(AlarmColumns.PROJECTION_EXTRA_INDEX);

                if(listener != null) {
                    listener.onVisit(context, id, label, hour, minutes,
                                     atTimeInMillis, repeatOnDaysCode,
                                     enabled, action, extra);
                }
            } while(cursor.moveToNext());
        }
        cursor.close();
    }

    /**
     *
     *
     * @param context
     *
     * @return
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

    /**
     *
     *
     * @param context
     * @param alarmId
     * @param enabled
     */
    public static void setAlarm(final Context context,
                                final Uri alarmUri,
                                final boolean enabled) {
        Context appContext = context.getApplicationContext();
        int alarmId = Integer.parseInt(alarmUri.getLastPathSegment());
        NotificationManager nm =
            (NotificationManager)appContext.getSystemService(Context.NOTIFICATION_SERVICE);

        ContentValues newValues = new ContentValues();
        newValues.put(AlarmColumns.ENABLED, enabled ? 1 : 0);
        if(enabled) {
            long newAtTimeInMillis = activateAlarm(context, alarmUri);
            newValues.put(AlarmColumns.AT_TIME_IN_MILLIS, newAtTimeInMillis);


            Intent alarmChanged = new Intent(Intent.ACTION_ALARM_CHANGED);
            alarmChanged.putExtra("alarmSet", enabled);
            context.sendBroadcast(alarmChanged);


            // Class<?> handler;
            // try {
            //     handler = Class.forName(context.getPackageName() + ".AlarmClockPlus");
            // } catch(ClassNotFoundException e) {
            //     return;
            // }

            // Intent notificationIntent = new Intent(context, handler);
            // PendingIntent pendingIntent = PendingIntent.getActivity(
            //     context, 0, notificationIntent, 0);
            // Notification notification = new Notification(
            //     R.drawable.stat_notify_alarm,
            //     "Hello",
            //     System.currentTimeMillis());
            // notification.flags = Notification.FLAG_AUTO_CANCEL|Notification.FLAG_ONGOING_EVENT ;
            // notification.setLatestEventInfo(appContext,
            //                                 "My notification title",
            //                                 "Hello World!",
            //                                 pendingIntent);
            // nm.notify(alarmId, notification);
        } else {
            Log.d(TAG, "===> setAlarm(): deactivate alarm '" + alarmUri);
            deactivateAlarm(context, alarmUri);
            // nm.cancel(alarmId);
        }
        updateAlarm(context, alarmUri, newValues);
    }

    private synchronized static long activateAlarm(final Context context,
                                                   final Uri alarmUri) {
        class ActivateAlarm implements OnVisitListener {
            public long mAtTimeInMillis;

            public void onVisit(final Context context,
                                final int id,
                                final String label,
                                final int hour,
                                final int minutes,
                                final int oldAtTimeInMillis,
                                final int repeatOnDaysCode,
                                final boolean enabled,
                                final String action,
                                final String extra) {
                Intent intent = new Intent(DISPATCH_ACTION);
                intent.putExtra(Alarms.AlarmColumns._ID, id);
                intent.putExtra(Alarms.AlarmColumns.LABEL, label);
                intent.putExtra(Alarms.AlarmColumns.ACTION, action);

                intent.setClassName(context, action);
                intent.putExtra(Alarms.AlarmColumns.EXTRA, extra);

                AlarmManager alarmManager =
                    (AlarmManager)context.getSystemService(
                        Context.ALARM_SERVICE);

                mAtTimeInMillis =
                    calculateAlarmAtTimeInMillis(hour, minutes, repeatOnDaysCode);

                intent.putExtra(Alarms.AlarmColumns.AT_TIME_IN_MILLIS, mAtTimeInMillis);

                // NotificationManager notificationManager =
                //     (NotificationManager)context.getSystemService(
                //                   Context.NOTIFICATION_SERVICE);

                alarmManager.set(AlarmManager.RTC_WAKEUP,
                                 mAtTimeInMillis,
                                 PendingIntent.getBroadcast(
                                     context, 0, intent,
                                     PendingIntent.FLAG_CANCEL_CURRENT));

                // Prepare for notification and toast message
                // Date date = new Date(mAtTimeInMillis);
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                                                                       DateFormat.MEDIUM);
                String text =
                    String.format(context.getString(R.string.alarm_activated_toast_text),
                                  dateFormat.format(new Date(mAtTimeInMillis)));
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        }

        ActivateAlarm alarmSettings = new ActivateAlarm();
        forEachAlarm(context, alarmUri, alarmSettings);
        return alarmSettings.mAtTimeInMillis;
    }

    private static void deactivateAlarm(final Context context,
                                        final Uri alarmUri) {
        forEachAlarm(
            context, alarmUri,
            new OnVisitListener() {
                public void onVisit(Context context,
                                    final int id,
                                    final String label,
                                    final int hour,
                                    final int minutes,
                                    final int atTimeInMillis,
                                    final int repeatOnDaysCode,
                                    final boolean enabled,
                                    final String action,
                                    final String extra) {
                    Intent intent = new Intent(DISPATCH_ACTION);
                    intent.putExtra(Alarms.AlarmColumns._ID, id);
                    intent.putExtra(Alarms.AlarmColumns.LABEL, label);
                    intent.putExtra(Alarms.AlarmColumns.ACTION, action);
                    intent.putExtra(Alarms.AlarmColumns.EXTRA, extra);

                    AlarmManager alarmManager =
                        (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                    alarmManager.cancel(
                        PendingIntent.getBroadcast(
                            context, 0, intent,
                            PendingIntent.FLAG_CANCEL_CURRENT));
                }
            });

        // TODO: Cancel notification
    }

    public static String formatDate(String pattern, Calendar calendar) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern);
        return dateFormatter.format(calendar.getTime());
    }

    private static long calculateAlarmAtTimeInMillis(final int hourOfDay,
                                                     final int minutes,
                                                     final int repeatOnCode) {
        // Start with current date and time.
        Calendar calendar = getCalendarInstance();

        int nowHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int nowMinutes = calendar.get(Calendar.MINUTE);

        if((hourOfDay < nowHourOfDay) ||
           ((hourOfDay == nowHourOfDay) && (minutes < nowMinutes))) {
            // If this alarm is behind current time. Move
            // calendar to tomorrow.
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

        Log.d(TAG, "===> next alarm " + calendar.getTimeInMillis());
        return calendar.getTimeInMillis();
    }
}
