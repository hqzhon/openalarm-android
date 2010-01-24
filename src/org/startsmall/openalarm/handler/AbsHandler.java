package org.startsmall.openalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceCategory;

abstract class AbsHandler extends BroadcastReceiver {
    public static final String SEPARATOR = ";%%%%;";

    public abstract void addMyPreferences(final Context context,
                                          final PreferenceCategory category,
                                          final String defaultValue);

    protected abstract Bundle getBundleFromExtra(String extra);
    protected abstract void putBundleIntoIntent(Intent intent, Bundle bundle);
}
