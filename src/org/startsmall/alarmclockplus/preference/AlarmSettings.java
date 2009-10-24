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
import android.app.Dialog;
import android.os.Bundle;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import java.util.*;
import java.lang.reflect.Method;

public class AlarmSettings extends PreferenceActivity {
    private static final String TAG = "AlarmSettings";
    private static final int LABEL_INPUT_DIALOG = 1;
    private static final int TIME_PICK_DIALOG = 2;
    private static final int ACTION_PICK_DIALOG = 3;

    AlarmLabelPreference mLabelPreference;
    AlarmTimePreference mTimePreference;
    AlarmActionPreference mActionPreference;
    AlarmRepeatOnDialogPreference mRepeatOnPreference;
    PreferenceCategory mExtraSettingsCategory;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Log.d(TAG, "====> AlarmSettings.onCreate()");

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
        mActionPreference.setOnSelectActionListener(
            new AlarmActionPreference.OnSelectActionListener() {
                public void onSelectAction(String handlerClassName) {
                    loadExtraSettingsOfActionHandler(
                        handlerClassName);
                }
            });

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

        // Fetch settings of this alarm and show them on the preferences.
        Alarms.forEachAlarm(
            this,
            Alarms.getAlarmUri(alarmId),
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

                    mTimePreference.setPreferenceValue(
                        Integer.toString(hour * 100 + minutes));
                    mRepeatOnPreference.setPreferenceValue(repeatDays);

                    Log.d(TAG, "========> from here");
                    mActionPreference.setPreferenceValue(action);
                    AlarmSettings.this.loadExtraSettingsOfActionHandler(action);
                }
            });
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        int id = -1;
        if(preference == mLabelPreference) {
            id = LABEL_INPUT_DIALOG;
        } else if(preference == mTimePreference) {
            id = TIME_PICK_DIALOG;
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
                final int time = Integer.parseInt(mTimePreference.getPreferenceValue());
                final int hourOfDay = time / 100;
                final int minutes = time % 100;
                final int repeatOnCode = mRepeatOnPreference.getPreferenceValue();
                final String action = (String)mActionPreference.getPreferenceValue();
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
        Dialog dialog;
        switch(dialogId) {
        case LABEL_INPUT_DIALOG:
            dialog = createLabelInputDialog();
            break;

        case TIME_PICK_DIALOG:
            dialog = createTimePickDialog();
            break;

        case ACTION_PICK_DIALOG:
            dialog = createActionPickDialog();
            break;

        default:
            throw new IllegalArgumentException("wrong dialog id");
        }

        return dialog;
    }

    protected void onPause() {
        super.onPause();

    }

    private Dialog createTimePickDialog() {
        return mTimePreference.getDialog();
    }

    private Dialog createLabelInputDialog() {
        return mLabelPreference.getDialog();
    }

    private Dialog createActionPickDialog() {
        return mActionPreference.getDialog();
    }

    private void loadExtraSettingsOfActionHandler(String handlerClassName) {
        if(TextUtils.isEmpty(handlerClassName)) {
            return;
        }

        mExtraSettingsCategory.removeAll();

        Log.d(TAG, "===> load extra settings for "
              + handlerClassName + "under category'"
              + mExtraSettingsCategory + "'");

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
