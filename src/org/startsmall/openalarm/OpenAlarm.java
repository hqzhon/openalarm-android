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
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import com.admob.android.ads.AdView;
import java.util.*;

public class OpenAlarm extends ExpandableListActivity {
    private static final String TAG = "OpenAlarm";

    private static final int MENU_ITEM_ID_DELETE = 0;
    private static final int DIALOG_ID_ABOUT = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        // Query currently installed action handlers and create a
        // list of group data map.
        PackageManager pm = getPackageManager();
        List<ResolveInfo> infoList = Alarms.queryAlarmHandlers(pm, true);
        List<HashMap<String, Object>> groupData = new ArrayList<HashMap<String, Object>>(infoList.size() + 1);
        Iterator<ResolveInfo> infoIter = infoList.iterator();
        while (infoIter.hasNext()) {
            ActivityInfo activityInfo = infoIter.next().activityInfo;

            HashMap<String, Object> map = new HashMap<String, Object>();
            String label = activityInfo.loadLabel(pm).toString();
            String className = activityInfo.name;
            Drawable icon = activityInfo.loadIcon(pm);

            map.put(AlarmAdapter.GROUP_DATA_KEY_LABEL, label);
            map.put(AlarmAdapter.GROUP_DATA_KEY_HANDLER, className);
            map.put(AlarmAdapter.GROUP_DATA_KEY_ICON, icon);

            groupData.add(map);
        }

        // Null handler group
        HashMap<String, Object> nullHandlerMap = new HashMap<String, Object>();
        nullHandlerMap.put(AlarmAdapter.GROUP_DATA_KEY_LABEL,
                           getString(R.string.uncategorized));
        nullHandlerMap.put(AlarmAdapter.GROUP_DATA_KEY_HANDLER, "");
        nullHandlerMap.put(AlarmAdapter.GROUP_DATA_KEY_ICON, null);
        groupData.add(nullHandlerMap);

        AlarmAdapter adapter = new AlarmAdapter(this, groupData,
                                                new String[]{AlarmAdapter.GROUP_DATA_KEY_LABEL,
                                                             AlarmAdapter.GROUP_DATA_KEY_ICON},
                                                new int[]{R.id.label, R.id.icon});
        setListAdapter(adapter);

        Notification.getInstance().set(this);

        Alarms.is24HourMode = Alarms.is24HourMode(this);

