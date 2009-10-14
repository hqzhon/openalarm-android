package org.startsmall.alarmclockplus;

import android.content.Context;
// import android.content.ContentResolver;
// import android.content.ContentUris;
import android.content.BroadcastReceiver;
import android.content.Intent;
//import android.database.Cursor;
// import android.net.Uri;
// import android.net.wifi.WifiManager;
// import android.provider.BaseColumns;
import android.util.Log;

// import java.util.*;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String TITLE = "Alarm";

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> My AlarmReceiver.onReceive() haha");
    }
}
