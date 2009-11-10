/**
 * @file   ActionDispatcher.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Tue Nov 10 17:47:41 2009
 *
 * @brief Delegator that redirects intent to its stored handler.
 *
 *
 */

package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.Alarms;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;
import android.text.TextUtils;

public class ActionDispatcher extends BroadcastReceiver {
    private static final String TAG = "ActionDispatcher";

    @Override
    public void onReceive(Context context, Intent intent) {
        final int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);
        Log.v(TAG, "====> Received action request....id=" + alarmId);

        final String handler =
            intent.getStringExtra(Alarms.AlarmColumns.HANDLER);
        if(!TextUtils.isEmpty(handler)) {
            Class<?> handlerClass;
            try {
                handlerClass = Class.forName(handler);
                intent.setClass(context, handlerClass);
                context.sendBroadcast(intent);
            } catch (ClassNotFoundException e) {
                Log.d(TAG, "=================> Class not found - " + e);
            }
        }
    }
}
