/**
 * @file   AlarmSettings.java
 * @author  <yenliangl@gmail.com>
 * @date   Wed Oct  7 16:05:00 2009
 *
 * @brief  Settings for an alarm.
 *
 *
 */
package org.startsmall.alarmclockplus.preference;

import org.startsmall.alarmclockplus.*;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.TimePicker;

import java.util.*;
import java.lang.reflect.Method;

public class AlarmSettings extends PreferenceActivity {
    private static final String TAG = "AlarmSettings";
    private static final int LABEL_INPUT_DIALOG = 1;
    private static final int TIME_PICK_DIALOG = 2;
    private static final int ACTION_PICK_DIALOG = 3;

    AlarmTimePreference mTimePreference;
    AlarmLabelPreference mLabelPreference;
    AlarmActionPreference mActionPreference;
    AlarmRepeatOnDialogPreference mRepeatOnPreference;
    PreferenceCategory mExtraSettingsCategory;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.alarm_settings);

        PreferenceManager preferenceManager = getPreferenceManager();
        mTimePreference =
            (AlarmTimePreference)preferenceManager.findPreference(
                getString(R.string.alarm_settings_time_key));
        mLabelPreference =
            (AlarmLabelPreference)preferenceManager.findPreference(
                getString(R.string.alarm_settings_label_key));
        mActionPreference =
            (AlarmActionPreference)preferenceManager.findPreference(
                getString(R.string.alarm_settings_action_key));
        mRepeatOnPreference =
            (AlarmRepeatOnDialogPreference)preferenceManager.findPreference(
                getString(R.string.alarm_settings_repeat_days_key));
        mRepeatOnPreference.setOnRepeatWeekdaysSetListener(
            new AlarmRepeatOnDialogPreference.OnRepeatWeekdaysSetListener() {
                public void onRepeatWeekdaysSet(Alarms.RepeatWeekdays weekdays) {
                    mRepeatOnPreference.setSummary(weekdays.toString());
                }
            });
        mExtraSettingsCategory =
            (PreferenceCategory)preferenceManager.findPreference(
                getString(R.string.alarm_settings_extra_category_key));

        Intent intent = getIntent();
        int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);
        if(alarmId == -1) {
            throw new IllegalArgumentException("invalid alarm id");
        }

        Uri alarmUri = Alarms.getAlarmUri(alarmId);
        Alarms.forEachAlarm(
            this,
            alarmUri,
            new Alarms.OnVisitListener() {
                public void onVisit(final Context context,
                                    final int id,
                                    final String label,
                                    final int hour,
                                    final int minutes,
                                    final int atTimeInMillis,
                                    final int repeatDays,
                                    final boolean enabled,
                                    final String action,
                                    final String extra) {
                    mLabelPreference.setPreferenceValue(label);
                    mTimePreference.setPreferenceValue(hour * 100 + minutes);
                    mRepeatOnPreference.setPreferenceValue(repeatDays);

                    // TODO: Load extra settings for this action.
                    mActionPreference.setValue(action);
                    AlarmSettings.this.loadExtraSettingsForActionHandler(
                        action);
                }
            });

        loadActionHandlers();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        int id = -1;
        if(preference == mTimePreference) {
            id = TIME_PICK_DIALOG;
        } else if(preference == mLabelPreference) {
            id = LABEL_INPUT_DIALOG;
        } else if(preference == mActionPreference) {
            id = ACTION_PICK_DIALOG;
        }

        if(id != -1) {
            showDialog(id);
        }
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
            event.getRepeatCount() == 0) {

            Intent intent = getIntent();
            int alarmId =
                intent.getIntExtra(Alarms.AlarmColumns._ID, -1);
            if(alarmId == -1) {
                throw new IllegalArgumentException("invalid alarm id");
            } else {
                Uri alarmUri = Alarms.getAlarmUri(alarmId);

                final String label = (String)mLabelPreference.getPreferenceValue();
                final int time = (Integer)mTimePreference.getPreferenceValue();
                final int hourOfDay = time / 100;
                final int minutes = time % 100;
                final int repeatOnCode = mRepeatOnPreference.getPreferenceValue();
                final String action = (String)mActionPreference.getValue();
                final String extra =
                    getValueOfExtraSettings(mExtraSettingsCategory);

                Intent result = new Intent();
                result.putExtra(Alarms.AlarmColumns._ID, alarmId);
                result.putExtra(Alarms.AlarmColumns.LABEL, label);
                result.putExtra(Alarms.AlarmColumns.HOUR, hourOfDay);
                result.putExtra(Alarms.AlarmColumns.MINUTES, minutes);
                result.putExtra(Alarms.AlarmColumns.REPEAT_DAYS, repeatOnCode);
                result.putExtra(Alarms.AlarmColumns.ACTION, action);
                if(!TextUtils.isEmpty(extra)) {
                    result.putExtra(Alarms.AlarmColumns.EXTRA, extra);
                }
                setResult(RESULT_OK, result);
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    protected Dialog onCreateDialog(int dialogId) {
        Dialog dialog = new Dialog(this);
        switch(dialogId) {
        case TIME_PICK_DIALOG:
            dialog = createTimePickDialog();
            break;

        case LABEL_INPUT_DIALOG:
            dialog = createLabelInputDialog();
            break;

        case ACTION_PICK_DIALOG:
            dialog = createActionPickDialog();
            break;

        default:
            break;
        }

        return dialog;
    }

    protected void onPause() {
        super.onPause();

    }

    private Dialog createTimePickDialog() {
        int time = (Integer)mTimePreference.getPersistedValue();
        final int hourOfDay = time / 100;
        final int minutes = time % 100;

        return new TimePickerDialog(
            this,
            new TimePickerDialog.OnTimeSetListener() {
                public void onTimeSet(TimePicker view,
                                      int hourOfDay,
                                      int minutes) {
                    mTimePreference.setPreferenceValue(
                        hourOfDay * 100 + minutes);
                }
            },
            hourOfDay,
            minutes,
            true);
    }

    private Dialog createLabelInputDialog() {
        String label = (String)mLabelPreference.getPersistedValue();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
            .setTitle(R.string.alarm_settings_input_label_dialog_title)
            .setPositiveButton(
                R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        Dialog dialog = (Dialog)d;
                        switch(which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            EditText editText = (EditText)dialog.findViewById(R.id.input);
                            mLabelPreference.setPreferenceValue(
                                editText.getText().toString());
                            break;
                        }
                    }
                })
            .setNegativeButton(R.string.cancel, null);

        LayoutInflater inflater =
            (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView =
            inflater.inflate(R.layout.text_input_dialog_widget, null);
        ((EditText)contentView).setText(label);
        return builder.setView(contentView).create();
    }

    private Dialog createActionPickDialog() {
        loadActionHandlers();
        return mActionPreference.getDialog();

        // int actionEntryIndex = (Integer)mActionPreference.getPersistedValue();

        // AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // builder
        //     .setTitle(R.string.alarm_settings_action_dialog_title)
        //     .setSingleChoiceItems(
        //         mActionPreference.getEntries(),
        //         actionEntryIndex,
        //         new DialogInterface.OnClickListener() {
        //             public void onClick(DialogInterface dialog,
        //                                 int which) {
        //                 mActionPreference.setPreferenceValue(which);
        //                 AlarmSettings.this.loadExtraSettingsForActionHandler(mActionPreference.getValue().toString());
        //                 dialog.dismiss();
        //             }
        //         });

        // return builder.create();
    }

    private void loadActionHandlers() {
        PackageManager pm = getPackageManager();
        Intent queryIntent = new Intent(Alarms.ALARM_ACTION);
        queryIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);

        List<ResolveInfo> actions = pm.queryBroadcastReceivers(queryIntent, 0);

        CharSequence[] entries = new CharSequence[actions.size()];
        CharSequence[] entryValues = new CharSequence[actions.size()];
        // Class<? extends BroadcastReceiver>[] entryValues = new Class<? extends BroadcastReceiver>[actions.size()];
        for(int i = 0; i < actions.size(); i++) {
            ResolveInfo resInfo = actions.get(i);
            ActivityInfo info = actions.get(i).activityInfo;
            entries[i] = info.loadLabel(pm);
            entryValues[i] = info.name;
        }

        if(entries.length > 0) {
            mActionPreference.setEntries(entries);
            mActionPreference.setEntryValues(entryValues);
        }
    }

    private void loadExtraSettingsForActionHandler(String handlerClassName) {
        mExtraSettingsCategory.removeAll();

        Log.d(TAG, "===> load extra settings for " + handlerClassName + "under category'" + mExtraSettingsCategory + "'");


        try {
            Class<?> handler = Class.forName(handlerClassName);
            Method m = handler.getDeclaredMethod(
                "addMyPreferences",
                Class.forName("android.content.Context"),
                Class.forName("android.preference.PreferenceCategory"));
            m.invoke(handler.newInstance(), this, mExtraSettingsCategory);
        } catch(ClassNotFoundException e) {
        } catch(NoSuchMethodException e) {
        } catch(IllegalAccessException e) {
        } catch(IllegalArgumentException e) {
        } catch(InstantiationException e) {
        } catch(java.lang.reflect.InvocationTargetException e) {
        }
    }

    private String getValueOfExtraSettings(PreferenceCategory category) {
        SharedPreferences sharedPreferences =
            category.getSharedPreferences();
        String result = "";
        int numberOfPreferences =
            category.getPreferenceCount();
        for(int i = 0; i < numberOfPreferences; i++) {
            Preference preference = category.getPreference(i);

            String key = preference.getKey();
            String value = sharedPreferences.getString(key, "");
            if(TextUtils.isEmpty(key) ||
               TextUtils.isEmpty(value)) {
                continue;
            }

            result += (key + "=" + value + '\n');
        }

        return result;
    }
}
