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

        Log.d(TAG, "====> AlarmSettings.onCreate(" + bundle + ")");

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
                    inflateExtraSettings(handlerClassName, null);
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

        if(bundle != null) {
            // If this activity is destroyed and recreated due to
            // some reasons, for instance, orientation change,
            // the bundle is non-null, we should try to load
            // settings of this alarm from persisted preferences
            // (preferences can handle this by their own)

            // int savedAlarmId = bundle.getInt(Alarms.AlarmColumns._ID, -1);
            // if(alarmId == savedAlarmId) {
            Log.d(TAG, "====> Using persisted preferences for this alarm");

            // mLabelPreference.setPreferenceValue(
            //     bundle.getString(Alarms.AlarmColumns.LABEL));

            // mRepeatOnPreference.setPreferenceValue(
            //     bundle.getInt(Alarms.AlarmColumns.REPEAT_DAYS, -1));

            // The extra settings under Extra Settings category
            // were destroyed, need to inflate them again.
            String handlerClassName =
                bundle.getString(Alarms.AlarmColumns.ACTION);
            // mActionPreference.setPreferenceValue(handlerClassName);
            String extra = bundle.getString(
                getString(R.string.alarm_settings_extra_category_key));
            inflateExtraSettings(handlerClassName, extra);

            // mTimePreference.setPreferenceValue(
            //     bundle.getString(Alarms.AlarmColumns.AT_TIME_IN_MILLIS));
        } else {
            // If this activity is launched at its first time and
            // fetch settings of this alarm and show them on the
            // preferences.
            Log.d(TAG,
                  "========> Loading settings from SQL db for alarm"
                  + alarmId);
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

                        String newAction = action;
                        if(TextUtils.isEmpty(action)) {
                            // This happens when this is the
                            // first time the user uses this
                            // application and no any action
                            // string has stored in the SQL db.
                            mActionPreference.setPreferenceValueIndex(0);
                            newAction =
                                mActionPreference.getPreferenceValue();
                        } else {
                            mActionPreference.setPreferenceValue(action);
                        }
                        AlarmSettings.this.inflateExtraSettings(newAction,
                                                                extra);
                    }
                });
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {

        Log.d(TAG, "====> onPreferenceTreeClick()");

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
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
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
                    generateValueOfExtraSettings(mExtraSettingsCategory);

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

    protected void onSaveInstanceState(Bundle outState) {
        Intent intent = getIntent();
        int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);
        outState.putInt(Alarms.AlarmColumns._ID, alarmId);
        outState.putString(Alarms.AlarmColumns.LABEL,
                           mLabelPreference.getPreferenceValue());
        outState.putInt(Alarms.AlarmColumns.REPEAT_DAYS,
                        mRepeatOnPreference.getPreferenceValue());
        outState.putString(Alarms.AlarmColumns.AT_TIME_IN_MILLIS,
                           mTimePreference.getPreferenceValue());
        outState.putString(Alarms.AlarmColumns.ACTION,
                           mActionPreference.getPreferenceValue());

        outState.putString(
            getString(R.string.alarm_settings_extra_category_key),
            generateValueOfExtraSettings(mExtraSettingsCategory));
    }

    protected void onRestoreInstanceState(Bundle outState) {
        Log.d(TAG, "===========> onRestoreInstanceState()");
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "======> AlarmSettings.onDestroy()");
    }


    protected Dialog onCreateDialog(int dialogId) {
        Dialog dialog;
        switch(dialogId) {
        case LABEL_INPUT_DIALOG:
            dialog = mLabelPreference.getDialog();
            break;

        case TIME_PICK_DIALOG:
            dialog = mTimePreference.getDialog();
            break;

        case ACTION_PICK_DIALOG:
            dialog = mActionPreference.getDialog();
            break;

        default:
            throw new IllegalArgumentException("wrong dialog id");
        }

        return dialog;
    }

    protected void onPause() {
        super.onPause();

    }

    private void inflateExtraSettings(String handlerClassName,
                                      String defaultValue) {
        if(TextUtils.isEmpty(handlerClassName)) {
            return;
        }
        mExtraSettingsCategory.removeAll();
        try {
            Class<?> handler = Class.forName(handlerClassName);
            Method m = handler.getDeclaredMethod(
                "addMyPreferences",
                Class.forName("android.content.Context"),
                Class.forName("android.preference.PreferenceCategory"),
                Class.forName("java.lang.String"));
            m.invoke(handler.newInstance(), this, mExtraSettingsCategory, defaultValue);
        } catch(ClassNotFoundException e) {
            Log.d(TAG, "===> class not found - " + e);
        } catch(NoSuchMethodException e) {
            Log.d(TAG, "===> not such method - " + e);
        } catch(IllegalAccessException e) {
            Log.d(TAG, "===> illegal access - " + e);
        } catch(IllegalArgumentException e) {
            Log.d(TAG, "===> illegal argument - " + e);
        } catch(InstantiationException e) {
            Log.d(TAG, "===> instantiation  - " + e);
        } catch(java.lang.reflect.InvocationTargetException e) {
            Log.d(TAG, "===> invocation target - " + e);
        }
    }

    private String generateValueOfExtraSettings(PreferenceCategory category) {
        SharedPreferences sharedPreferences =
            category.getSharedPreferences();
        String result = "";
        int numberOfPreferences =
            category.getPreferenceCount();
        for(int i = 0; i < numberOfPreferences; i++) {
            Preference preference = category.getPreference(i);

            String key = preference.getKey();
            if(!TextUtils.isEmpty(key) &&
               sharedPreferences.contains(key)) {

                try {
                    String value = sharedPreferences.getString(key, "");
                    result += (key + "=" + value + ';');
                    continue;
                } catch(ClassCastException e) {
                }

                try {
                    boolean value = sharedPreferences.getBoolean(key, false);
                    result += (key + "=" + value + ';');
                    continue;
                } catch(ClassCastException e) {
                }

                try {
                    float value = sharedPreferences.getFloat(key, -1.0f);
                    result += (key + "=" + value + ';');
                    continue;
                } catch(ClassCastException e) {
                }

                try {
                    int value = sharedPreferences.getInt(key, -1);
                    result += (key + "=" + value + ';');
                    continue;
                } catch(ClassCastException e) {
                }

                try {
                    long value = sharedPreferences.getLong(key, -1);
                    result += (key + "=" + value + ';');
                    continue;
                } catch(ClassCastException e) {
                }
            }
        }

        Log.d(TAG, "=======> value of extra settings=" + result);

        return result;
    }
}
