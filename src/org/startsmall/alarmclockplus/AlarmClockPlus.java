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
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
// import android.view.animation.AnimationUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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

        private class EditAlarmMenuItemListener implements MenuItem.OnMenuItemClickListener {
            /**
             * Edit preferences of this alarm.
             *
             * @param item Menu item
             *
             * @return whether or not this menu item click is consumed.
             */
            public boolean onMenuItemClick(MenuItem item) {
                Log.d(TAG, "Edit settings for alarm " + item.getItemId());
                Intent intent = new Intent(AlarmClockPlus.this,
                                           AlarmSettings.class);
                intent.putExtra(Alarms.INTENT_EXTRA_ALARM_ID_KEY,
                                item.getItemId());
                startActivity(intent);
                return true;
            }
        }

        private class DeleteAlarmMenuItemListener implements MenuItem.OnMenuItemClickListener {
            private class ButtonListener implements DialogInterface.OnClickListener {
                private int mAlarmItemId;

                public ButtonListener(int alarmItemId) {
                    mAlarmItemId = alarmItemId;
                }

                public void onClick(DialogInterface dialog, int which) {
                    switch(which) {
                    case DialogInterface.BUTTON_POSITIVE: // delete alarm
                        AlarmClockPlus.this.deleteAlarm(mAlarmItemId);
                        break;
                    default: // BUTTON_NEGATIVE is ignored.
                        break;
                    }
                }
            }

            private String mAlarmLabel;

            public DeleteAlarmMenuItemListener(String label) {
                mAlarmLabel = label;
            }

            public boolean onMenuItemClick(MenuItem item) {
                Log.d(TAG, "Delete alarm " + item.getItemId());
                ButtonListener listener = new ButtonListener(item.getItemId());
                new AlertDialog.Builder(AlarmClockPlus.this).
                    setMessage(R.string.delete_alarm_message).
                    setTitle(mAlarmLabel).
                    setPositiveButton("Yes", listener).
                    setNegativeButton("No", listener).
                    create().
                    show();
                return true;
            }
        }

        private LayoutInflater mLayoutInflater;
        private EditAlarmMenuItemListener mEditAlarmMenuItemListener;

        public AlarmAdapter(Context context, Cursor c) {
            super(context, c);

            mLayoutInflater = AlarmClockPlus.this.getLayoutInflater();
            mEditAlarmMenuItemListener = new EditAlarmMenuItemListener();
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

            // Label of Alarm
            final TextView labelView =
                (TextView)view.findViewById(R.id.label);
            labelView.setText(label);

            // Enable this alarm?
            CheckBox enabledChkBox =
                (CheckBox)view.findViewById(R.id.enabled);
            enabledChkBox.setChecked(enabled);

            // Repeat days
            ViewGroup.LayoutParams layoutParams =
                new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            final LinearLayout repeatDaysLayout =
                (LinearLayout)view.findViewById(R.id.repeat_days);
            String daysString =
                new Alarms.RepeatWeekdays(daysCode).toString();
            TextUtils.SimpleStringSplitter split =
                new TextUtils.SimpleStringSplitter(' ');
            split.setString(daysString);
            Iterator<String> days = split.iterator();
            while(days.hasNext()) {
                TextView textView = new TextView(context);
                textView.setText(days.next());
                //
                textView.setBackgroundResource(R.drawable.blue);
                textView.setTextAppearance(context,
                                           R.style.RepeatDaysTextAppearance);
                repeatDaysLayout.addView(textView, layoutParams);
            }

            // Create context menu for this view.
            view.setOnCreateContextMenuListener(
                new View.OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu,
                                                    View view,
                                                    ContextMenuInfo menuInfo) {
                        menu.setHeaderTitle(labelView.getText());
                        menu.add(Menu.NONE,
                                 id, /* id of the alarm */
                                 Menu.NONE, /* don't care about the order */
                                 R.string.menu_item_edit_alarm).
                            setOnMenuItemClickListener(mEditAlarmMenuItemListener);

                        menu.add(Menu.NONE,
                                 id, /* id of the alarm */
                                 Menu.NONE, /* don't care about the order */
                                 R.string.menu_item_delete_alarm).
                            setOnMenuItemClickListener(
                                new DeleteAlarmMenuItemListener(labelView.getText().toString()));
                    }
                });

            view.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        LinearLayout extraView = (LinearLayout)view.findViewById(R.id.extra);

                        if(extraView.isShown()) {
                            extraView.setVisibility(View.GONE);
                        } else {
                            extraView.setVisibility(View.VISIBLE);
                        }
                    }
                });


            Button editButton = (Button)view.findViewById(R.id.edit);
            editButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        Intent intent = new Intent(AlarmClockPlus.this,
                                                   AlarmSettings.class);
                        intent.putExtra(Alarms.INTENT_EXTRA_ALARM_ID_KEY, id);
                        startActivity(intent);
                    }
                });




















        }

        @Override
        public View newView(Context context,
                            Cursor cursor,
                            ViewGroup parent) {
            View view = mLayoutInflater.inflate(R.layout.alarm_list_item,
                                                parent, false);
            return view;
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

    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        Log.d(TAG, "ListActivity.onCreateContextMenu()");
    }

    private void editAppPreferences() {
        Log.d(TAG, "TODO: Editing application preferences");
    }

    private void showHelpDialog() {
        Log.d(TAG, "TODO: Show help dialog and copyright");
    }

    private void addAlarm() {
        Log.d(TAG, "TODO: Adding an alarm");
    }

    private int deleteAlarm(int alarmId) {
        Log.d(TAG, "TODO: Deleting an alarm");
        // ContentResolver contentResolver = getContentResolver();
        // return Alarms.deleteAlarm(contentResolver, alarmId);
        return 0;
    }
}
