/**
 * @file   AbsActionHandler.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Sat Dec 26 16:27:35 2009
 *
 * @brief
 *
 *
 */

package org.startsmall.openalarm;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.preference.PreferenceCategory;

abstract class AbsActionHandler extends BroadcastReceiver {
    @Override
    public abstract void onReceive(Context context, Intent intent);
    public abstract void addMyPreferences(final Context context,
                                          final PreferenceCategory category,
                                          final String defaultValue);
}
