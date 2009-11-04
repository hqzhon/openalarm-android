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

import org.startsmall.alarmclockplus.preference.AlarmSettings;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
//import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.TextUtils;

import java.util.*;

public class AlarmClockPlus extends ListActivity {
    public static final int OPEN_ALARM_SETTINGS_CODE = 1;

    private static final String TAG = "AlarmClockPlus";
    private static final int MENU_ITEM_DELETE_ID = 0;
    private Cursor mAlarmsCursor;

    private class AlarmAdapter extends CursorAdapter {
        private LayoutInflater mInflater;
        private View.OnClickListener mOnClickListener;
        private View.OnCreateContextMenuListener mOnCreateContextMenuListener;
        private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;

        public AlarmAdapter(Context context, Cursor c) {
            super(context, c);
            mInflater = AlarmClockPlus.this.getLayoutInflater();
            mOnClickListener =
                new View.OnClickListener() {
                    // @Override
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
            final String action = cursor.getString(Alarms.AlarmColumns.PROJECTION_ACTION_INDEX);

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

            // Set checkbox's listener to null. If set, the
            // listener defined will try to update the database
            // and make bindView() called which enters an
            // infinite loop.
            enabledCheckBox.setOnCheckedChangeListener(null);
            enabledCheckBox.setChecked(enabled);
            enabledCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);
            final LinearLayout repeatDaysView =
                (LinearLayout)view.findViewById(R.id.repeat_days);
            repeatDaysView.removeAllViews();
            Iterator<String> days =
                Alarms.RepeatWeekdays.toStringList(daysCode).iterator();

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(2, 2, 2, 2);

            while(days.hasNext()) {
                TextView dayLabel = new TextView(context);
                dayLabel.setClickable(false);
                dayLabel.setBackgroundResource(R.drawable.rounded_background);
                dayLabel.setTextAppearance(context, android.R.attr.textAppearanceSmall);
                dayLabel.setText(days.next());
                repeatDaysView.addView(dayLabel, params);
            }

            if(daysCode == 0) {
                repeatDaysView.setVisibility(View.GONE);
            } else {
                repeatDaysView.setVisibility(View.VISIBLE);
            }

            // Action
            if(!TextUtils.isEmpty(action)) {
                PackageManager pm = context.getPackageManager();
                try {
                    ActivityInfo info =
                        pm.getReceiverInfo(
                            new ComponentName(context, action), 0);
                    Drawable actionIcon = info.loadIcon(pm);
                    String actionLabel = info.loadLabel(pm).toString();

                    ImageView actionIconView =
                        (ImageView)view.findViewById(R.id.icon);
                    actionIconView.setImageDrawable(actionIcon);

                    // TODO: Should load the default icon with
                    // question mark to indicate that this action
                    // handler has problems so that it can't not
                    // be loaded.

                    // TextView actionTextView =
                    //     (TextView)view.findViewById(R.id.action);
                    // if(!TextUtils.isEmpty(actionLabel)) {
                    //     actionTextView.setText(actionLabel.toLowerCase());
                    // } else {
                    //     actionTextView.setText("not set");
                    // }
                } catch(PackageManager.NameNotFoundException e) {
                    Log.d(TAG, "xxxxxxxxxxxc 1" + e);
                }
            }

            final String extra = cursor.getString(Alarms.AlarmColumns.PROJECTION_EXTRA_INDEX);
            Log.d(TAG, "===> Bind these alarm settigs to view: id=" + id
                  + ", label=" + label
                  + ", time=" + hourOfDay + ":" + minutes
                  + ", enabled=" + enabledCheckBox.isChecked()
                  + ", repeat on=" + Alarms.RepeatWeekdays.toString(daysCode)
                  + ", action=" + action
                  + ", extra=" + extra);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.alarm_list_item,
                                          parent,
                                          false);

            // TextView actionTextView =
            //     (TextView)view.findViewById(R.id.action);
            // PaintDrawable actionBackground =
            //     new PaintDrawable(R.drawable.blue);
            // actionBackground.setCornerRadius(0.5f);
            // actionTextView.setBackgroundDrawable(actionBackground);

            Bundle attachment = new Bundle();
            view.setTag(attachment);

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
            view.setOnCreateContextMenuListener(
                mOnCreateContextMenuListener);

            return view;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Cursor alarmsCursor =
        //     Alarms.getAlarmCursor(this, Alarms.getAlarmUri(-1));
        mAlarmsCursor =
            Alarms.getAlarmCursor(this, Alarms.getAlarmUri(-1));

        // FIXME: Still don't know why enabling this line will prevent
        // deleted row from removing from ListView. Need to
        // figure out.
        // startManagingCursor(cursor);
        setListAdapter(new AlarmAdapter(this, mAlarmsCursor));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onDestroy() {
        // Closing the cached cursor;
        mAlarmsCursor.close();

        super.onDestroy();
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
            // Extract alarm settings derived from user on AlarmSettings
            // activity.
            final int alarmId = data.getIntExtra(Alarms.AlarmColumns._ID, -1);
            final String newLabel = data.getStringExtra(Alarms.AlarmColumns.LABEL);
            final int newHourOfDay = data.getIntExtra(Alarms.AlarmColumns.HOUR, -1);
            final int newMinutes = data.getIntExtra(Alarms.AlarmColumns.MINUTES, -1);
            final int newRepeatOnDaysCode = data.getIntExtra(Alarms.AlarmColumns.REPEAT_DAYS, -1);
            final String newAction = data.getStringExtra(Alarms.AlarmColumns.ACTION);
            final String newExtra = data.getStringExtra(Alarms.AlarmColumns.EXTRA);

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

            Log.d(TAG, "===> Alarm settings in SQL: id=" + alarmId
                  + ", label=" + settings.mLabel
                  + ", enabled=" + settings.mEnabled
                  + ", time=" + settings.mHour + ":" + settings.mMinutes
                  + ", repeat_on=" + Alarms.RepeatWeekdays.toString(settings.mRepeatOnDaysCode)
                  + ", action=" + settings.mAction
                  + ", extra=" + settings.mExtra);

            ContentValues newValues = new ContentValues();
            if(!newLabel.equals(settings.mLabel)) {
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

            if(!TextUtils.isEmpty(newAction) &&
               !newAction.equals(settings.mAction)) {
                newValues.put(Alarms.AlarmColumns.ACTION, newAction);
            }

            if(!TextUtils.isEmpty(newExtra) &&
               !newExtra.equals(settings.mExtra)) {
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
        Intent intent = new Intent(this, AlarmSettings.class);
        intent.putExtra(Alarms.AlarmColumns._ID, alarmId);
        startActivityForResult(intent, OPEN_ALARM_SETTINGS_CODE);
    }

    private int deleteAlarm(int alarmId) {
        return Alarms.deleteAlarm(this, alarmId);
    }
}
