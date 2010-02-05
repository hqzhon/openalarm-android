package org.startsmall.openalarm;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.preference.PreferenceCategory;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.Log;
import java.util.Calendar;

public class ApnHandler extends AbsHandler {
    private static final String TAG = "ApnHandler";
    private static final String KEY_TOGGLE = "apn_toggle";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "===> ApnHandler.onReceive() start: " + Calendar.getInstance());

        // Parse extra settings out of combined value.
        final int alarmId = intent.getIntExtra("_id", -1);
        final String label = intent.getStringExtra("label");
        final String extra = intent.getStringExtra("extra");
        putBundleIntoIntent(intent, getBundleFromExtra(extra));

        // Start ApnService to handle time-consuming APN content
        // manipulation.
        boolean toggle = intent.getBooleanExtra(KEY_TOGGLE, false);
        Intent serviceIntent = new Intent();
        serviceIntent.putExtra("label", label);
        serviceIntent.putExtra(KEY_TOGGLE, toggle);
        serviceIntent.setClassName("org.startsmall.openalarm",
                                   "org.startsmall.openalarm.ApnService");
        context.startService(serviceIntent);

        // Reshedule this alarm.
        Intent scheduleIntent = new Intent(intent);
        scheduleIntent.setAction(Alarm.ACTION_SCHEDULE);
        // Clear explicitly defined component from this Intent to
        // prevent this receiver to be called recursively.
        scheduleIntent.setComponent(null);
        context.sendBroadcast(scheduleIntent);

        Log.v(TAG, "===> ApnHandler.onReceive() end: " + Calendar.getInstance());
    }

    public void addMyPreferences(final Context context,
                                 final PreferenceCategory category,
                                 final String defaultValue) {
        ListPreference onOffPref = new ListPreference(context);
        CharSequence[] entries =
            new CharSequence[]{context.getString(R.string.on),
                               context.getString(R.string.off)};
        CharSequence[] entryValues = new CharSequence[]{"true", "false"};
        onOffPref.setKey(KEY_TOGGLE);
        onOffPref.setPersistent(true);
        onOffPref.setKey(KEY_TOGGLE);
        onOffPref.setPersistent(true);
        onOffPref.setEntries(entries);
        onOffPref.setEntryValues(entryValues);
        category.addPreference(onOffPref);

        onOffPref.setValueIndex(1);
        onOffPref.setTitle(context.getString(R.string.toggle_title));
        onOffPref.setDialogTitle(context.getString(R.string.apn_toggle_dialog_title));
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
            onOffPref.setValueIndex(1); // set to false.
        } else {
            Bundle result = getBundleFromExtra(defaultValue);
            boolean state = result.getBoolean(KEY_TOGGLE, false);
            onOffPref.setValueIndex(state ? 0 : 1);
        }
        onOffPref.setSummary(onOffPref.getEntry());
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
