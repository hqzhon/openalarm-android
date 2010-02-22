package org.startsmall.openalarm;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.preference.Preference;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.preference.PreferenceCategory;
import android.preference.ListPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
// import java.util.Calendar;

public class ToggleHandler extends AbsHandler {
    private static final String TAG = "ToggleHandler";
    private static final String KEY_OPERATION = "operation";

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
        switch (operation) {
        case OPERATION_WIFI:
            toggleWifi(context);
            break;

        case OPERATION_AIRPLANE_MODE:
            toggleAirplaneMode(context);
            break;

        case OPERATION_APN:
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName("org.startsmall.openalarm",
                                       "org.startsmall.openalarm.ApnService");
            context.startService(serviceIntent);
            break;
        }

        // Reshedule this alarm.
        final int alarmId = intent.getIntExtra(AlarmColumns._ID, -1);
        Alarms.dismissAlarm(context, alarmId);

        Log.v(TAG, "===>.onReceive() end");
    }

    public void addMyPreferences(final Context context,
                                 final PreferenceCategory category,
                                 final String defaultValue) {
        ListPreference operationPref = new ListPreference(context);
        CharSequence[] entries =
            new CharSequence[]{
            context.getString(R.string.toggle_handler_operation_airplane_mode),
            context.getString(R.string.toggle_handler_operation_apn),
            context.getString(R.string.toggle_handler_operation_wifi)};
        CharSequence[] entryValues =
            new CharSequence[]{String.valueOf(OPERATION_AIRPLANE_MODE),
                               String.valueOf(OPERATION_APN),
                               String.valueOf(OPERATION_WIFI)};
        operationPref.setKey(KEY_OPERATION);
        operationPref.setPersistent(true);
        operationPref.setEntries(entries);
        operationPref.setEntryValues(entryValues);
        category.addPreference(operationPref);

        operationPref.setValueIndex(OPERATION_AIRPLANE_MODE);
        operationPref.setTitle(context.getString(R.string.toggle_handler_operation_title));
        operationPref.setDialogTitle(context.getString(R.string.toggle_handler_dialog_title));
        operationPref.setOnPreferenceChangeListener(
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
            operationPref.setValueIndex(-1); // set to false.
        } else {
            Bundle result = getBundleFromExtra(defaultValue);
            int operation = result.getInt(KEY_OPERATION, -1);
            operationPref.setValueIndex(operation);
        }
        operationPref.setSummary(operationPref.getEntry());
    }

    @Override
    protected void putBundleIntoIntent(Intent intent, Bundle bundle) {
        final int operation = bundle.getInt(KEY_OPERATION, -1);
        intent.putExtra(KEY_OPERATION, operation);
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
                }
            }
        }
        return result;
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

    private void toggleAirplaneMode(Context context) {
        boolean enabled = isAirplaneModeOn(context);

        Log.d(TAG, "===> toggle airplane mode " + !enabled);


        setAirplaneModeEnabled(context, !enabled);
    }

    private void toggleWifi(Context context) {
        WifiManager wm =
            (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        boolean enabled = wm.isWifiEnabled();
        wm.setWifiEnabled(!enabled);
    }

}
