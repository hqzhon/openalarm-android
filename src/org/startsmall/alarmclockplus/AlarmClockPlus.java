/**
 * @file   AlarmClockPlus.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Fri Oct  9 16:57:28 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.alarmclockplus;

import org.startsmall.alarmclockplus.preference.*;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.AdapterView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.CheckBox;
import android.text.TextUtils;

import java.util.*;

public class AlarmClockPlus extends ListActivity {
    public static final int OPEN_ALARM_SETTINGS_CODE = 1;

    private static final String TAG = "AlarmClockPlus";
    private static final int MENU_ITEM_DELETE_ID = 0;

    private class AlarmAdapter extends CursorAdapter {
        private LayoutInflater mInflater;
        private Alarms.RepeatWeekdays mRepeatOn;
        private View.OnClickListener mOnClickListener;
        private View.OnCreateContextMenuListener mOnCreateContextMenuListener;
        private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;

        public AlarmAdapter(Context context, Cursor c) {
            super(context, c);
            mInflater = AlarmClockPlus.this.getLayoutInflater();
            mRepeatOn = Alarms.RepeatWeekdays.getInstance();
            mOnClickListener =
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Bundle attachment = (Bundle)view.getTag();

                        int alarmId = attachment.getInt(Alarms.AlarmColumns._ID);
                        editAlarmSettings(alarmId);
                    }
                };
            mOnCreateContextMenuListener =
                new View.OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu,
                                                    View view,
                                                    ContextMenu.ContextMenuInfo menuInfo) {
                        Bundle attachment = (Bundle)view.getTag();

                        String label = attachment.getString(Alarms.AlarmColumns.LABEL);
                        int alarmId = attachment.getInt(Alarms.AlarmColumns._ID);

                        menu.setHeaderTitle(label);
                        menu.add(alarmId, MENU_ITEM_DELETE_ID, 0, R.string.menu_item_delete_alarm);
                    }
                };
            mOnCheckedChangeListener =
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        View parent = (View)buttonView.getParent();
                        Bundle attachment = (Bundle)parent.getTag();
                        int alarmId =
                            attachment.getInt(Alarms.AlarmColumns._ID);

                        // Log.d(TAG, "=====> onCheckedChanged("
                        //       + buttonView + "): parent=" + parent
                        //       + ", isChecked=" + isChecked
                        //       + ", alarmId=" + alarmId);

                        Alarms.setAlarm(AlarmClockPlus.this,
                                        Alarms.getAlarmUri(alarmId),
                                        isChecked);
                    }
                };

        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Important note:
            // You have to use all columns in cursor to update this
            // view. Android seems to have an internal stack-alike
            // structure to keep the views and it follows the
            // first-in-last-out order to retrieve the view out of
            // stack. The only thing you can trust is the order of
            // positions is the same which is from 0 to the last.

            final int id = cursor.getInt(Alarms.AlarmColumns.PROJECTION_ID_INDEX);
            final String label = cursor.getString(Alarms.AlarmColumns.PROJECTION_LABEL_INDEX);
            final int hourOfDay = cursor.getInt(Alarms.AlarmColumns.PROJECTION_HOUR_INDEX);
            final int minutes = cursor.getInt(Alarms.AlarmColumns.PROJECTION_MINUTES_INDEX);
            final int daysCode = cursor.getInt(Alarms.AlarmColumns.PROJECTION_REPEAT_DAYS_INDEX);
            final boolean enabled = cursor.getInt(Alarms.AlarmColumns.PROJECTION_ENABLED_INDEX) == 1;

            Bundle attachment = (Bundle)view.getTag();
            attachment.putInt(Alarms.AlarmColumns._ID, id);
            attachment.putString(Alarms.AlarmColumns.LABEL, label);

            // Time
            TextView timeView = (TextView)view.findViewById(R.id.time);
            Calendar calendar = Alarms.getCalendarInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minutes);
            timeView.setText(Alarms.formatDate("HH:mm", calendar));

            // Label
            final TextView labelView =
                (TextView)view.findViewById(R.id.label);
            labelView.setText(label);

            // Enable this alarm?
            final CheckBox enabledCheckBox =
                (CheckBox)view.findViewById(R.id.enabled);

            // Log.d(TAG, "===> bindView(): view=" + view
            //       + ", position=" + cursor.getPosition()
            //       + ", id=" + id
            //       + ", attachment=" + Integer.toHexString(attachment.hashCode())
            //       + ", isChecked(" + enabledCheckBox + ", " + enabledCheckBox.isChecked() + ")"
            //       + ", enabled=" + enabled);

            // Set checkbox's listener to null. If set, the
            // listener defined will try to update the database
            // and make bindView() called which enters an
            // infinite loop.
            enabledCheckBox.setOnCheckedChangeListener(null);
            enabledCheckBox.setChecked(enabled);
            enabledCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);

            // Repeat days
            final TextView repeatDays =
                (TextView)view.findViewById(R.id.repeat_days);
            mRepeatOn.setCode(daysCode);
            repeatDays.setText(mRepeatOn.toString());
            if(daysCode == 0) {
                repeatDays.setVisibility(View.GONE);
            } else {
                repeatDays.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.alarm_list_item,
                                          parent,
                                          false);

            Bundle attachment = new Bundle();
            view.setTag(attachment);

            final int id = cursor.getInt(Alarms.AlarmColumns.PROJECTION_ID_INDEX);
            Log.d(TAG, "=====> newView(): view=" + view
                  + ", position=" + cursor.getPosition()
                  + ", id=" + id
                  + ", attachment=" + Integer.toHexString(attachment.hashCode()));

            /**
             * Should follow Android's design. Click to go to
             * item's default activity and long-click to go to
             * item's extended activities. Here, the first-class
             * activity is to edit item's settings.
             */
            view.setOnClickListener(mOnClickListener);

            /**
             * The context menu listener of the view must be set
             * in order for its parent's onCreateMenu() to be
             * called to create context menu. This should be a
             * bug but I'm not very sure.
             *
             * The ContextMenu.ContextMenuInfo object is returned
             * by getContextMenuInfo() overriden by
             * clients. Here, it is null.
             */
            view.setOnCreateContextMenuListener(mOnCreateContextMenuListener);

            //
            final CheckBox enabledCheckBox =
                (CheckBox)view.findViewById(R.id.enabled);
            return view;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Cursor alarmsCursor =
            Alarms.getAlarmCursor(this, Alarms.getAlarmUri(-1));

        // FIXME: Still don't know why enabling this line will prevent
        // deleted row from removing from ListView. Need to
        // figure out.
        // startManagingCursor(cursor);
        setListAdapter(new AlarmAdapter(this, alarmsCursor));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.menu_item_add:
            addAlarm();
            break;

        case R.id.menu_item_preferences:
            editAppPreferences();
            break;

        case R.id.menu_item_help:
            showHelpDialog();
            break;
        }

        return true;
    }

    public boolean onContextItemSelected(MenuItem item) {
        final int alarmId = item.getGroupId();
        switch(item.getItemId()) {
        case MENU_ITEM_DELETE_ID:
            new AlertDialog.Builder(this)
                .setMessage(R.string.delete_alarm_message)
                .setTitle(R.string.confirm_alarm_deletion_title)
                .setPositiveButton(
                    R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            AlarmClockPlus.this.deleteAlarm(alarmId);
                        }
                    })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
            break;

        default:
            break;
        }

        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != RESULT_OK) {
            return;
        }

        switch(requestCode) {
        case OPEN_ALARM_SETTINGS_CODE:
            final int alarmId =
                data.getIntExtra(Alarms.AlarmColumns._ID, -1);
            final String newLabel =
                data.getStringExtra(Alarms.AlarmColumns.LABEL);
            final int newHourOfDay =
                data.getIntExtra(Alarms.AlarmColumns.HOUR, -1);
            final int newMinutes =
                data.getIntExtra(Alarms.AlarmColumns.MINUTES, -1);
            final int newRepeatOnDaysCode =
                data.getIntExtra(Alarms.AlarmColumns.REPEAT_DAYS, -1);
            final String newAction =
                data.getStringExtra(Alarms.AlarmColumns.ACTION);
            final String newExtra =
                data.getStringExtra(Alarms.AlarmColumns.EXTRA);

            Log.d(TAG, "===> Result back: alarmId=" + alarmId
                  + ", label=" + newLabel
                  + ", time=" + newHourOfDay + ":" + newMinutes
                  + ", action=" + newAction
                  + (TextUtils.isEmpty(newExtra) ? "" : (", extra=" + newExtra)));

            // Get old values from database
            class GetAlarmSettings implements Alarms.OnVisitListener {
                public String mLabel;
                public int mHour;
                public int mMinutes;
                public int mRepeatOnDaysCode;
                public boolean mEnabled;
                public String mAction;
                public String mExtra;
                public void onVisit(final Context context,
                                    final int id,
                                    final String label,
                                    final int hour,
                                    final int minutes,
                                    final int atTimeInMillis,
                                    final int repeatOnDaysCode,
                                    final boolean enabled,
                                    final String action,
                                    final String extra) {
                    mLabel = label;
                    mHour = hour;
                    mMinutes = minutes;
                    mRepeatOnDaysCode = repeatOnDaysCode;
                    mEnabled = enabled;
                    mAction = action;
                    mExtra = extra;
                }
            }
            Uri alarmUri = Alarms.getAlarmUri(alarmId);
            GetAlarmSettings settings = new GetAlarmSettings();
            Alarms.forEachAlarm(this, alarmUri, settings);

            Log.d(TAG, "===> Old values: alarmId=" + alarmId
                  + ", label=" + settings.mLabel
                  + ", time=" + settings.mHour + ":" + settings.mMinutes);

            ContentValues newValues = new ContentValues();
            if(newLabel != settings.mLabel) {
                newValues.put(Alarms.AlarmColumns.LABEL, newLabel);
            }

            if(newHourOfDay != settings.mHour) {
                newValues.put(Alarms.AlarmColumns.HOUR, newHourOfDay);
            }

            if(newMinutes != settings.mMinutes) {
                newValues.put(Alarms.AlarmColumns.MINUTES, newMinutes);
            }

            if(newRepeatOnDaysCode != settings.mRepeatOnDaysCode) {
                newValues.put(Alarms.AlarmColumns.REPEAT_DAYS,
                              newRepeatOnDaysCode);
            }

            if(newAction != settings.mAction) {
                newValues.put(Alarms.AlarmColumns.ACTION, newAction);
            }

            if(newAction != settings.mAction) {
                newValues.put(Alarms.AlarmColumns.ACTION, newAction);
            }

            if(newExtra != settings.mExtra) {
                newValues.put(Alarms.AlarmColumns.EXTRA, newExtra);
            }

            if(settings.mEnabled) {
                // If these values were updated, we need to
                // re-schedule the alarm with these new values.
                if(newValues.containsKey(Alarms.AlarmColumns.HOUR) ||
                   newValues.containsKey(Alarms.AlarmColumns.MINUTES) ||
                   newValues.containsKey(Alarms.AlarmColumns.REPEAT_DAYS) ||
                   newValues.containsKey(Alarms.AlarmColumns.ACTION) ||
                   newValues.containsKey(Alarms.AlarmColumns.EXTRA)) {

                    // Deactivate the old alarm.
                    Alarms.setAlarm(this, alarmUri, false);

                    // Update the alarm with new settings.
                    Alarms.updateAlarm(this, alarmUri, newValues);

                    // Activate the alarm now.
                    Alarms.setAlarm(this, alarmUri, true);
                }
            } else {
                if(newValues.size() > 0) {
                    Alarms.updateAlarm(this, alarmUri, newValues);
                }
            }
            break;
        default:
            break;
        }
    }

    // TODO:
    private void editAppPreferences() {
        Log.d(TAG, "===> Editing application preferences");
    }

    // TODO:
    private void showHelpDialog() {
        Log.d(TAG, "===> Show help dialog and copyright");
    }

    private void addAlarm() {
        Uri uri = Alarms.newAlarm(this);
        int alarmId = Integer.parseInt(uri.getLastPathSegment());
        editAlarmSettings(alarmId);
    }

    private void editAlarmSettings(int alarmId) {
        Log.d(TAG, "Edit alarm settings for " + Alarms.getAlarmUri(alarmId));
        Intent intent = new Intent(this, AlarmSettings.class);
        intent.putExtra(Alarms.AlarmColumns._ID, alarmId);
        startActivityForResult(intent, OPEN_ALARM_SETTINGS_CODE);
    }

    private int deleteAlarm(int alarmId) {
        return Alarms.deleteAlarm(this, alarmId);
    }
}
