/**
 * @file   AlarmHandler.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Thu Oct 29 11:22:32 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.openalarm;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Log;

public class AlarmHandler extends AbsActionHandler {

    interface IRingtoneChangedListener {
        public void onRingtoneChanged(Uri uri);
    }

    private class MyRingtonePreference extends RingtonePreference {
        IRingtoneChangedListener mRingtoneChangedListener;

        public MyRingtonePreference(Context context) {
            super(context);

            setShowDefault(true);
            setShowSilent(true);
        }

        @SuppressWarnings("unused")
		public void setRingtoneChangedListener(IRingtoneChangedListener listener) {
            mRingtoneChangedListener = listener;
        }

        public Uri getRingtoneUri() {
            return Uri.parse(getPersistedString(""));
        }

        public void setRingtoneUri(Uri ringtoneUri) {
            persistString(ringtoneUri.toString());
            Ringtone ringtone =
                RingtoneManager.getRingtone(getContext(), ringtoneUri);
            setSummary(ringtone.getTitle(getContext()));
        }

        protected void onSaveRingtone(Uri ringtoneUri) {
            setRingtoneUri(ringtoneUri);
            if(mRingtoneChangedListener != null) {
                mRingtoneChangedListener.onRingtoneChanged(ringtoneUri);
            }
        }

        protected Uri onRestoreRingtone() {
            return getRingtoneUri();
        }
    }

    private static final String TAG = "AlarmHandler";
    private static final String VIBRATE_KEY = "vibrate";
    private static final String RINGTONE_KEY = "ringtone";
    private static final String SNOOZE_DURATION_KEY = "snooze_duration";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "===> AlarmHandler.onReceive()");


        //final int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);
        // Parse extra settings out of combined value.
        final String extra =
            intent.getStringExtra(Alarms.AlarmColumns.EXTRA);
        if(!TextUtils.isEmpty(extra)) {
            Bundle values = parsePreferenceValuesFromExtra(extra);

            // Vibrate or not?
            Boolean vibrate = values.getBoolean(VIBRATE_KEY, false);
            intent.putExtra(VIBRATE_KEY, vibrate);

            // Get ringtone URI.
            String rtString = values.getString(RINGTONE_KEY);
            if(rtString != null) {
                intent.putExtra(RINGTONE_KEY, rtString);
            }

            int snoozeDuration = values.getInt(SNOOZE_DURATION_KEY, -1);
            if (snoozeDuration != -1) {
                intent.putExtra(SNOOZE_DURATION_KEY, snoozeDuration);
            }
        }

        // Start a parent activity with a brand new activity stack.
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
        vibratePref.setKey(VIBRATE_KEY);
        vibratePref.setPersistent(true);
        vibratePref.setTitle(R.string.alarm_handler_vibrate_title);
        vibratePref.setSummaryOn(R.string.alarm_handler_vibrate_summary_on);
        vibratePref.setSummaryOff(R.string.alarm_handler_vibrate_summary_off);
        category.addPreference(vibratePref);

        // Ringtone;
        MyRingtonePreference ringtonePref = new MyRingtonePreference(context);
        ringtonePref.setShowDefault(false);
        ringtonePref.setShowSilent(false);
        ringtonePref.setTitle(R.string.alarm_handler_ringtone_title);
        ringtonePref.setKey(RINGTONE_KEY);
        ringtonePref.setPersistent(true);
        ringtonePref.setRingtoneType(RingtoneManager.TYPE_ALL);
        category.addPreference(ringtonePref);

        // Snooze duration
        EditTextPreference snoozeDurationPref = new EditTextPreference(context);
        snoozeDurationPref.setPersistent(true);
        snoozeDurationPref.setTitle(R.string.alarm_handler_snooze_duration_title);
        snoozeDurationPref.setKey(SNOOZE_DURATION_KEY);
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

        if(!TextUtils.isEmpty(extra)) {
            Bundle result = parsePreferenceValuesFromExtra(extra);

            boolean vibrate = result.getBoolean(VIBRATE_KEY, false);
            vibratePref.setChecked(vibrate);

            String rtString = result.getString(RINGTONE_KEY);
            if(rtString != null) {
                Uri rtUri = Uri.parse(rtString);
                ringtonePref.setRingtoneUri(rtUri);
                Ringtone ringtone =
                    RingtoneManager.getRingtone(context, rtUri);
                ringtonePref.setSummary(ringtone.getTitle(context));
            }

            int snoozeDuration = result.getInt(SNOOZE_DURATION_KEY, -1);
            if (snoozeDuration != -1) {
                snoozeDurationPref.setSummary(
                    Integer.toString(snoozeDuration) + " minutes");
            }
        }
    }

    // Generate patten: key=value
    private Bundle parsePreferenceValuesFromExtra(String extra) {
        Bundle result = new Bundle();
        String[] values = TextUtils.split(extra, ";");
        for(String value : values) {
            if(TextUtils.isEmpty(value) ||
               !value.matches("(\\w+)=.*")) {
                continue;
            }

            String[] elems = value.split("=");
            if(elems[0].equals(VIBRATE_KEY)) {
                boolean vibrate = false;
                if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                    vibrate = Boolean.parseBoolean(elems[1]);
                }
                result.putBoolean(VIBRATE_KEY, vibrate);
            } else if(elems[0].equals(RINGTONE_KEY)) {
                if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                    result.putString(RINGTONE_KEY, elems[1]);
                }
            } else if (elems[0].equals(SNOOZE_DURATION_KEY)) {
                if (elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                    result.putInt(SNOOZE_DURATION_KEY,
                                  Integer.parseInt(elems[1]));
                }
            }
        }
        return result;
    }
}
