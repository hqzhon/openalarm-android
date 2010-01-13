/**
 * @file   AbsAlarmHandler.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Sat Dec 26 16:27:35 2009
 *
 * @brief
 *
 *
 */

package org.startsmall.openalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.preference.PreferenceCategory;

abstract class AbsAlarmHandler extends BroadcastReceiver {
    public abstract void addMyPreferences(final Context context,
                                          final PreferenceCategory category,
                                          final String defaultValue);
}
