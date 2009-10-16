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
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 *
 *
 */
public class AlarmProvider extends ContentProvider {
    private static final String TAG = "AlarmProvider";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int MATCH_CODE_ALL_ALARMS = 1;
    private static final int MATCH_CODE_SINGLE_ALARM = 2;

    private DatabaseOpenHelper mDbOpenHelper;

    static {
        /// org.startsmall.alarmclockplus/alarms
        sURIMatcher.addURI(Alarms.CONTENT_URI_AUTH,
                           Alarms.CONTENT_URI_PATH,
                           MATCH_CODE_ALL_ALARMS);

        /// org.startsmall.alarmclockplus/alarms/#
        sURIMatcher.addURI(Alarms.CONTENT_URI_AUTH,
                           Alarms.CONTENT_URI_PATH + "/#",
                           MATCH_CODE_SINGLE_ALARM);
    }

    private class DatabaseOpenHelper extends SQLiteOpenHelper {
        private static final String TAG = "DatabaseOpenHelper";
        private static final String DATABASE_NAME = "alarmclockplus.db";
        public static final String DATABASE_TABLE_NAME = "alarms";
        private static final int DATABASE_VERSION = 1;

        private static final String DATABASE_CREATE_CMD =
            "CREATE TABLE " + DATABASE_TABLE_NAME + "(" +
            Alarms.AlarmColumns._ID   + " INTEGER PRIMARY KEY, " +
            Alarms.AlarmColumns.LABEL + " TEXT, " +
            Alarms.AlarmColumns.HOUR  + " INTEGER, " +
            Alarms.AlarmColumns.MINUTES  + " INTEGER, " +
            Alarms.AlarmColumns.REPEAT_DAYS + " INTEGER, " +
            Alarms.AlarmColumns.ENABLED + " INTEGER, " +
            Alarms.AlarmColumns.VIBRATE + " INTEGER, " +
            Alarms.AlarmColumns.ALERT_URI + " TEXT);";

        private static final String DATABASE_DROP_CMD =
            "DROP TABLE IF EXISTS " + DATABASE_TABLE_NAME;

