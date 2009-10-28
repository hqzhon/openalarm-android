package org.startsmall.alarmclockplus;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;

abstract class ActionHandler extends BroadcastReceiver {
    @Override
    public abstract void onReceive(Context context, Intent intent);
    public abstract void addMyPreferences(final Context context,
                                          final PreferenceCategory category,
                                          final String defaultValue);
}
