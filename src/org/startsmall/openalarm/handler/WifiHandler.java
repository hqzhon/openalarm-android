package org.startsmall.openalarm;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.preference.PreferenceCategory;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class WifiHandler extends AbsHandler {
    private static final String TAG = "WifiHandler";
    private static final String KEY_TOGGLE = "wifi_state";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String extra = intent.getStringExtra("extra");
        putBundleIntoIntent(intent, getBundleFromExtra(extra));

        if (intent.hasExtra(KEY_TOGGLE)) {
            Boolean toggle = intent.getBooleanExtra(KEY_TOGGLE, false);
            setWifiEnabled(context, toggle);
        }

        Intent scheduleIntent = new Intent(intent);
        scheduleIntent.setAction(Alarm.ACTION_SCHEDULE);
        // Clear explicitly defined component from this Intent to
        // prevent this receiver to be called recursively.
        scheduleIntent.setComponent(null);
        context.sendBroadcast(scheduleIntent);
    }

    /**
     * Add preferences of this handler into OpenAlarm's Extra
     * Settings catergory.
     *
     * @param mainAppContext Context of OpenAlarm.
     * @param category Extra Settings category object reference.
     * @param defaultValue Values of these preferences in string format.
     */
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
        onOffPref.setTitle(context.getString(R.string.wifi_toggle_title));
        onOffPref.setDialogTitle(context.getString(R.string.wifi_toggle_dialog_title));
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

        if (!TextUtils.isEmpty(defaultValue)) {
            Bundle result = getBundleFromExtra(defaultValue);
            boolean state = result.getBoolean(KEY_TOGGLE, false);
            onOffPref.setValueIndex(state ? 0 : 1);
        }
        onOffPref.setSummary(onOffPref.getEntry());
    }

    private void setWifiEnabled(Context context, boolean toggle) {
        WifiManager wm =
            (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        if (toggle != wm.isWifiEnabled()) {
            if (!wm.setWifiEnabled(toggle)) {
                return;
            }
        }
    }

    protected void putBundleIntoIntent(Intent intent, Bundle bundle) {
        final Boolean toggle = bundle.getBoolean(KEY_TOGGLE, false);
        intent.putExtra(KEY_TOGGLE, toggle);
    }

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
