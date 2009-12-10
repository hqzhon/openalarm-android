/**
 * @file   AlarmActionHandler.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Thu Oct 29 11:22:32 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.openalarm.receiver;

import org.startsmall.openalarm.Alarms;
import org.startsmall.openalarm.R;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;
import android.util.AttributeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlarmActionHandler extends AbsActionHandler {

    interface IRingtoneChangedListener {
        public void onRingtoneChanged(Uri uri);
    }

    private class MyRingtonePreference extends RingtonePreference {
        IRingtoneChangedListener mRingtoneChangedListener;

        public MyRingtonePreference(Context context, AttributeSet attrs) {
            super(context, attrs);

            setShowDefault(true);
            setShowSilent(true);
        }

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

    private static final String TAG = "AlarmActionHandler";
    private static final String VIBRATE_KEY = "vibrate";
    private static final String RINGTONE_KEY = "ringtone";

    @Override
    public void onReceive(Context context, Intent intent) {
        final int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);
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
        }

        // Start a parent activity with a brand new activity stack.
        intent.setClass(context, FireAlarm.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Intent.FLAG_ACTIVITY_NO_USER_ACTION);
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
        vibratePref.setTitle(R.string.alarm_extra_settings_vibrate_title);
        category.addPreference(vibratePref);

        // Ringtone;
        MyRingtonePreference ringtonePref = new MyRingtonePreference(context,
            null);
        ringtonePref.setRingtoneType(RingtoneManager.TYPE_ALARM);
        ringtonePref.setShowDefault(false);
        ringtonePref.setShowSilent(false);
        ringtonePref.setTitle("Set Ringtone");
        ringtonePref.setKey(RINGTONE_KEY);
        ringtonePref.setPersistent(true);
        category.addPreference(ringtonePref);

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
            }
        }
        return result;
    }
}
