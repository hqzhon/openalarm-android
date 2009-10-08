/**
 * @file   AlarmTimePreference.java
 * @author  <josh@alchip.com>
 * @date   Wed Oct  7 17:13:07 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.alarmclockplus;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.preference.Preference;
import android.view.View;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class AlarmTimePreference extends TextViewPreference {
    private static final String TAG = "AlarmTimePreference";

    public AlarmTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void persistValue(Object value) {
        persistInt((Integer)value);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        Log.d(TAG, "onBindView(view)");

        final TextView timeText =
            (TextView)view.findViewById(R.id.text);

        int time = getPersistedInt(0);
        int hourOfDay = time / 100;
        int minutes = time % 100;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);
        timeText.setText(Alarms.formatDate("HH:mm", calendar));
    }

    /**
     * Bring up a TimePicker dialog.
     *
     */
    @Override
    protected void onClick() {
        int time = getPersistedInt(0);
        final int hourOfDay = time / 100;
        final int minutes = time % 100;

        new TimePickerDialog(
            getContext(),
            new TimePickerDialog.OnTimeSetListener() {
                public void onTimeSet(TimePicker view,
                                      int hourOfDay,
                                      int minutes) {
                    setPreferenceValue(hourOfDay * 100 + minutes);
                }
            },
            hourOfDay,
            minutes,
            true).show();
    }

    /**
     * Return default value of this Preference
     *
     * @param a
     * @param index
     *
     * @return
     */
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 815); // default time is 08:15
    }

    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defValue) {
        if(restorePersistedValue) {

            // Restore


        } else {
            setPreferenceValue(defValue);
        }
    }

    // protected Parcelable onSaveInstanceState() {

    // }

    // protected void onRestoreInstanceState(Parcelable state) {

    // }
}
