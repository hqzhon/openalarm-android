package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.Alarms;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;

public class ActionDispatcher extends BroadcastReceiver {
    private static final String TAG = "ActionDispatcher";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> Received action request....");

        // The action string stored in intent should be dispatched here.
        String actionUri = intent.getStringExtra(Alarms.INTENT_EXTRA_ALARM_ACTION_KEY);
        Log.d(TAG, "Prepare to launch " + actionUri);

    }
}
