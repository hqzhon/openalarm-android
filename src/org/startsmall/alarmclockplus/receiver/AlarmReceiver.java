package org.startsmall.alarmclockplus.receiver;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String TITLE = "Alarm";

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> My AlarmReceiver.onReceive() haha");
    }
}
