package org.startsmall.alarmclockplus.receiver;

// import org.startsmall.alarmclockplus.*;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;

// import java.util.*;

public class InitReceiver extends BroadcastReceiver {
    private static final String TAG = "InitReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() at wall time " + System.currentTimeMillis());

        // Cancel expired alarms.

        // Cancel snoozed alarms.


        // Reschedule all enabled alarms;
        // Alarms.scheduleAlarms(context);

    }
}
