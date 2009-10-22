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

        final int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);
        final String label = intent.getStringExtra(Alarms.AlarmColumns.LABEL);
        final String extra = intent.getStringExtra(Alarms.AlarmColumns.EXTRA);
        final String action = intent.getStringExtra(Alarms.AlarmColumns.ACTION);

        Log.v(TAG, "********* Received action request....id=" + alarmId
              + ", label=" + label);
    }
}
