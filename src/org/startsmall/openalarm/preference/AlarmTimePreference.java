/**
 * @file   AlarmTimePreference.java
 * @author  <josh@alchip.com>
 * @date   Wed Oct  7 17:13:07 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.openalarm;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.widget.TimePicker;
import java.util.Calendar;

public class AlarmTimePreference extends TextViewPreference {
    public AlarmTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected Object parsePreferenceValue(String value) {
        return Integer.parseInt(value);
    }

    protected String toPreferenceValue(Object obj) {
        return Integer.toString((Integer)obj);
    }

    @Override
    public Dialog getDialog() {
        final int time = (Integer)getPreferenceValue();
        final int hourOfDay = time / 100;
        final int minutes = time % 100;
        final boolean is24HourFormat = Alarms.is24HourMode(getContext());

        return new TimePickerDialog(
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
            is24HourFormat);
    }

    @Override
    protected String transformValueBeforeDisplay(Object value) {
        int time = (Integer)value;
        final int hourOfDay = time / 100;
        final int minutes = time % 100;

        Calendar calendar = Alarms.getCalendarInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);

        return
            DateUtils.formatDateTime(getContext(),
                                     calendar.getTimeInMillis(),
                                     DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_CAP_AMPM);
    }
}
