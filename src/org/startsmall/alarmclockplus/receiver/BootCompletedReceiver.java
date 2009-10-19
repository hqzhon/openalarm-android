package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.*;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.util.Log;

// import java.util.*;

public class BootCompleted extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceiver(Context context, Intent intent) {
        Alarms.sBootWallTimeInMillis = System.currentTimeMillis();

    }
}
