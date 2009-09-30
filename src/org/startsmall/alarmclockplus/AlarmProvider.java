/**
 * @file   Alarms.java
 * @author  <yenliangl@gmail.com>
 * @date   Wed Sep 30 16:47:42 2009
 *
 * @brief  Content provider.
 *
 * A bridge between content resolver and internal database.
 *
 */
package org.startsmall.alarmclockplus;

import android.content.ContentProvider;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 *
 *
 */
public class AlarmProvider extends ContentProvider {

    private static final TAG = "ALARM_PROVIDER";

    private DatabaseOpenHelper mDbOpenHelper;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private final int URI_MATCH_ID_ALARMS = 1;
    private final int URI_MATCH_ID_ALARM = 2;

    static {
        sURIMatcher.addURI(Alarms.AlarmColumns.CONTENT_URI,
                           "/alarms", URI_MATCH_ID_ALARMS);
        sURIMatcher.addURI(Alarms.AlarmColumns.CONTENT_URI,
                           "/alarms/#", URI_MATCH_ID_ALARM);
    }

    private static class DatabaseOpenHelper extends SQLiteOpenHelper {
        private final String DATABASE_NAME = "alarmclockplus.db";
        private final String DATABASE_TABLE_NAME = "alarms";
        private final int DATABASE_VERSION = 1;

        private final String DATABASE_CREATE_CMD =
            "CREATE TABLE " + DATABASE_TABLE_NAME + "(" +
            Alarms.AlarmColumns._ID   + "INTEGER PRIMARY KEY," +
            Alarms.AlarmColumns.LABEL + "TEXT," +
            Alarms.AlarmColumns.HOUR  + "INTEGER,"
            Alarms.AlarmColumns.DAYS_OF_WEEK + "INTEGER," +
            Alarms.AlarmColumns.ENABLED + "INTEGER," +
            Alarms.AlarmColumns.VIBRATE + "INTEGER," +
            Alarms.AlarmColumns.ALERT_URI + "TEXT);";

        private final String DATABASE_DROP_CMD =
            "DROP TABLE IF EXISTS " + DATABASE_TABLE_NAME;

        public DbOpenHelper(Context cxt) {
            super(cxt, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "DatabaseOpenHelper.onCreate(" +
                  DATABASE_CREATE_CMD + ")");
            db.execSQL(DATABASE_CREATE_CMD);

            // @note: should we insert default alarm entries???
        }

        @Override
        public void onUpgrade(SQLiteDatabase db,
                              int oldVersion,
                              int newVersion) {
            Log.d(TAG, "DatabaseOpenHelper.onUpgrade(" +
                  DATABASE_DROP_CMD + ")");
            db.execSQL(DATABASE_DROP_CMD);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        mDbOpenHelper = new DatabaseOpenHelper(getContext());
        return true;
    }

    // TODO:
    @Override
    public Cursor query(Uri uri, String[] projection,
                        String selection,
                        String selectionArgs,
                        String sortOrder) {



    }

    // TODO:
    @Override
    public Uri inert(Uri rui, ContentValues values) {


    }

    // TODO:
    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs) {


    }

    // TODO:
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
    }

    @Override
    public String getType(Uri uri) {
        switch(sURIMatcher.match(uri)) {
        case URI_MATCH_ID_ALARMS:
            return "vnd.android.cursor.dir/vnd.startsmall.alarms";
        case URI_MATCH_ID_ALARM:
            return "vnd.android.cursor.item/vnd.startsmall.alarms"
        default:
            return new IllegalArgumentException("unkown content URI");
        }
    }
}
