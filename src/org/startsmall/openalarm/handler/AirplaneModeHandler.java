package org.startsmall.openalarm;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.preference.PreferenceCategory;
import android.preference.ListPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.util.Calendar;

public class AirplaneModeHandler extends AbsHandler {
    private static final String TAG = "AirplaneModeHandler";
    private static final String KEY_TOGGLE = "airplanemode_state";

    public void onReceive(Context context, Intent intent) {
        // Log.v(TAG, "===> AirplaneModeHandler.onReceive() start:" + Calendar.getInstance());

        final String extra = intent.getStringExtra("extra");
        putBundleIntoIntent(intent, getBundleFromExtra(extra));

        if (intent.hasExtra(KEY_TOGGLE)) {
            boolean toggle = intent.getBooleanExtra(KEY_TOGGLE, false);
            boolean currentState = isAirplaneModeOn(context);
            if (toggle != currentState) {
                setAirplaneModeEnabled(context, toggle);
            }
        }

        // Reshedule this alarm.
        Intent scheduleIntent = new Intent(intent);
        scheduleIntent.setAction(Alarm.ACTION_SCHEDULE);
        // Clear previously defined component name so that this
        // BroadcastReceiver isn't called recursively.
        scheduleIntent.setComponent(null);
        context.sendBroadcast(scheduleIntent);

        // Log.v(TAG, "===> AirplaneModeHandler.onReceive() start:" + Calendar.getInstance());
    }

    @Override
    public void addMyPreferences(final Context context,
                                 final PreferenceCategory category,
                                 final String defaultValue) {
        ListPreference onOffPref = new ListPreference(context);
        CharSequence[] entries = new CharSequence[]{"On", "Off"};
        CharSequence[] entryValues = new CharSequence[]{"true", "false"};
        onOffPref.setKey(KEY_TOGGLE);
        onOffPref.setPersistent(true);
        onOffPref.setEntries(entries);
        onOffPref.setEntryValues(entryValues);
        category.addPreference(onOffPref);

        onOffPref.setValueIndex(1);
        onOffPref.setTitle(context.getString(R.string.airplanemode_title));
        onOffPref.setDialogTitle(context.getString(R.string.airplanemode_dialog_title));
        onOffPref.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference pref,
                                                  Object newValue) {
                    ListPreference preference = (ListPreference)pref;
                    preference.setValue((String)newValue);
                    preference.setSummary(preference.getEntry());
                    return true;
                }
            });

        if (TextUtils.isEmpty(defaultValue)) {
            onOffPref.setValueIndex(1); // set to false
        } else {
            Bundle result = getBundleFromExtra(defaultValue);
            boolean state = result.getBoolean(KEY_TOGGLE, false);
            onOffPref.setValueIndex(state ? 0 : 1);
        }
        onOffPref.setSummary(onOffPref.getEntry());
    }

    private void setAirplaneModeEnabled(Context context, boolean toggle) {
        Settings.System.putInt(context.getContentResolver(),
                               Settings.System.AIRPLANE_MODE_ON,
                               toggle ? 1 : 0);

        // @note This intent was claimed protected that should be
        // sent only from system. But, this is the only way to
        // toggle airplane mode. Set
        // Settings.System.AIRPLANE_MODE_ON in settings has no
        // actual effect.
        Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        i.putExtra("state", toggle);
        context.sendBroadcast(i);

        // No need to put an notification on the status bar. The
        // system already has one for airplane mode.

        // String label = i.getStringExtra("label");
        // String state = toggle ? "on" : "off";
        // Notificator notificator = new Notificator(context);
        // if (toggle) {
        //     notificator.set(0,
        //                     R.drawable.ic_lock_airplane_mode,
        //                     context.getString(
        //                         R.string.airplanemode_notification_ticker,
        //                         state.toUpperCase()),
        //                     context.getString(
        //                         R.string.airplanemode_notification_content,
        //                         label, state));
        // }
    }

    private boolean isAirplaneModeOn(Context context) {
        try {
            int state = Settings.System.getInt(context.getContentResolver(),
                                               Settings.System.AIRPLANE_MODE_ON);
            return state == 1;
        } catch (Settings.SettingNotFoundException e) {
        }
        return false;
    }

    @Override
    protected void putBundleIntoIntent(Intent intent, Bundle bundle) {
        final Boolean toggle = bundle.getBoolean(KEY_TOGGLE, false);
        intent.putExtra(KEY_TOGGLE, toggle);
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
                if (elems[0].equals(KEY_TOGGLE)) {
                    boolean toggle = false;
                    if (elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        toggle = Boolean.parseBoolean(elems[1]);
                    }
                    result.putBoolean(KEY_TOGGLE, toggle);
                }
            }
        }
        return result;
    }

}
