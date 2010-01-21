package org.startsmall.openalarm;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;
import java.util.Calendar;

/**
 * All enabled alarms should be rescheduled if system time
 * changed, time zone changed or machine rebooted.
 *
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Calendar calendar = Calendar.getInstance();
        Log.i(TAG, "===> onReceive(" + intent.getAction() + ")" + " start: " + calendar);

        // Redirect time-consuming alarm scheduling to BootService.
        intent.setAction(null);
        intent.setClassName("org.startsmall.openalarm", "org.startsmall.openalarm.BootService");
        context.startService(intent);

        calendar = Calendar.getInstance();
        Log.i(TAG, "===> onReceive(" + intent.getAction() + ") end: " + calendar);
    }
}
