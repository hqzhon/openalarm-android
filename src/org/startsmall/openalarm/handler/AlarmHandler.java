package org.startsmall.openalarm;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.text.method.DigitsKeyListener;
import android.text.TextUtils;
import android.util.Log;

// import java.util.Calendar;

public class AlarmHandler extends AbsHandler {
    private static final String TAG = "AlarmHandler";

    static final String EXTRA_KEY_VIBRATE = "vibrate";
    static final String EXTRA_KEY_RINGTONE = "ringtone";
    static final String EXTRA_KEY_SNOOZE_DURATION = "snooze_duration";
    static final String EXTRA_KEY_MATH_LOCK_ON = "math_lock_on";

    private static final int DEFAULT_SNOOZE_DURATION = 2; // 2 minutes

    public void onReceive(Context context, Intent intent) {
        // Log.i(TAG, "===> onReceive(): " + Calendar.getInstance());

        // Parse extra data in the Intent and put them into Intent.
        final String extra = intent.getStringExtra(AlarmColumns.EXTRA);
        putBundleIntoIntent(intent, getBundleFromExtra(extra));

        // Start an activity to play ringtone.
        intent.setClass(context, FireAlarm.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        context.startActivity(intent);
    }

    /**
     * Add preferences of this handler into Extra Settings.
     *
     * @param context
     * @param category
     * @param extra
     */
    @Override
    public void addMyPreferences(final Context context,
                                 final PreferenceCategory category,
                                 final String extra) {
        // Vibrate
        CheckBoxPreference vibratePref = new CheckBoxPreference(context);
        vibratePref.setKey(EXTRA_KEY_VIBRATE);
        vibratePref.setPersistent(true);
        vibratePref.setTitle(R.string.alarm_handler_vibrate_title);
        vibratePref.setSummaryOn(R.string.on);
        vibratePref.setSummaryOff(R.string.off);
        category.addPreference(vibratePref);

        // Ringtone;
        RingtonePreference ringtonePref = new RingtonePreference(context);
        ringtonePref.setShowDefault(false);
        ringtonePref.setShowSilent(false);
        ringtonePref.setTitle(R.string.ringtone_title);
        ringtonePref.setKey(EXTRA_KEY_RINGTONE);
        ringtonePref.setPersistent(true);
        ringtonePref.setRingtoneType(RingtoneManager.TYPE_ALL);
        category.addPreference(ringtonePref);

        // Snooze duration
        EditTextPreference snoozeDurationPref = new EditTextPreference(context);
        snoozeDurationPref.setPersistent(true);
        snoozeDurationPref.setTitle(R.string.alarm_handler_snooze_duration_title);
        snoozeDurationPref.setKey(EXTRA_KEY_SNOOZE_DURATION);
        snoozeDurationPref.setDefaultValue("2");
        snoozeDurationPref.getEditText().setKeyListener(
            DigitsKeyListener.getInstance(false, false));
        snoozeDurationPref.setDialogTitle(R.string.alarm_handler_snooze_duration_dialog_title);
        snoozeDurationPref.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference p, Object newValue) {
                    ((EditTextPreference)p).setSummary(
                        Integer.parseInt((String)newValue) + " minutes");
                    return true;
                }
            });
        category.addPreference(snoozeDurationPref);

        // Math mode: Do a math in order to snooze or dimiss alarm
        CheckBoxPreference mathModePref = new CheckBoxPreference(context);
        mathModePref.setKey(EXTRA_KEY_MATH_LOCK_ON);
        mathModePref.setPersistent(true);
        mathModePref.setTitle(R.string.alarm_handler_math_lock_title);
        mathModePref.setSummaryOn(R.string.on);
        mathModePref.setSummaryOff(R.string.off);
        category.addPreference(mathModePref);

        if (TextUtils.isEmpty(extra)) {
            vibratePref.setChecked(false);
            ringtonePref.setRingtoneUri(null);
            ringtonePref.setSummary("");
            snoozeDurationPref.setText("");
            snoozeDurationPref.setSummary("");
            mathModePref.setChecked(false);
        } else {
            Bundle result = getBundleFromExtra(extra);

            boolean vibrate = result.getBoolean(EXTRA_KEY_VIBRATE, false);
            vibratePref.setChecked(vibrate);

            String rtString = result.getString(EXTRA_KEY_RINGTONE);
            if(!TextUtils.isEmpty(rtString)) {
                Uri rtUri = Uri.parse(rtString);
                ringtonePref.setRingtoneUri(rtUri);
            }

            int snoozeDuration = result.getInt(EXTRA_KEY_SNOOZE_DURATION, -1);
            if (snoozeDuration != -1) {
                snoozeDurationPref.setSummary(
                    Integer.toString(snoozeDuration) + " minutes");
                snoozeDurationPref.setText(String.valueOf(snoozeDuration));
            }

            boolean isMathModeOn = result.getBoolean(EXTRA_KEY_MATH_LOCK_ON, false);
            mathModePref.setChecked(isMathModeOn);
        }
    }

    @Override
    protected void putBundleIntoIntent(Intent intent, Bundle bundle) {
        final boolean vibrate = bundle.getBoolean(EXTRA_KEY_VIBRATE, false);
        intent.putExtra(EXTRA_KEY_VIBRATE, vibrate);

        final String uriString = bundle.getString(EXTRA_KEY_RINGTONE);
        if (!TextUtils.isEmpty(uriString)) {
            intent.putExtra(EXTRA_KEY_RINGTONE, uriString);
        }

        final int ringtoneDuration = bundle.getInt(EXTRA_KEY_SNOOZE_DURATION, DEFAULT_SNOOZE_DURATION);
        intent.putExtra(EXTRA_KEY_SNOOZE_DURATION, ringtoneDuration);

        final boolean isMathModeOn = bundle.getBoolean(EXTRA_KEY_MATH_LOCK_ON, false);
        intent.putExtra(EXTRA_KEY_MATH_LOCK_ON, isMathModeOn);
    }

    @Override
    protected Bundle getBundleFromExtra(String extra) {
        Bundle result = new Bundle();
        if (!TextUtils.isEmpty(extra)) {
            String[] values = TextUtils.split(extra, SEPARATOR);
            for (String value : values) {
                if (TextUtils.isEmpty(value) ||
                    !value.matches("(\\w+)=.*")) {
                    continue;
                }

                String[] elems = value.split("=");
                if (elems[0].equals(EXTRA_KEY_VIBRATE)) {
                    boolean vibrate = false;
                    if (elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        vibrate = Boolean.parseBoolean(elems[1]);
                    }
                    result.putBoolean(EXTRA_KEY_VIBRATE, vibrate);
                } else if (elems[0].equals(EXTRA_KEY_RINGTONE)) {
                    if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        result.putString(EXTRA_KEY_RINGTONE, elems[1]);
                    }
                } else if (elems[0].equals(EXTRA_KEY_SNOOZE_DURATION)) {
                    if (elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        result.putInt(EXTRA_KEY_SNOOZE_DURATION,
                                      Integer.parseInt(elems[1]));
                    }
                } else if (elems[0].equals(EXTRA_KEY_MATH_LOCK_ON)) {
                    if (elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        result.putBoolean(EXTRA_KEY_MATH_LOCK_ON,
                                          Boolean.parseBoolean(elems[1]));
                    }
                }
            }
        }
        return result;
    }
}