        public DatabaseOpenHelper(Context cxt) {
            super(cxt, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "onCreate(" + DATABASE_CREATE_CMD + ")");
            db.execSQL(DATABASE_CREATE_CMD);
            insertDefaultAlarms(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db,
                              int oldVersion,
                              int newVersion) {
            Log.d(TAG, "Upgrading database from " + oldVersion + " to " + newVersion);
            db.execSQL(DATABASE_DROP_CMD);
            onCreate(db);
        }

        private void insertDefaultAlarms(SQLiteDatabase db) {
            String cmd = "INSERT INTO " + DATABASE_TABLE_NAME + " (" +
                         Alarms.AlarmColumns.LABEL + ", " +
                         Alarms.AlarmColumns.HOUR + ", " +
                         Alarms.AlarmColumns.MINUTES + ", " +
                         Alarms.AlarmColumns.REPEAT_DAYS + ", " +
                         Alarms.AlarmColumns.ENABLED + ", " +
                         Alarms.AlarmColumns.VIBRATE + ", " +
                         Alarms.AlarmColumns.ALERT_URI + ") VALUES ";
            db.execSQL(cmd + "('default1', 7, 00, 1, 0, 1, '');");
            db.execSQL(cmd + "('default2', 8, 30, 5, 0, 0, '');");
            db.execSQL(cmd + "('default3', 9, 00, 9, 0, 1, '');");
        }
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate()");

        mDbOpenHelper = new DatabaseOpenHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {
        int matchId = sURIMatcher.match(uri);
        if(matchId != MATCH_CODE_SINGLE_ALARM &&
           matchId != MATCH_CODE_ALL_ALARMS) {
            throw new IllegalArgumentException("Unknown alarm URI");
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DatabaseOpenHelper.DATABASE_TABLE_NAME);

        if(matchId == MATCH_CODE_SINGLE_ALARM) {
            long rowId = ContentUris.parseId(uri);
            qb.appendWhere(Alarms.AlarmColumns._ID + "=" + rowId); // append _id=#
        }

        Cursor c = qb.query(mDbOpenHelper.getReadableDatabase(),
                            projection,
                            selection, selectionArgs,
                            null, /* groupBy */
                            null, /* having */
                            sortOrder);
        if(c == null) {
            Log.d(TAG, "AlarmProvider.query(): failed alarm query");
        } else {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if(sURIMatcher.match(uri) != MATCH_CODE_ALL_ALARMS) {
            throw new IllegalArgumentException(
                "unable to insert into URL - " + uri);
        }

        ContentValues values;
        if(initialValues == null) {
            values = new ContentValues();
        } else {
            values = new ContentValues(initialValues);
        }

        if(!values.containsKey(Alarms.AlarmColumns.HOUR)) {
            values.put(Alarms.AlarmColumns.HOUR, 0);
        }

        if(!values.containsKey(Alarms.AlarmColumns.MINUTES)) {
            values.put(Alarms.AlarmColumns.MINUTES, 0);
        }

        if(!values.containsKey(Alarms.AlarmColumns.REPEAT_DAYS)) {
            values.put(Alarms.AlarmColumns.REPEAT_DAYS, 0);
        }

        if(!values.containsKey(Alarms.AlarmColumns.ENABLED)) {
            values.put(Alarms.AlarmColumns.ENABLED, 0);
        }

        if(!values.containsKey(Alarms.AlarmColumns.VIBRATE)) {
            values.put(Alarms.AlarmColumns.VIBRATE, 0);
        }

        if(!values.containsKey(Alarms.AlarmColumns.LABEL)) {
            values.put(Alarms.AlarmColumns.LABEL, "My Alarm");
        }

        if(!values.containsKey(Alarms.AlarmColumns.ALERT_URI)) {
            values.put(Alarms.AlarmColumns.ALERT_URI, "");
        }

        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        long rowId = db.insertOrThrow(DatabaseOpenHelper.DATABASE_TABLE_NAME,
                                      Alarms.AlarmColumns.LABEL,
                                      values);
        Log.d(TAG, "Trying to insert a row into " + uri);

        Uri insertedUri = Alarms.getAlarmUri(rowId);
        Log.d(TAG, "Added alarm - " + insertedUri);
        getContext().getContentResolver().notifyChange(insertedUri, null);
        return insertedUri;
    }

    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection, String[] selectionArgs) {
        if(sURIMatcher.match(uri) != MATCH_CODE_SINGLE_ALARM) {
            throw new IllegalArgumentException(
                "unsupported content provider operation");
        }

        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        int count =
            db.update(
                DatabaseOpenHelper.DATABASE_TABLE_NAME,
                values,
                Alarms.AlarmColumns._ID + "=" + ContentUris.parseId(uri),
                null);

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        int matchId = sURIMatcher.match(uri);
        int count;
        switch(matchId) {
        case MATCH_CODE_SINGLE_ALARM: // delete one specific row.
            String where =
                Alarms.AlarmColumns._ID + "=" + ContentUris.parseId(uri) +
                (!TextUtils.isEmpty(selection)) ?
                " AND (" + selection + ")" : "";
            count = db.delete(DatabaseOpenHelper.DATABASE_TABLE_NAME,
                              where, selectionArgs);
            break;
        case MATCH_CODE_ALL_ALARMS: // delete rows
            count = db.delete(DatabaseOpenHelper.DATABASE_TABLE_NAME,
                              selection,
                              selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("cannot delete URI");
        }

        Log.d(TAG, "Deleted alarm - " + uri);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch(sURIMatcher.match(uri)) {
        case MATCH_CODE_ALL_ALARMS:
            return "vnd.android.cursor.dir/vnd.startsmall.alarms";
        case MATCH_CODE_SINGLE_ALARM:
            return "vnd.android.cursor.item/vnd.startsmall.alarms";
        default:
            throw new IllegalArgumentException("unkown content URI");
        }
    }
}
