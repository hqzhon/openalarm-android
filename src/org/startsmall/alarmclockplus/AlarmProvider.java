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
    private DatabaseOpenHelper mDbOpenHelper;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private final int URI_MATCH_ID_ALARMS = 1;
    private final int URI_MATCH_ID_ALARM = 2;

    static {
        sURIMatcher.addURI(Alarms.AlarmColumns.CONTENT_URI,
                           "/alarms", URI_MATCH_ID_ALARMS);
        sURIMatcher.addURI(Alarms.AlarmColumns.CONTENT_URI,
                           "/alarm/#", URI_MATCH_ID_ALARM);
    }

    private static class DatabaseOpenHelper extends SQLiteOpenHelper {
        private final String DATABASE_NAME = "alarmclockplus.db";
        private final String DATABASE_TABLE_NAME = "alarms";
        private final int DATABASE_VERSION = 1;

        public DbOpenHelper(Context cxt) {
            super(cxt, DATABASE_NAME, null, DATABASE_VERSION);
        }

        // TODO:
        @Override
        public void onCreate(SQLiteDatabase db) {
            // Execute SQLite database creation statement
        }

        // TODO:
        @Override
        public void onUpgrade(SQLiteDatabase db,
                              int oldVersion,
                              int newVersion) {
            // Execute SQLite database upgrade statement
        }
    }

    @Override
    public boolean onCreate() {
        mDbOpenHelper = new DatabaseOpenHelper(getContext());
        return true;
    }

    // TODO:
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String selectionArgs, String sortOrder) {



    }

    // TODO:
    @Override
    public Uri inert(Uri rui, ContentValues values) {


    }

    // TODO:
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {


    }

    // TODO:
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
    }

    @Override
    public String getType(Uri uri) {
        switch(sURIMatcher.match(uri)) {
        case URI_MATCH_ID_ALARMS:
            return "vnd.android.cursor.dir/alarms";
        case URI_MATCH_ID_ALARM:
            return "vnd.android.cursor.item/alarm"
        default:
            return new IllegalArgumentException("unkown content URI");
        }
    }
}


