package org.startsmall.openalarm;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.preference.Preference;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.preference.ListPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

public class ToggleHandler extends AbsHandler {
    private static final String TAG = "ToggleHandler";
    private static final String KEY_OPERATION = "operation";
    public  static final String KEY_ONOFF = "onoff";

    private static final int OPERATION_AIRPLANE_MODE = 0;
    private static final int OPERATION_APN = 1;
    private static final int OPERATION_WIFI = 2;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "===> onReceive() ");

        // Parse extra settings out of combined value.
        final String label = intent.getStringExtra("label");
        final String extra = intent.getStringExtra("extra");
        putBundleIntoIntent(intent, getBundleFromExtra(extra));

        // Get operation to perform
        int operation = intent.getIntExtra(KEY_OPERATION, -1);
        boolean onOff = intent.getBooleanExtra(KEY_ONOFF, false);
        switch (operation) {
        case OPERATION_WIFI:
            toggleWifi(context, onOff);
            break;

        case OPERATION_AIRPLANE_MODE:
            toggleAirplaneMode(context, onOff);
            break;

        case OPERATION_APN:
            Intent serviceIntent = new Intent();
            serviceIntent.putExtra(KEY_ONOFF, onOff);
            serviceIntent.setClassName("org.startsmall.openalarm",
                                       "org.startsmall.openalarm.ApnService");
            context.startService(serviceIntent);
            break;
        }

        // Reshedule this alarm.
        final int alarmId = intent.getIntExtra(AlarmColumns._ID, -1);
        Alarms.dismissAlarm(context, alarmId);
    }

    @Override
    public void addMyPreferences(final Context context,
                                 final PreferenceCategory category,
                                 final String defaultValue) {
        Preference.OnPreferenceChangeListener prefChangeListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference pref,
                                                  Object newValue) {
                    ListPreference preference = (ListPreference)pref;
                    preference.setValue((String)newValue);
                    preference.setSummary(preference.getEntry());
                    return true;
                }
            };

        ListPreference operationPref = new ListPreference(context);
        operationPref.setKey(KEY_OPERATION);
        operationPref.setPersistent(true);
        operationPref.setEntries(
            new CharSequence[]{
                context.getString(R.string.toggle_handler_operation_airplane_mode),
                context.getString(R.string.toggle_handler_operation_apn),
                context.getString(R.string.toggle_handler_operation_wifi)});
        operationPref.setEntryValues(
            new CharSequence[]{String.valueOf(OPERATION_AIRPLANE_MODE),
                               String.valueOf(OPERATION_APN),
                               String.valueOf(OPERATION_WIFI)});
        category.addPreference(operationPref);

        operationPref.setValueIndex(OPERATION_AIRPLANE_MODE);
        operationPref.setTitle(R.string.toggle_handler_operation_title);
        operationPref.setDialogTitle(R.string.toggle_handler_operation_title);
        operationPref.setOnPreferenceChangeListener(prefChangeListener);

        ListPreference onOffPref = new ListPreference(context);
        onOffPref.setTitle(R.string.toggle_handler_activate_title);
        onOffPref.setDialogTitle(R.string.toggle_handler_activate_title);
        onOffPref.setKey(KEY_ONOFF);
        onOffPref.setPersistent(true);
        onOffPref.setEntries(new CharSequence[]{context.getString(R.string.on),
                                                context.getString(R.string.off)});
        onOffPref.setEntryValues(new CharSequence[]{"true", "false"});
        category.addPreference(onOffPref);
        onOffPref.setOnPreferenceChangeListener(prefChangeListener);
        if (TextUtils.isEmpty(defaultValue)) {
            operationPref.setValueIndex(0);
            onOffPref.setValueIndex(1);
        } else {
            Bundle result = getBundleFromExtra(defaultValue);
            int operation = result.getInt(KEY_OPERATION, -1);
            operationPref.setValueIndex(operation);

            boolean onOff = result.getBoolean(KEY_ONOFF, false);
            onOffPref.setValueIndex(onOff ? 0 : 1);
        }

        operationPref.setSummary(operationPref.getEntry());
        onOffPref.setSummary(onOffPref.getEntry());
    }

    @Override
    protected void putBundleIntoIntent(Intent intent, Bundle bundle) {
        final int operation = bundle.getInt(KEY_OPERATION, -1);
        intent.putExtra(KEY_OPERATION, operation);

        final boolean onOff = bundle.getBoolean(KEY_ONOFF, false);
        intent.putExtra(KEY_ONOFF, onOff);
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
                if (elems[0].equals(KEY_OPERATION)) {
                    if (elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        int operation = Integer.parseInt(elems[1]);
                        result.putInt(KEY_OPERATION, operation);
                    }
                } else if (elems[0].equals(KEY_ONOFF)) {
                    boolean onOff = false;
                    if (elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        onOff = Boolean.parseBoolean(elems[1]);
                    }
                    result.putBoolean(KEY_ONOFF, onOff);
                }
            }
        }
        return result;
    }

    private void toggleAirplaneMode(Context context, boolean onOff) {
        boolean enabled = isAirplaneModeOn(context);
        if (onOff != enabled) {
            setAirplaneModeEnabled(context, onOff);
        }
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

    private void toggleWifi(Context context, boolean onOff) {
        WifiManager wm =
            (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        boolean enabled = wm.isWifiEnabled();
        if (enabled != onOff) {
            wm.setWifiEnabled(onOff);
        }
    }
}
