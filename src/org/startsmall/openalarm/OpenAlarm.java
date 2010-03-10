/**
 *  OpenAlarm - an extensible alarm for Android
 *  Copyright (C) 2010 Liu Yen-Liang (Josh)
 *
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.startsmall.openalarm;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.util.Log;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import java.util.*;
import java.text.*;

public class OpenAlarm extends Activity implements ListView.OnKeyListener {
    private static final String TAG = "OpenAlarm";

    private static final int MENU_ITEM_ID_DELETE = 0;
    private static final int MENU_ITEM_ID_GROUP_BY = 0;

    private static final int DIALOG_ID_ABOUT = 0;
    private static final int DIALOG_ID_FILTER_BY = 1;

    private static final int MSGID_FILTER_ALARMS = 0;
    private static final int MSGID_SET_BANNER = 1;

    private static final int FILTER_BY_NONE = 0;
    private static final int FILTER_BY_ACTION = 1;
    private static final int FILTER_BY_REPEAT_DAYS = 2;

    private ListView mAlarmListView;
    private TextView mBannerTextView;
    private ImageView mFilterView;
    private Animation mSlideInLeft;
    private Animation mSlideOutRight;

    private HashMap<String, HandlerInfoCache> mHandlerInfoCacheMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (!Alarm.hasAlarms()) {
            Alarm.foreach(this, Alarms.getAlarmUri(-1), new BootService.ScheduleEnabledAlarm());
        }

        Notification.getInstance().set(this);

        Alarms.is24HourMode = Alarms.is24HourMode(this);

        initHandlerInfoCacheMap();

        updateLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayout();
    }

    private void initHandlerInfoCacheMap() {
        mHandlerInfoCacheMap = new HashMap<String, HandlerInfoCache>();

        PackageManager pm = getPackageManager();
        Iterator<ResolveInfo> handlerInfos = Alarms.queryAlarmHandlers(pm, true).iterator();
        while (handlerInfos.hasNext()) {
            ActivityInfo activityInfo = handlerInfos.next().activityInfo;
            String key = activityInfo.name;

            HandlerInfoCache cache = new HandlerInfoCache();
            cache.label = activityInfo.loadLabel(pm).toString().substring(1);
            cache.className = activityInfo.name;
            cache.icon = activityInfo.loadIcon(pm);
            mHandlerInfoCacheMap.put(key, cache);
        }
    }

    private void updateLayout() {
        setContentView(R.layout.main);

        mBannerTextView = (TextView)findViewById(R.id.banner);

        mSlideInLeft = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        mSlideOutRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);

        // mBar = (BarWidget)findViewById(R.id.bar);
        // initBarWidget();

        // Initialize ListView for alarm.
        mAlarmListView = (ListView)findViewById(android.R.id.list);
        mAlarmListView.setOnKeyListener(this);
        CursorAdapter alarmAdapter = (CursorAdapter)mAlarmListView.getAdapter();
        if (alarmAdapter == null) {
            Cursor cursor = managedQuery(Alarms.getAlarmUri(-1),
                                         AlarmColumns.QUERY_COLUMNS, null, null,
                                         AlarmColumns.DEFAULT_SORT_ORDER);
            startManagingCursor(cursor);
            mAlarmListView.setAdapter(new AlarmAdapter(this, cursor));
        }

        mFilterView = (ImageView)findViewById(R.id.filter);
        mFilterView.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    showDialog(DIALOG_ID_FILTER_BY);
                }
            });
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
            addNewAlarm();
            break;

        case R.id.menu_item_send_feedback:
            sendFeedback();
            break;

        case R.id.menu_item_about:
            showDialog(DIALOG_ID_ABOUT);
            break;
        }

        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int alarmId = item.getGroupId();
        switch(item.getItemId()) {
        case MENU_ITEM_ID_DELETE:
            // This dialog is not managed by Activity but I can't
            // find a way to pass alarmId to the
            // onCreateDialog().
            new AlertDialog.Builder(this)
                .setMessage(R.string.delete_alarm_message)
                .setTitle(R.string.confirm_alarm_deletion_title)
                .setPositiveButton(
                    android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Context context = OpenAlarm.this;
                            Alarm.getInstance(context, alarmId).delete(context);
                        }
                    })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
            break;

        default:
            break;
        }

        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        switch (keyCode) {
            // AdapterView doesn't handle DPAD_CENTER and CENTER key
            // event, it propagate key event up to ListView.
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER:
            View selectedAlarmView = mAlarmListView.getSelectedView();
            if (selectedAlarmView != null) {
                selectedAlarmView.performClick();
            }
            return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
        case DIALOG_ID_ABOUT:
            WebView helpWebView = new WebView(this);
            helpWebView.loadUrl("file:///android_asset/" +
                                getString(R.string.about_html));
            dialog = builder.
                     setTitle(R.string.about_this_application).
                     setPositiveButton(android.R.string.ok,
                                       new DialogInterface.OnClickListener() {
                                           @Override
                                           public void onClick(DialogInterface dlg, int which) {
                                               dlg.dismiss();
                                           }
                                       }).
                     setView(helpWebView).
                     create();
            break;

        case DIALOG_ID_FILTER_BY:
            View content =
                LayoutInflater.from(this).inflate(R.layout.filter, null);

            final ListView listView = (ListView)content.findViewById(android.R.id.list);

            ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(
                    this, R.array.group_by, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            final Spinner spinner = (Spinner)content.findViewById(R.id.group);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position == FILTER_BY_NONE) {
                            listView.setAdapter(null);
                        } else if (position == FILTER_BY_ACTION) {
                            PackageManager pm = getPackageManager();
                            Collection<HandlerInfoCache> handlerInfoCaches = mHandlerInfoCacheMap.values();
                            if (handlerInfoCaches.size() > 0) {
                                ArrayAdapter actionAdapter =
                                    new ArrayAdapter(OpenAlarm.this,
                                                     android.R.layout.simple_list_item_single_choice,
                                                     handlerInfoCaches.toArray());
                                listView.setAdapter(actionAdapter);
                                listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                            }
                        } else if (position == FILTER_BY_REPEAT_DAYS) {
                            DateFormatSymbols dateFormatSymbols =
                                ((SimpleDateFormat)DateFormat.getDateInstance(DateFormat.MEDIUM)).getDateFormatSymbols();
                            CharSequence[] weekdays = new CharSequence[7];
                            System.arraycopy(dateFormatSymbols.getWeekdays(),
                                             Calendar.SUNDAY,
                                             weekdays,
                                             0,
                                             7);
                            ArrayAdapter repeatDaysAdapter =
                                    new ArrayAdapter(OpenAlarm.this,
                                                     android.R.layout.simple_list_item_multiple_choice,
                                                     weekdays);
                            listView.setAdapter(repeatDaysAdapter);
                            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                        }
                    }
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

            dialog = builder.
                     setPositiveButton(
                         android.R.string.ok,
                         new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dlg, int which) {
                                 int filterBy = spinner.getSelectedItemPosition();
                                 switch (filterBy) {
                                 case FILTER_BY_NONE: {
                                     // Reset banner to application name;

                                     Message msg = mHandler.obtainMessage(MSGID_SET_BANNER,
                                                                          0, 0,
                                                                          getString(R.string.app_name));
                                     mHandler.sendMessage(msg);

                                     msg = mHandler.obtainMessage(MSGID_FILTER_ALARMS, FILTER_BY_NONE, 0);
                                     mHandler.sendMessage(msg);
                                     break;
                                 }

                                 case FILTER_BY_ACTION: {
                                     int position = listView.getCheckedItemPosition();
                                     if (position > -1) {
                                         HandlerInfoCache cache = (HandlerInfoCache)listView.getItemAtPosition(position);
                                         Message msg = mHandler.obtainMessage(MSGID_SET_BANNER,
                                                                              0, 0, cache.label);
                                         mHandler.sendMessage(msg);

                                         msg = mHandler.obtainMessage(MSGID_FILTER_ALARMS,
                                                                      FILTER_BY_ACTION,
                                                                      0, cache);
                                         mHandler.sendMessage(msg);
                                     }
                                     break;
                                 }
                                 }

                                 dlg.dismiss();
                             }
                         }).
                     setView(content).
                     create();
            break;

        default:
            throw new IllegalArgumentException("illegal dialog id");
        }
        return dialog;
    }

    private void sendFeedback() {
        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setData(Uri.parse("mailto:yenliangl@gmail.com"));
        i.putExtra(Intent.EXTRA_SUBJECT, "[OpenAlarm] ");

        String deviceInfo =
            String.format("Build.DISPLAY=%s\nBuild.FINGERPRINT=%s\nBuild.HOST=%s\nBuild.ID=%s\nBuild.MODEL=%s\nBuild.PRODUCT=%s\nBuild.TYPE=%s\nBuild.USER=%s\n",
                          Build.DISPLAY, Build.FINGERPRINT, Build.HOST, Build.ID, Build.MODEL, Build.PRODUCT, Build.TYPE, Build.USER);

        i.putExtra(Intent.EXTRA_TEXT,
                   getString(R.string.feedback_mail_content, deviceInfo));
        startActivity(
            Intent.createChooser(
                i, getString(R.string.menu_item_send_feedback)));
    }

    private void addNewAlarm() {
        Alarm alarm = Alarm.getInstance(this);
        editAlarm(alarm.getIntField(Alarm.FIELD_ID));
    }

    private void editAlarm(int alarmId) {
        Intent intent = new Intent(this, AlarmSettings.class);
        intent.putExtra(AlarmColumns._ID, alarmId);
        startActivity(intent);
    }

    class AlarmAdapter extends CursorAdapter {
        private View.OnClickListener mOnClickListener;
        private View.OnCreateContextMenuListener mOnCreateContextMenuListener;
        private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;

        public AlarmAdapter(Context context, Cursor c) {
            super(context, c);

            mOnClickListener =
                new View.OnClickListener() {
                    // @Override
                    public void onClick(View view) {
                        DataHolder attachment = (DataHolder)view.getTag();
                        editAlarm(attachment.alarmId);
                    }
                };

            mOnCreateContextMenuListener =
                new View.OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu,
                                                    View view,
                                                    ContextMenu.ContextMenuInfo menuInfo) {
                        DataHolder attachment = (DataHolder)view.getTag();
                        final int alarmId = attachment.alarmId;
                        String label = Alarm.getInstance(OpenAlarm.this, alarmId).getStringField(Alarm.FIELD_LABEL);

                        menu.setHeaderTitle(label);
                        menu.add(alarmId, MENU_ITEM_ID_DELETE, 0, R.string.menu_item_delete_alarm);
                    }
                };

            mOnCheckedChangeListener =
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        View parent = (View)buttonView.getParent();
                        DataHolder attachment = (DataHolder)parent.getTag();
                        final int alarmId = attachment.alarmId;
                        Context context = (Context)OpenAlarm.this;
                        Alarm alarm = Alarm.getInstance(context, alarmId);
                        if (alarm.isValid()) {
                            // Alarm looks good. Enable it or disable it.
                            alarm.update(
                                context,
                                isChecked,
                                alarm.getStringField(Alarm.FIELD_LABEL),
                                alarm.getIntField(Alarm.FIELD_HOUR_OF_DAY),
                                alarm.getIntField(Alarm.FIELD_MINUTES),
                                alarm.getIntField(Alarm.FIELD_REPEAT_DAYS),
                                alarm.getStringField(Alarm.FIELD_HANDLER),
                                alarm.getStringField(Alarm.FIELD_EXTRA));
                        } else {
                            // Alarm can't be set because its
                            // settings are't good enough. Bring
                            // up its Settings activity
                            // automatically for user to chage.
                            if (isChecked) {
                                int errCode = alarm.getErrorCode();
                                int errMsgResId = R.string.alarm_handler_unset_message;
                                if (errCode == Alarm.ERROR_NO_REPEAT_DAYS) {
                                    errMsgResId = R.string.alarm_repeat_days_unset_message;
                                }
                                Toast.makeText(context, errMsgResId, Toast.LENGTH_LONG).show();
                                parent.performClick();
                                buttonView.setChecked(false);
                            }
                        }
                    }
                };
        }

        public void bindView(View view, Context context, Cursor cursor) {
            Alarm alarm = Alarm.getInstance(cursor);

            DataHolder attachment = (DataHolder)view.getTag();
            attachment.alarmId = alarm.getIntField(Alarm.FIELD_ID);

            // Label
            attachment.labelView.setText(alarm.getStringField(Alarm.FIELD_LABEL));

            // Time of an alarm: hourOfDay and minutes
            attachment.timeAmPmView.setTime(
                alarm.getIntField(Alarm.FIELD_HOUR_OF_DAY),
                alarm.getIntField(Alarm.FIELD_MINUTES));

            // Enabled?
            final CheckBox enabledCheckBox = attachment.enabledView;
            // Set checkbox's listener to null. If set, the
            // defined listener will try to update the database
            // and make bindView() called which enters an
            // infinite loop.
            enabledCheckBox.setOnCheckedChangeListener(null);
            enabledCheckBox.setChecked(alarm.getBooleanField(Alarm.FIELD_ENABLED));
            enabledCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);

            // Action
            final TextView actionTextView = attachment.actionView;
            String handler = alarm.getStringField(Alarm.FIELD_HANDLER);
            if (!TextUtils.isEmpty(handler)) {
                HandlerInfoCache handlerCache = mHandlerInfoCacheMap.get(handler);
                actionTextView.setVisibility(View.VISIBLE);
                actionTextView.setText(handlerCache.label);
            } else {
                actionTextView.setVisibility(View.GONE);
                actionTextView.setText("");
            }

            // RepeatDays
            final LinearLayout repeatDaysView = attachment.repeatDaysView;
            repeatDaysView.removeAllViews();
            Iterator<String> days =
                Alarms.RepeatWeekdays.toStringList(
                    alarm.getIntField(Alarm.FIELD_REPEAT_DAYS),
                    context.getString(R.string.repeat_on_everyday),
                    context.getString(R.string.no_repeat_days)).iterator();

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(1, 1, 1, 1);

            while(days.hasNext()) {
                TextView dayLabel = new TextView(context);
                dayLabel.setClickable(false);
                dayLabel.setBackgroundResource(R.drawable.rounded_background);
                dayLabel.setTextAppearance(context, R.style.RepeatDaysTextAppearance);
                dayLabel.setText(days.next());
                repeatDaysView.addView(dayLabel, params);
            }
            repeatDaysView.setVisibility(View.VISIBLE);
        }

        public View newView(Context context, Cursor c, ViewGroup parent) {
            // LayoutInflater inflater = getLayoutInflater();
            // View view = inflater.inflate(R.layout.alarm_list_item, parent, false);
            View view = LayoutInflater.from(context).inflate(R.layout.alarm_list_item, parent, false);

            DataHolder attachment = new DataHolder();
            attachment.labelView = (TextView)view.findViewById(R.id.label);
            attachment.timeAmPmView = (TimeAmPmView)view.findViewById(R.id.time_am_pm);
            attachment.actionView = (TextView)view.findViewById(R.id.action);
            attachment.enabledView = (CheckBox)view.findViewById(R.id.enabled);
            attachment.repeatDaysView = (LinearLayout)view.findViewById(R.id.repeat_days);
            view.setTag(attachment);

            view.setOnClickListener(mOnClickListener);

            // The ContextMenu.ContextMenuInfo object is returned
            // by getContextMenuInfo() overriden by
            // clients. Here, it is null.
            view.setOnCreateContextMenuListener(mOnCreateContextMenuListener);

            return view;
        }

        protected void onContentChanged() {
            super.onContentChanged();
            Notification.getInstance().set(OpenAlarm.this);
        }
    }

    static class DataHolder {
        int alarmId;
        TextView labelView;
        TextView actionView;
        TimeAmPmView timeAmPmView;
        CheckBox enabledView;
        LinearLayout repeatDaysView;
    }

    private Handler mHandler = new Handler() {
            public void handleMessage(Message msg) {
                int arg1 = msg.arg1;
                int arg2 = msg.arg2;
                Object obj = msg.obj;

                switch (msg.what) {
                case MSGID_FILTER_ALARMS:
                    // arg1 is the group, arg2
                    Cursor cursor = null;
                    if (arg1 == FILTER_BY_NONE) {
                        cursor = managedQuery(Alarms.getAlarmUri(-1),
                                              AlarmColumns.QUERY_COLUMNS, null, null,
                                              AlarmColumns.DEFAULT_SORT_ORDER);
                    } else if (arg1 == FILTER_BY_ACTION) {
                        HandlerInfoCache cache = (HandlerInfoCache)obj;
                        cursor = managedQuery(Alarms.getAlarmUri(-1),
                                              AlarmColumns.QUERY_COLUMNS,
                                              AlarmColumns.HANDLER + "=?",
                                              new String[]{cache.className},
                                              AlarmColumns.DEFAULT_SORT_ORDER);
                    } else if (arg1 == FILTER_BY_REPEAT_DAYS) {
                        // int dayCode = msg.arg2;
                        // cursor = managedQuery(Alarm.getAlarmUri(-1),
                        //                       AlarmColumns.QUERY_COLUMNS,
                        //                       AlarmColumns.REPEAT_DAYS + " && " + dayCode + " = "
                        //                       AlarmColumns.HANDLER + "=?",
                        //                       new String[]{handler},
                        //                       AlarmColumns.DEFAULT_SORT_ORDER);
                    }

                    if (cursor != null) {
                        // Stop managing current cursor held by mAlarmListView.
                        CursorAdapter adapter = (CursorAdapter)mAlarmListView.getAdapter();
                        if (adapter != null) {
                            Cursor oldCursor = adapter.getCursor();
                            stopManagingCursor(oldCursor);

                            startManagingCursor(cursor);
                            adapter.changeCursor(cursor);
                        }
                    }
                    break;
                case MSGID_SET_BANNER:
                    String text = (String)obj;
                    mBannerTextView.setText(text);
                    break;
                }
            }
        };

    static class HandlerInfoCache {
        String label;
        String className;
        Drawable icon;

        public String toString() {
            return label;
        }
    }
}
