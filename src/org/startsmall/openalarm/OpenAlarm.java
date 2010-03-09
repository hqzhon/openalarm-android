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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import java.util.*;

public class OpenAlarm extends Activity
    implements ListView.OnKeyListener, BarWidget.OnChildSelectionChangedListener {
    private static final String TAG = "OpenAlarm";

    private static final int MENU_ITEM_ID_DELETE = 0;
    private static final int DIALOG_ID_ABOUT = 0;

    // Build tabs by querying alarm handlers
    private static final String GROUP_DATA_KEY_LABEL = "label";
    private static final String GROUP_DATA_KEY_HANDLER = "handler";
    private static final String GROUP_DATA_KEY_ICON = "icon";

    private int mShowAdsCount = 0;

    private ListView mAlarmListView;
    private TextView mBannerTextView;
    private BarWidget mBar;
    private Animation mSlideInLeft;
    private Animation mSlideOutRight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (!Alarm.hasAlarms()) {
            Alarm.foreach(this, Alarms.getAlarmUri(-1), new BootService.ScheduleEnabledAlarm());
        }

        Notification.getInstance().set(this);

        Alarms.is24HourMode = Alarms.is24HourMode(this);

        updateLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayout();
    }

    private void updateLayout() {
        setContentView(R.layout.main);

        mBannerTextView = (TextView)findViewById(R.id.banner);

        mSlideInLeft = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        mSlideOutRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);

        mBar = (BarWidget)findViewById(R.id.bar);
        initBarWidget();

        // Initialize ListView for alarm.
        mAlarmListView = (ListView)findViewById(android.R.id.list);
        mAlarmListView.setOnKeyListener(this);
        CursorAdapter alarmAdapter = (CursorAdapter)mAlarmListView.getAdapter();
        if (alarmAdapter == null) {
            View v = mBar.getChildAt(0);
            if (v != null && v instanceof ImageView) {
                ViewAttachment attachment = (ViewAttachment)v.getTag();
                Cursor cursor = getAlarmCursor(attachment.handler);
                startManagingCursor(cursor);
                mAlarmListView.setAdapter(new AlarmAdapter(this, cursor));
            }
        }
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

        default:
            throw new IllegalArgumentException("illegal dialog id");
        }
        return dialog;
    }

    static class ViewAttachment {
        String label;
        String handler;
    }

    private void initBarWidget() {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> infoList = Alarms.queryAlarmHandlers(pm, true);
        Iterator<ResolveInfo> infoIter = infoList.iterator();
        while (infoIter.hasNext()) {
            ActivityInfo activityInfo = infoIter.next().activityInfo;

            View view = createBarChild(activityInfo.loadLabel(pm).toString().substring(1),
                                       activityInfo.name,
                                       activityInfo.loadIcon(pm));
            mBar.addView(view);
        }

        // Null handler group
        View view = createBarChild(getString(R.string.uncategorized),
                                   "",
                                   getResources().getDrawable(R.drawable.null_handler));
        mBar.addView(view);
        mBar.setOnChildSelectionChangedListener(this);
    }

    @Override
    public void onChildSelectionChanged(int childIndex, boolean click) {
        View v = mBar.getChildAt(childIndex);
        CursorAdapter adapter = (CursorAdapter)mAlarmListView.getAdapter();

        ViewAttachment attachment = (ViewAttachment)v.getTag();
        Cursor cursor = getAlarmCursor(attachment.handler);
        adapter.changeCursor(cursor);
        mBannerTextView.setText(attachment.label);
    }

    private View createBarChild(String label, String handler, Drawable icon) {
        ViewAttachment attachment = new ViewAttachment();
        attachment.label = label;
        attachment.handler = handler;

        ImageView view = new ImageView(this);
        view.setScaleType(ImageView.ScaleType.FIT_XY);
        view.setImageDrawable(icon);
        view.setTag(attachment);
        view.setBackgroundResource(R.drawable.bar_button_background);

        return view;
    }

    private Cursor getAlarmCursor(String handler) {
        Cursor c =
            getContentResolver().query(
                Alarms.getAlarmUri(-1),
                AlarmColumns.QUERY_COLUMNS,
                AlarmColumns.HANDLER + "=?",
                new String[]{handler},
                AlarmColumns.DEFAULT_SORT_ORDER);
        return c;
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
        TimeAmPmView timeAmPmView;
        CheckBox enabledView;
        LinearLayout repeatDaysView;
    }

    private boolean startMediaService() {
        ComponentName service =
            new ComponentName("com.htc.music",
                              "com.htc.music.MediaPlaybackService");
        ComponentName started = startService(new Intent().setComponent(service));
        if (started != null && started.equals(service)) {
            Log.i(TAG, "===> Started com.htc.music.MediaPlaybackService...");
            return true;
        }

        service =
            new ComponentName("com.android.music",
                              "com.android.music.MediaPlaybackService");
        started = startService(new Intent().setComponent(service));
        if (started != null && started.equals(service)) {
            Log.i(TAG, "===> Started com.android.music.MediaPlaybackService...");
            return true;
        }

        return false;
    }
}
