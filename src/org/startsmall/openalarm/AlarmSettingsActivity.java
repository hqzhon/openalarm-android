package org.startsmall.openalarm;

import android.app.Dialog;
import android.os.Bundle;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

public class AlarmSettingsActivity extends PreferenceActivity {
    private static final String TAG = "AlarmSettingsActivity";
    private int mAlarmId;

    // Dialog IDs
    private static final int DIALOG_ID_ENTER_LABEL = 1;
    private static final int DIALOG_ID_PICKUP_TIME = 2;
    private static final int DIALOG_ID_PICKUP_HANDLER = 3;
    private static final int DIALOG_ID_PICKUP_REPEAT_DAYS = 4;

    private AlarmLabelPreference mLabelPreference;
    private AlarmTimePreference mTimePreference;
    private AlarmActionPreference mActionPreference;
    private AlarmRepeatOnPreference mRepeatOnPreference;
    private PreferenceCategory mExtraSettingsCategory;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Intent i = getIntent();
        mAlarmId = i.getIntExtra(AlarmColumns._ID, -1);

        addPreferencesFromResource(R.xml.alarm_settings_activity);

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
            new AlarmActionPreference.IOnSelectActionListener() {
                public void onSelectAction(String handlerClassName) {
                    loadExtraPreferences(handlerClassName, null);
                }
            });
        mRepeatOnPreference =
            (AlarmRepeatOnPreference)preferenceManager.findPreference(
                getString(R.string.alarm_settings_repeat_days_key));
        mExtraSettingsCategory =
            (PreferenceCategory)preferenceManager.findPreference(
                getString(R.string.alarm_settings_extra_category_key));
        setVolumeControlStream(AudioManager.STREAM_ALARM);
    }

    /**
     * Pop up appropriate dialog of the clicked Preference.
     *
     * @param preferenceScreen a PreferenceScreen object.
     * @param preference Preference object that was clicked.
     *
     * @return true/false. True if click event is handled.
     */
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        int id = -1;
        if(preference == mLabelPreference) {
            id = DIALOG_ID_ENTER_LABEL;
        } else if(preference == mTimePreference) {
            id = DIALOG_ID_PICKUP_TIME;
        } else if(preference == mActionPreference) {
            id = DIALOG_ID_PICKUP_HANDLER;
        } else if(preference == mRepeatOnPreference) {
            id = DIALOG_ID_PICKUP_REPEAT_DAYS;
        }

        if(id != -1) {
            showDialog(id);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    // Save the current state of this activity into Bundle. It
    // will be restored in onCreate() with saved state passed in.
    // protected void onSaveInstanceState(Bundle outState) {
    //     super.onSaveInstanceState(outState);
    // }

    // protected void onRestoreInstanceState(Bundle outState) {
    //     Log.d(TAG, "===========> onRestoreInstanceState()");
    //     super.onRestoreInstanceState(outState);
    // }

    /**
     * All dialogs are managed by this activity. Things on the
     * screen must be snapshot into a Bundle through their
     * onSaveInstanceState() and restored through
     * onRestoreInstanceState(). \sa MyPreference.
     *
     */
    @Override
    protected Dialog onCreateDialog(int dialogId) {
        Dialog dialog;
        switch(dialogId) {
        case DIALOG_ID_ENTER_LABEL:
            dialog = mLabelPreference.getDialog();
            break;

        case DIALOG_ID_PICKUP_TIME:
            dialog = mTimePreference.getDialog();
            break;

        case DIALOG_ID_PICKUP_HANDLER:
            dialog = mActionPreference.getDialog();
            break;

        case DIALOG_ID_PICKUP_REPEAT_DAYS:
            dialog = mRepeatOnPreference.getDialog();
            break;

        default:
            throw new IllegalArgumentException("wrong dialog id");
        }
        return dialog;
    }

    /**
     * Commit alarm settings into backing SQL database and
     * re-calculate its time if it is needed.
     *
     */
    @Override
    protected void onPause() {
        super.onPause();

        Intent i = getIntent();
        final String newLabel = (String)mLabelPreference.getPreferenceValue();
        final int newTime = (Integer)mTimePreference.getPreferenceValue();
        final int newHourOfDay = newTime / 100;
        final int newMinutes = newTime % 100;
        final int newRepeatDays = (Integer)mRepeatOnPreference.getPreferenceValue();
        final String newHandler = (String)mActionPreference.getPreferenceValue();

        final String newExtra = getValueOfExtraPreferences(mExtraSettingsCategory);

        // Get alarm from cache.
        Alarm alarm = Alarm.getInstance(this, mAlarmId);

        // Update new values of the alarm.
        boolean enabled = alarm.getBooleanField(Alarm.FIELD_ENABLED);
        if (alarm.update(this,
                         enabled,
                         newLabel, newHourOfDay, newMinutes, newRepeatDays,
                         newHandler, newExtra)) {
            // Alarm has been rescheduled,
            Toast.makeText(
                this,
                getString(R.string.alarm_set_notification_content,
                          alarm.formatSchedule(this)),
                Toast.LENGTH_LONG).show();
        };
    }

    /**
     * Resume this activity and populate alarm settings from
     * saved SQL rows.
     *
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Fetch alarm settings from persistent content
        Alarm alarm = Alarm.getInstance(this, mAlarmId);

        // Populate alarm settings into Preferences.
        String label = alarm.getStringField(Alarm.FIELD_LABEL);
        setTitle(getString(R.string.alarm_basic_settings_title, label));
        mLabelPreference.setPreferenceValue(label);
        mActionPreference.setPreferenceValue(alarm.getStringField(Alarm.FIELD_HANDLER));
        mTimePreference.setPreferenceValue(
            alarm.getIntField(Alarm.FIELD_HOUR_OF_DAY) * 100 +
            alarm.getIntField(Alarm.FIELD_MINUTES));
        mRepeatOnPreference.setPreferenceValue(alarm.getIntField(Alarm.FIELD_REPEAT_DAYS));

        // Get the value of extra preferences to see if they have
        // local modifications that weren't persited.
        String valueOfExtraSettings = getValueOfExtraPreferences(mExtraSettingsCategory);
        if (TextUtils.isEmpty(valueOfExtraSettings)) {
            loadExtraPreferences(alarm.getStringField(Alarm.FIELD_HANDLER),
                                 alarm.getStringField(Alarm.FIELD_EXTRA));
        } else {
            loadExtraPreferences(alarm.getStringField(Alarm.FIELD_HANDLER),
                                 valueOfExtraSettings);
        }
    }

    /**
     * Load preferences of an alarm handler. Use Class.forName()
     * and reflective methods to call addMyPreferences() of an
     * alarm handler which installs its preferences under Extra
     * Settings category.
     *
     */
    private void loadExtraPreferences(String handlerClassName, String extraValue) {
        if(TextUtils.isEmpty(handlerClassName)) {
            return;
        }
        mExtraSettingsCategory.removeAll();

        try {
            Class<?> handler = getHandlerClass(handlerClassName);
            Method m = handler.getDeclaredMethod(
                "addMyPreferences",
                Class.forName("android.content.Context"),
                Class.forName("android.preference.PreferenceCategory"),
                Class.forName("java.lang.String"));
            m.invoke(handler.newInstance(), this, mExtraSettingsCategory, extraValue);
        } catch (Exception e) {
            Log.d(TAG, "ERROR: Unable to load preferences of this alarm handler - " + handlerClassName);
        }
    }

    private String getValueOfExtraPreferences(PreferenceCategory category) {
        SharedPreferences sharedPreferences = category.getSharedPreferences();
        StringBuilder sb = new StringBuilder();
        final int numberOfPreferences = category.getPreferenceCount();
        for (int i = 0; i < numberOfPreferences; i++) {
            Preference preference = category.getPreference(i);

            // Preference must be persisted in order for me to
            // use unified SharedPreference to get preference
            // value.
            if (preference.hasKey() && preference.isPersistent()) {
                String key = preference.getKey();

                try {
                    String value = sharedPreferences.getString(key, null);
                    sb.append(key + "=" + value + AbsHandler.SEPARATOR);
                    continue;
                } catch (ClassCastException e) {
                }

                try {
                    boolean value = sharedPreferences.getBoolean(key, false);
                    sb.append(key + "=" + value + AbsHandler.SEPARATOR);
                    continue;
                } catch (ClassCastException e) {
                }

                try {
                    float value = sharedPreferences.getFloat(key, -1.0f);
                    sb.append(key + "=" + value + AbsHandler.SEPARATOR);
                    continue;
                } catch (ClassCastException e) {
                }

                try {
                    int value = sharedPreferences.getInt(key, -1);
                    sb.append(key + "=" + value + AbsHandler.SEPARATOR);
                    continue;
                } catch (ClassCastException e) {
                }

                try {
                    long value = sharedPreferences.getLong(key, -1);
                    sb.append(key + "=" + value + AbsHandler.SEPARATOR);
                    continue;
                } catch (ClassCastException e) {
                }
            }
        }

        return sb.toString();
    }

    /**
     * Return the class object of an alarm handler.
     *
     */
    private Class<?> getHandlerClass(String handlerClassName)
        throws ClassNotFoundException, PackageManager.NameNotFoundException {
        // ApplicationInfo appInfo = getPackageManager().getApplicationInfo("org.startsmall.openalarm", 0);
        ApplicationInfo appInfo = getApplicationInfo();
        dalvik.system.PathClassLoader classLoader =
            new dalvik.system.PathClassLoader(appInfo.sourceDir, ClassLoader.getSystemClassLoader());

        return Class.forName(handlerClassName, true, classLoader);
    }
}
