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
import android.provider.BaseColumns;
import android.util.Log;

/**
 *
 *
 */
public class AlarmProvider extends ContentProvider {
    private static final String TAG = "ALARM_PROVIDER";
    private static final String DATABASE_TABLE_NAME = "alarms";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int URI_MATCH_ID_ALARMS = 1;
    private static final int URI_MATCH_ID_ALARM = 2;

    private DatabaseOpenHelper mDbOpenHelper;

    static {
        sURIMatcher.addURI(Alarms.AlarmColumns.CONTENT_URI,
                           "/alarms", URI_MATCH_ID_ALARMS);
        sURIMatcher.addURI(Alarms.AlarmColumns.CONTENT_URI,
                           "/alarms/#", URI_MATCH_ID_ALARM);
    }

    private static class DatabaseOpenHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "alarmclockplus.db";
        private static final int DATABASE_VERSION = 1;

        private final String DATABASE_CREATE_CMD =
            "CREATE TABLE " + DATABASE_TABLE_NAME + "(" +
            Alarms.AlarmColumns._ID   + "INTEGER PRIMARY KEY," +
            Alarms.AlarmColumns.LABEL + "TEXT," +
            Alarms.AlarmColumns.HOUR  + "INTEGER," +
            Alarms.AlarmColumns.DAYS_OF_WEEK + "INTEGER," + // TODO:
            // TODO: Time in millis
            Alarms.AlarmColumns.ENABLED + "INTEGER," +
            Alarms.AlarmColumns.VIBRATE + "INTEGER," +
            Alarms.AlarmColumns.ALERT_URI + "TEXT);";

        private final String DATABASE_DROP_CMD =
            "DROP TABLE IF EXISTS " + DATABASE_TABLE_NAME;

        public DatabaseOpenHelper(Context cxt) {
            super(cxt, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "DatabaseOpenHelper.onCreate(" +
                  DATABASE_CREATE_CMD + ")");
            db.execSQL(DATABASE_CREATE_CMD);
            insertDefaultAlarms();
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

        private void insertDefaultAlarms() {
            String cmd = "INSERT INTO " + DATABASE_TABLE_NAME + " (" +
                         Alarms.AlarmColumns.LABEL + ", " +
                         Alarms.AlarmColumns.HOUR + ", " +
                         Alarms.AlarmColumns.MINUTES + ", " +
                         Alarms.AlarmColumns.ENABLED + ", " +
                         Alarms.AlarmColumns.VIBRATE + ", " +
                         Alarms.AlarmColumns.ALERT_URI + ") VALUES ";
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL(cmd + "('default1', 7, 00, 0, 1, '');");
            db.execSQL(cmd + "('default2', 8, 30, 0, 0, '');");
            db.execSQL(cmd + "('default3', 9, 00, 0, 1, '');");
        }
    }

    @Override
    public boolean onCreate() {
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
        if(matchId != URI_MATCH_ID_ALARM &&
           matchId != URI_MATCH_ID_ALARMS) {
            throw new IllegalArgumentException("unknown alarm URL");
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DATABASE_TABLE_NAME);

        if(matchId == URI_MATCH_ID_ALARM) {
            long rowId = ContentUris.parseId(uri);
            qb.appendWhere(Alarms.AlarmColumns._ID + "=" + rowId);
        }

        Cursor c = qb.query(mDbOpenHelper.getReadableDatabase(),
                            projection,
                            selection, selectionArgs,
                            null, /* groupBy */
                            null, /* having */
                            sortOrder);
        if(c == null) {
            Log.d(TAG, "failed alarm query");
        } else {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if(sURIMatcher.match(uri) != URI_MATCH_ID_ALARMS) {
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
            values.put(Alarms.AlarmColumns.HOUR,0);
        }

        if(!values.containsKey(Alarms.AlarmColumns.MINUTES)) {
            values.put(Alarms.AlarmColumns.MINUTES, 0);
        }

        // TODO: DAYS_OF_WEEK


        // TODO: Time in millis

        if(!values.containsKey(Alarms.AlarmColumns.ENABLED)) {
            values.put(Alarms.AlarmColumns.ENABLED,0);
        }

        if(!values.containsKey(Alarms.AlarmColumns.VIBRATE)) {
            values.put(Alarms.AlarmColumns.VIBRATE,0);
        }

        if(!values.containsKey(Alarms.AlarmColumns.LABEL)) {
            values.put(Alarms.AlarmColumns.LABEL, "FIXME:default_label");
        }

        if(!values.containsKey(Alarms.AlarmColumns.ALERT_URI)) {
            values.put(Alarms.AlarmColumns.ALERT_URI, "");
        }

        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        long rowId = db.insertOrThrow(DATABASE_TABLE_NAME,
                                      Alarms.AlarmColumns.LABEL,
                                      values);

        Uri insertedUri =
            Uri.parse(Alarms.AlarmColumns.CONTENT_URI + "/alarms/" + rowId);
        getContext().getContentResolver().notifyChange(insertedUri, null);
        Log.d(TAG, "Added alarm - " + insertedUri);
        return insertedUri;
    }

    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection, String[] selectionArgs) {
        if(sURIMatcher.match(uri) != URI_MATCH_ID_ALARM) {
            throw new IllegalArgumentException(
                "unsupported content provider operation");
        }

        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        long rowId = ContentUris.parseId(uri);
        int count = db.update(DATABASE_TABLE_NAME, values,
                              Alarms.AlarmColumns._ID + "=" + rowId,
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
        case URI_MATCH_ID_ALARM: // delete one specific row.
            long rowId = ContentUris.parseId(uri);
            String where = Alarms.AlarmColumns._ID + "=" + rowId;
            if(selection.length() != 0) {
                where = where + " AND (" + selection + ")";
            }
            count = db.delete(DATABASE_TABLE_NAME, where, selectionArgs);
            break;
        case URI_MATCH_ID_ALARMS: // delete rows
            count = db.delete(DATABASE_TABLE_NAME,
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
        case URI_MATCH_ID_ALARMS:
            return "vnd.android.cursor.dir/vnd.startsmall.alarms";
        case URI_MATCH_ID_ALARM:
            return "vnd.android.cursor.item/vnd.startsmall.alarms";
        default:
            throw new IllegalArgumentException("unkown content URI");
        }
    }
}
