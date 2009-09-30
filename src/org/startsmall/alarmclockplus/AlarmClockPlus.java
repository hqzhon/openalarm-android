package org.startsmall.alarmclockplus;

import android.app.ListActivity;
import android.os.Bundle;

import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;

public class AlarmClockPlus extends ListActivity {
    private static final String TAG = "ALARM_CLOCK_PLUS";

    private LayoutInflater mLayoutInflater;

    private class AlarmAdapter extends CursorAdapter {
        private int mLayoutResId;
        private String[] mFromColumns;
        private int[] mToViewIds;

        public AlarmAdapter(Context context,
                            int layoutResId,
                            Cursor c,
                            String[] from,
                            int[] to) {
            super(context, c);

            mLayoutResId = layoutResId;
            mFromColumns = from;
            mToViewIds = to;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final int id = cursor.getInt(    );
            final int hour = cursor.getInt(   );
            final int minutes = cursor.getInt(   );


            final boolean enabled = cursor.getBoolean(   );
            final boolean vibrate = cursor.getBoolean(   );
            final String audioAlert = cursor.getString(   );
            final String label = cursor.getString(   );



        }

        @Override
        public void newView(Context context,
                            Cursor cursor,
                            ViewGroup parent) {
            View view =
                mLayoutInflater.inflate(mLayoutResId, parent, false);
            if (Log.LOGV) {
                Log.v("newView " + cursor.getPosition());
            }
            return view;
        }
    }


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mLayoutInflater = getLayoutInflater();







    }
}
