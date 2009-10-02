package org.startsmall.alarmclockplus;

import android.app.ListActivity;
import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.CheckBox;

public class AlarmClockPlus extends ListActivity {
    private static final String TAG = "ALARM_CLOCK_PLUS";

    private class AlarmAdapter extends CursorAdapter {
        private LayoutInflater mLayoutInflater;

        public AlarmAdapter(Context context, Cursor c) {
            super(context, c);

            mLayoutInflater = AlarmClockPlus.this.getLayoutInflater();
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final int id = cursor.getInt(Alarms.AlarmColumns.PROJECTION_ID_INDEX);
            final String label = cursor.getString(Alarms.AlarmColumns.PROJECTION_LABEL_INDEX);
            final int hour = cursor.getInt(Alarms.AlarmColumns.PROJECTION_HOUR_INDEX);
            final int minutes = cursor.getInt(Alarms.AlarmColumns.PROJECTION_MINUTES_INDEX);

            // TODO: DAYS_OF_WEEK and TIME_IN_MILLIS

            final boolean enabled = cursor.getInt(Alarms.AlarmColumns.PROJECTION_ENABLED_INDEX) == 1;
            final boolean vibrate = cursor.getInt(Alarms.AlarmColumns.PROJECTION_VIBRATE_INDEX) == 1;
            final String audioAlert = cursor.getString(Alarms.AlarmColumns.PROJECTION_ALERT_URI_INDEX);

            // TODO: Alarm icon

            TextView labelView = (TextView)view.findViewById(R.id.label);
            labelView.setText(label);

            CheckBox enabledChkBox = (CheckBox)view.findViewById(R.id.enabled);
            enabledChkBox.setChecked(enabled);

            // TODO: Alarm settings
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view =
                mLayoutInflater.inflate(R.layout.alarm_list_item, parent, false);
            Log.d(TAG, "newView " + cursor.getPosition());
            return view;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ContentResolver contentResolver = getContentResolver();
        if(contentResolver == null) {

            // TODO: throw error message

            Log.d(TAG, "onCreate(): no content resolver");


        }

        Cursor cursor = Alarms.getCursor(contentResolver);
        startManagingCursor(cursor);
        setListAdapter(new AlarmAdapter(this, cursor));


    }
}
