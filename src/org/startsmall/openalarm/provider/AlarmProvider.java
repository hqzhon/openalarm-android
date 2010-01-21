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
package org.startsmall.openalarm;

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
        /// org.startsmall.openalarm/alarms
        sURIMatcher.addURI(Alarms.CONTENT_URI_AUTH,
                           Alarms.CONTENT_URI_PATH,
                           MATCH_CODE_ALL_ALARMS);

        /// org.startsmall.openalarm/alarms/#
        sURIMatcher.addURI(Alarms.CONTENT_URI_AUTH,
                           Alarms.CONTENT_URI_PATH + "/#",
                           MATCH_CODE_SINGLE_ALARM);
    }

    private class DatabaseOpenHelper extends SQLiteOpenHelper {
        private static final String TAG = "DatabaseOpenHelper";
        private static final String DATABASE_NAME = "openalarm.db";
        public static final String DATABASE_TABLE_NAME = "alarms";
        private static final int DATABASE_VERSION = 1;

        private static final String DATABASE_CREATE_CMD =
            "CREATE TABLE " + DATABASE_TABLE_NAME + "(" +
            AlarmColumns._ID   + " INTEGER PRIMARY KEY, " +
            AlarmColumns.LABEL + " TEXT, " +
            AlarmColumns.HOUR_OF_DAY  + " INTEGER, " +
            AlarmColumns.MINUTES  + " INTEGER, " +
            AlarmColumns.TIME_IN_MILLIS  + " INTEGER, " +
            AlarmColumns.REPEAT_DAYS + " INTEGER, " +
            AlarmColumns.ENABLED + " INTEGER, " +
            AlarmColumns.HANDLER + " TEXT, " +
            AlarmColumns.EXTRA + " TEXT);";

        private static final String DATABASE_DROP_CMD =
            "DROP TABLE IF EXISTS " + DATABASE_TABLE_NAME;

        public DatabaseOpenHelper(Context cxt) {
            super(cxt, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // Log.d(TAG, "===> onCreate(" + DATABASE_CREATE_CMD + ")");
            db.execSQL(DATABASE_CREATE_CMD);
            insertDefaultAlarms(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db,
                              int oldVersion,
                              int newVersion) {
            // Log.d(TAG, "===> Upgrading database from " + oldVersion + " to " + newVersion);
            db.execSQL(DATABASE_DROP_CMD);
            onCreate(db);
        }

        private void insertDefaultAlarms(SQLiteDatabase db) {
            String cmd = "INSERT INTO " + DATABASE_TABLE_NAME + " (" +
                         AlarmColumns.LABEL + ", " +
                         AlarmColumns.HOUR_OF_DAY + ", " +
                         AlarmColumns.MINUTES + ", " +
                         AlarmColumns.TIME_IN_MILLIS + ", " +
                         AlarmColumns.REPEAT_DAYS + ", " +
                         AlarmColumns.ENABLED + ", " +
                         AlarmColumns.HANDLER + ", " +
                         AlarmColumns.EXTRA + ") VALUES ";
            db.execSQL(cmd + "('Go to work', 7, 00, 0, 1, 0, '', '');");
            db.execSQL(cmd + "('Pick up kids', 8, 30, 0, 5, 0, '', '');");
            db.execSQL(cmd + "('See her mom', 9, 00, 0, 9, 0, '', '');");
        }
    }

    @Override
    public boolean onCreate() {
        // Log.d(TAG, "===> onCreate()");

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
        if (matchId != MATCH_CODE_SINGLE_ALARM &&
           matchId != MATCH_CODE_ALL_ALARMS) {
            throw new IllegalArgumentException("Unknown alarm URI");
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DatabaseOpenHelper.DATABASE_TABLE_NAME);

        if (matchId == MATCH_CODE_SINGLE_ALARM) {
            long rowId = ContentUris.parseId(uri);
            qb.appendWhere(AlarmColumns._ID + "=" + rowId); // append _id=#
        }

        Cursor c = qb.query(mDbOpenHelper.getReadableDatabase(),
                            projection,
                            selection, selectionArgs,
                            null, /* groupBy */
                            null, /* having */
                            sortOrder);
        if (c == null) {
            Log.e(TAG, "===> AlarmProvider.query(): failed alarm query");
        } else {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (sURIMatcher.match(uri) != MATCH_CODE_ALL_ALARMS) {
            throw new IllegalArgumentException(
                "Unable to insert into URL - " + uri);
        }

        if (values == null) {
            values = new ContentValues();
        }
        // ContentValues values;
        // if (initialValues == null) {
        //     values = new ContentValues();
        // } else {
        //     values = new ContentValues(initialValues);
        // }

        if (!values.containsKey(AlarmColumns.HOUR_OF_DAY)) {
            values.put(AlarmColumns.HOUR_OF_DAY, 9);
        }

        if (!values.containsKey(AlarmColumns.MINUTES)) {
            values.put(AlarmColumns.MINUTES, 0);
        }

        if (!values.containsKey(AlarmColumns.TIME_IN_MILLIS)) {
            values.put(AlarmColumns.TIME_IN_MILLIS, 0L);
        }

        if (!values.containsKey(AlarmColumns.REPEAT_DAYS)) {
            values.put(AlarmColumns.REPEAT_DAYS, 0);
        }

        if (!values.containsKey(AlarmColumns.ENABLED)) {
            values.put(AlarmColumns.ENABLED, 0);
        }

        if (!values.containsKey(AlarmColumns.LABEL)) {
            values.put(AlarmColumns.LABEL, "My Alarm");
        }

        if (!values.containsKey(AlarmColumns.HANDLER)) {
            values.put(AlarmColumns.HANDLER, "");
        }

        if (!values.containsKey(AlarmColumns.EXTRA)) {
            values.put(AlarmColumns.EXTRA, "");
        }

        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        long rowId = db.insertOrThrow(DatabaseOpenHelper.DATABASE_TABLE_NAME,
                                      AlarmColumns.LABEL,
                                      values);

        Uri insertedUri = Alarms.getAlarmUri((int)rowId);
        getContext().getContentResolver().notifyChange(insertedUri, null);
        return insertedUri;
    }

    @Override
    public int update(Uri uri,
                      ContentValues values,
                      String selection, String[] selectionArgs) {
        if (sURIMatcher.match(uri) != MATCH_CODE_SINGLE_ALARM) {
            throw new IllegalArgumentException(
                "unsupported content provider operation");
        }

        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        int count =
            db.update(
                DatabaseOpenHelper.DATABASE_TABLE_NAME,
                values,
                AlarmColumns._ID + "=" + ContentUris.parseId(uri),
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
                AlarmColumns._ID + "=" + ContentUris.parseId(uri) +
                ((!TextUtils.isEmpty(selection)) ?
                 " AND (" + selection + ")" : "");
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
