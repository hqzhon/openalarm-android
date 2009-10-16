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
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CheckBox;
import android.text.TextUtils;

import java.util.*;

public class AlarmClockPlus extends ListActivity {
    private static final String TAG = "AlarmClockPlus";

    private class AlarmAdapter extends CursorAdapter {
        private static final int MENU_ITEM_EDIT_ID = 0;
        private static final int MENU_ITEM_DELETE_ID = 1;
        private final int[] MENU_ITEM_RES_IDS = {R.string.menu_item_edit_alarm,
                                                 R.string.menu_item_delete_alarm};

        private class MenuItemOnClickListener implements DialogInterface.OnClickListener {
            private final int mAlarmId;
            private final String mAlarmLabel;
            private DialogInterface.OnClickListener mListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(which) {
                        case DialogInterface.BUTTON_POSITIVE: // delete alarm
                            AlarmClockPlus.this.deleteAlarm(mAlarmId);
                            break;
                        default: // BUTTON_NEGATIVE is ignored.
                            break;
                        }
                    }
                };

            MenuItemOnClickListener(final int alarmId, final String alarmLabel) {
                mAlarmId = alarmId;
                mAlarmLabel = alarmLabel;
            }

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Context context = AlarmClockPlus.this;

                switch(which) {
                case MENU_ITEM_EDIT_ID:
                    AlarmClockPlus.this.editAlarmSettings(mAlarmId);
                    break;

                case MENU_ITEM_DELETE_ID:
                    new AlertDialog.Builder(context)
                        .setMessage(R.string.delete_alarm_message)
                        .setTitle(mAlarmLabel)
                        .setPositiveButton("Yes", mListener)
                        .setNegativeButton("No", mListener)
                        .create()
                        .show();
                    break;
                default:
                    break;
                }
            }
        }

        private LayoutInflater mInflater;

        public AlarmAdapter(Context context, Cursor c) {
            super(context, c);

            mInflater = AlarmClockPlus.this.getLayoutInflater();
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Log.d(TAG, "bindView " + view + " from " + cursor.getPosition());
            final int id = cursor.getInt(Alarms.AlarmColumns.PROJECTION_ID_INDEX);
            final String label = cursor.getString(Alarms.AlarmColumns.PROJECTION_LABEL_INDEX);
            final int hourOfDay = cursor.getInt(Alarms.AlarmColumns.PROJECTION_HOUR_INDEX);
            final int minutes = cursor.getInt(Alarms.AlarmColumns.PROJECTION_MINUTES_INDEX);
            final int daysCode = cursor.getInt(Alarms.AlarmColumns.PROJECTION_REPEAT_DAYS_INDEX);
            final boolean enabled = cursor.getInt(Alarms.AlarmColumns.PROJECTION_ENABLED_INDEX) == 1;
            final boolean vibrate = cursor.getInt(Alarms.AlarmColumns.PROJECTION_VIBRATE_INDEX) == 1;
            final String audioAlert = cursor.getString(Alarms.AlarmColumns.PROJECTION_ALERT_URI_INDEX);

            // Time
            TextView timeView = (TextView)view.findViewById(R.id.time);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minutes);
            Alarms.formatDate("HH:mm", calendar);
            timeView.setText(Alarms.formatDate("HH:mm", calendar));

            // Label
            final TextView labelView =
                (TextView)view.findViewById(R.id.label);
            labelView.setText(label);

            // Enable this alarm?
            final CheckBox enabledChkBox =
                (CheckBox)view.findViewById(R.id.enabled);
            enabledChkBox.setChecked(enabled);

            // Repeat days
            final TextView repeatDays =
                (TextView)view.findViewById(R.id.repeat_days);
            String daysString = new Alarms.RepeatWeekdays(daysCode).toString();
            // TextUtils.SimpleStringSplitter split =
            //     new TextUtils.SimpleStringSplitter(' ');
            // split.setString(daysString);
            // Iterator<String> days = split.iterator();
            // while(days.hasNext()) {
            //     TextView textView = new TextView(context);
            //     textView.setText(days.next());
            //     // FIXME: Hard-coded style. Unable to set through
            //     // RepeatDaysTextAppearance style.
            //     // textView.setBackgroundResource(R.drawable.label_blue);
            //     textView.setTextAppearance(context, R.style.RepeatDaysTextAppearance);
            //     repeatDaysLayout.addView(textView, layoutParams);
            // }
            repeatDays.setText(daysString);

            view.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder builder =
                            new AlertDialog.Builder(view.getContext());
                        builder
                            .setTitle(label)
                            .setItems(
                                new String[] {
                                    view.getContext().getString(MENU_ITEM_RES_IDS[0]),
                                    view.getContext().getString(MENU_ITEM_RES_IDS[1])},
                                new MenuItemOnClickListener(id, label))
                            .create()
                            .show();
                    }
                });
        }

        @Override
        public View newView(Context context,
                            Cursor cursor,
                            ViewGroup parent) {
            View view = mInflater.inflate(R.layout.alarm_list_item,
                                          parent, false);
            return view;
        }

        protected void onContentChanged() {

            Log.d(TAG, "Content changed");

        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ContentResolver contentResolver = getContentResolver();
        if(contentResolver == null) {
            Log.d(TAG, "onCreate(): no content resolver");
        }

        Cursor cursor = Alarms.getAlarmCursor(contentResolver, -1);
        startManagingCursor(cursor);
        setListAdapter(new AlarmAdapter(this, cursor));


        Alarms.RepeatWeekdays weekDays = new Alarms.RepeatWeekdays();
        weekDays.addDay(Calendar.SUNDAY);
        weekDays.addDay(Calendar.MONDAY);
        weekDays.addDay(Calendar.FRIDAY);
        weekDays.addDay(Calendar.SATURDAY);

        Log.d(TAG, "==========> " + weekDays);

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

    private void editAppPreferences() {
        Log.d(TAG, "TODO: Editing application preferences");
    }

    private void showHelpDialog() {
        Log.d(TAG, "TODO: Show help dialog and copyright");
    }

    private void addAlarm() {
        Uri uri = Alarms.newAlarm(getContentResolver());
        int alarmId = Integer.parseInt(uri.getLastPathSegment());
        editAlarmSettings(alarmId);
    }

    private void editAlarmSettings(int alarmId) {
        Intent intent = new Intent(this, AlarmSettings.class);
        intent.putExtra(Alarms.INTENT_EXTRA_ALARM_ID_KEY, alarmId);
        startActivity(intent);
    }

    private int deleteAlarm(int alarmId) {
        return Alarms.deleteAlarm(getContentResolver(), alarmId);
    }
}
