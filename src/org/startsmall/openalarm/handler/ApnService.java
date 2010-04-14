package org.startsmall.openalarm;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;

public class ApnService extends Service {
    private static class ApnInfo {
        public String id;
        public String apn;
        public String type;
    }

    private static final String TAG = "ApnService";

    private static final Uri CARRIERS_URI = Uri.parse("content://telephony/carriers");
    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_APN = 1;
    private static final int COLUMN_INDEX_TYPES = 2;

    private static final String COLUMN_NAME_ID = "_id";
    private static final String COLUMN_NAME_APN = "apn";
    private static final String COLUMN_NAME_TYPES = "type";

    private static final String SUFFIX = "openalarm";

    @Override
    public void onStart(Intent intent, int startId) {
        ConnectivityManager cm =
            (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo =
            cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean enabled = networkInfo.isConnectedOrConnecting();
        boolean onOff = intent.getBooleanExtra(ToggleHandler.KEY_ONOFF, false);
        if (enabled != onOff) {
            Log.d(TAG, "==> toggle all apn " + onOff);
            toggleAllApns(onOff);
        }
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent i) {return null;}

    private boolean toggleAllApns(boolean toggle) {
        ContentResolver contentResolver = getContentResolver();

        String whereStatement = null;
        String[] selectionArgs = null;
        if (toggle) { // apn apndroid => apn
            // ? in the statement will be replaced with
            // selectionArgs and put single quotes around it.
            whereStatement = COLUMN_NAME_APN + " like ? or " +
                             COLUMN_NAME_TYPES + " like ?";
            // Get arguments with a percentage before SUFFIX. The
            // replacement of ?s happening in whereState by query
            // doesn't automatically add %.
            selectionArgs = new String[]{ "%" + SUFFIX, "%" + SUFFIX};
        }

        // Get all APN out of database and toggle them.
        List<ApnInfo> apnInfoList = getApnInfoList(contentResolver, whereStatement, selectionArgs);
        int count = 0;
        ContentValues values = new ContentValues();
        for (ApnInfo apnInfo : apnInfoList) {
            String newApn = getNewApnString(apnInfo.apn, toggle);
            String newType = getNewApnString(apnInfo.type, toggle);

            values.clear();
            values.put(COLUMN_NAME_APN, newApn );
            values.put(COLUMN_NAME_TYPES, newType );
            if (contentResolver.update(
                    CARRIERS_URI, values,
                    COLUMN_NAME_ID + "=?", new String[]{apnInfo.id}) > 0) {
                count++;
            }
        }

        return count == apnInfoList.size();
    }

    private List<ApnInfo> getApnInfoList(ContentResolver contentResolver,
                                         final String whereStatement,
                                         final String[] selectionArgs) {
        List<ApnInfo> retList = new ArrayList<ApnInfo>();
        Cursor apnCursor =
            contentResolver.query(CARRIERS_URI,
                                  new String[]{COLUMN_NAME_ID,
                                               COLUMN_NAME_APN,
                                               COLUMN_NAME_TYPES},
                                  whereStatement, selectionArgs, null);
        if (apnCursor.moveToFirst()) {
            do {
                ApnInfo apnInfo = new ApnInfo();

                apnInfo.id = apnCursor.getString(COLUMN_INDEX_ID);
                apnInfo.apn = apnCursor.getString(COLUMN_INDEX_APN);
                apnInfo.type = apnCursor.getString(COLUMN_INDEX_TYPES);

                retList.add(apnInfo);
            } while (apnCursor.moveToNext());
        }
        apnCursor.close();
        return retList;
    }

    private String getNewApnString(String apnName, boolean enabled) {
        return enabled ? getApnUpString(apnName) : getApnDownString(apnName);
    }

    private String getApnUpString(String apnName) {
        if (TextUtils.isEmpty(apnName)) {
            return apnName;
        }

        int index = apnName.indexOf(SUFFIX);
        if (index < 0) {
            // No suffix, already enabled or other software
            // steps in APN stuff.
            return apnName;
        } else if (index == 0) {
            return "";
        } else {
            return apnName.substring(0, index - 1);
        }
    }

    private String getApnDownString(String apnName) {
        if (TextUtils.isEmpty(apnName)) {
            return SUFFIX;
        }

        int index = apnName.indexOf(SUFFIX);
        if (index >= 0) {
            // Have suffix... already disabled
            return apnName;
        } else {
            return apnName + " " + SUFFIX;
        }
    }
}