        showAds(true);
    }

    // @Override
    // public void onStop() {
    //     Log.d(TAG, "===> onStop()");
    //     super.onStop();

    //     // Deactivate all opened cursor helpers.
    //     // AlarmAdapter adapter = (AlarmAdapter)getExpandableListAdapter();
    //     // adapter.deactivateChildCursors();
    // }

    // @Override
    // public void onDestroy() {
    //     Log.d(TAG, "===> onDestroy()");
    //     super.onDestroy();
    // }

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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            // ListView doesn't handle DPAD_CENTER and CENTER key
            // event, we have to handle it in Activity.
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER:
            getExpandableListView().getSelectedView().performClick();
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

    private void sendFeedback() {
        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setData(Uri.parse("mailto:yenliangl@gmail.com"));
        i.putExtra(Intent.EXTRA_SUBJECT, "[OpenAlarm] ");
        i.putExtra(Intent.EXTRA_TEXT, "Hi Josh!" );
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

    private void showAds(boolean show) {
        AdView ads = (AdView)findViewById(R.id.ad);
        int currentVisibility = ads.getVisibility();
        if (show && currentVisibility == View.GONE) {
            ads.setVisibility(View.VISIBLE);
        } else {
            ads.setVisibility(View.GONE);
        }
    }

    private class AlarmAdapter extends ExpandableAlarmAdapter {
        private View.OnClickListener mOnClickListener;
        private View.OnCreateContextMenuListener mOnCreateContextMenuListener;
        private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;
        private LayoutInflater mInflater;
        private int mShowAdsCount = 0;

        AlarmAdapter(Context context,
                     List<HashMap<String, Object>> groupData, String[] groupFrom, int[] groupTo) {
            super(context, groupData, groupFrom, groupTo);

            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            mOnClickListener =
                new View.OnClickListener() {
                    // @Override
                    public void onClick(View view) {
                        Bundle attachment = (Bundle)view.getTag();

                        int alarmId = attachment.getInt(AlarmColumns._ID);
                        editAlarm(alarmId);
                    }
                };

            mOnCreateContextMenuListener =
                new View.OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu,
                                                    View view,
                                                    ContextMenu.ContextMenuInfo menuInfo) {
                        Bundle attachment = (Bundle)view.getTag();

                        int alarmId = attachment.getInt(AlarmColumns._ID);
                        String label = Alarm.getInstance(OpenAlarm.this, alarmId).getStringField(Alarm.FIELD_LABEL);

                        menu.setHeaderTitle(label);
                        menu.add(alarmId, MENU_ITEM_ID_DELETE, 0, R.string.menu_item_delete_alarm);

                        showAdsChecked();
                    }
                };

            mOnCheckedChangeListener =
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        View parent = (View)buttonView.getParent();
                        Bundle attachment = (Bundle)parent.getTag();
                        int alarmId = attachment.getInt(AlarmColumns._ID);
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

                            showAdsChecked();

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

        @Override
        protected View newGroupView(ViewGroup parent) {
            return mInflater.inflate(R.layout.group, parent, false);
        }

        @Override
        protected void bindGroupView(View view, Map<String, ?> data, int childrenCount, String[] from, int[] to) {
            final int len = to.length;

            for (int i = 0; i < len; i++) {
                View v = view.findViewById(to[i]);
                if (v instanceof TextView) {
                    TextView tv = (TextView)v;
                    tv.setText((String)data.get(from[i]));
                } else if (v instanceof ImageView) {
                    ImageView iv = (ImageView)v;
                    Drawable icon = (Drawable)data.get(from[i]);
                    if (icon == null) {
                        iv.setImageResource(R.drawable.null_handler);
                    } else {
                        iv.setImageDrawable(icon);
                    }
                }
            }

            //
            TextView childrenTextView = (TextView)view.findViewById(R.id.children_count);
            childrenTextView.setText("(" + childrenCount + ")");
        }

        @Override
        protected View newChildView(Context context, Cursor childCursor, boolean isLastChild, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.alarm_list_item, parent, false);

            Bundle attachment = new Bundle();
            view.setTag(attachment);

            // Should follow Android's design. Click to go to
            // item's default activity and long-click to go to
            // item's extended activities. Here, the first-class
            // activity is to edit item's settings.
            view.setOnClickListener(mOnClickListener);

            // The context menu listener of the view must be set
            // in order for its parent's onCreateMenu() to be
            // called to create context menu. This should be a
            // bug but I'm not very sure.

            // The ContextMenu.ContextMenuInfo object is returned
            // by getContextMenuInfo() overriden by
            // clients. Here, it is null.
            view.setOnCreateContextMenuListener(mOnCreateContextMenuListener);

            return view;
        }

        @Override
        protected void bindChildView(View view, Context context, Cursor childCursor, boolean isLastChild) {
            Alarm alarm = Alarm.getInstance(childCursor);

            Bundle attachment = (Bundle)view.getTag();
            attachment.putInt(AlarmColumns._ID,
                              alarm.getIntField(Alarm.FIELD_ID));

            // Label
            final TextView labelView =
                (TextView)view.findViewById(R.id.label);
            labelView.setText(alarm.getStringField(Alarm.FIELD_LABEL));

            // Time of an alarm: hourOfDay and minutes
            final CompoundTimeTextView timeTextView =
                (CompoundTimeTextView)view.findViewById(R.id.time);
            timeTextView.setTime(
                alarm.getIntField(Alarm.FIELD_HOUR_OF_DAY),
                alarm.getIntField(Alarm.FIELD_MINUTES));

            // Enabled?
            final CheckBox enabledCheckBox =
                (CheckBox)view.findViewById(R.id.enabled);
            // Set checkbox's listener to null. If set, the
            // defined listener will try to update the database
            // and make bindView() called which enters an
            // infinite loop.
            enabledCheckBox.setOnCheckedChangeListener(null);
            enabledCheckBox.setChecked(alarm.getBooleanField(Alarm.FIELD_ENABLED));
            enabledCheckBox.setOnCheckedChangeListener(mOnCheckedChangeListener);

            // RepeatDays
            final LinearLayout repeatDaysView =
                (LinearLayout)view.findViewById(R.id.repeat_days);
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

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            Notification.getInstance().set(OpenAlarm.this);
        }

        @Override
        public void onGroupCollapsed(int groupPosition) {
            super.onGroupCollapsed(groupPosition);
            showAdsChecked();
        }

        @Override
        public void onGroupExpanded(int groupPosition) {
            super.onGroupExpanded(groupPosition);
            showAdsChecked();
        }

        private void showAdsChecked() {
            boolean show = false;
            if (mShowAdsCount > 3) {
                mShowAdsCount = 0;
                show = true;
            }
            OpenAlarm.this.showAds(show);
            mShowAdsCount++;
        }
    }
}
