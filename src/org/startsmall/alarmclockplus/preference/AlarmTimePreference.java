/**
 * @file   AlarmTimePreference.java
 * @author  <josh@alchip.com>
 * @date   Wed Oct  7 17:13:07 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.alarmclockplus.preference;

import org.startsmall.alarmclockplus.*;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TimePicker;

import java.util.Calendar;

public class AlarmTimePreference extends TextViewPreference {
    public AlarmTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Dialog getDialog() {
        int time = Integer.parseInt(getPreferenceValue());
        final int hourOfDay = time / 100;
        final int minutes = time % 100;

        return new TimePickerDialog(
            getContext(),
            new TimePickerDialog.OnTimeSetListener() {
                public void onTimeSet(TimePicker view,
                                      int hourOfDay,
                                      int minutes) {
                    AlarmTimePreference.this.
                        setPreferenceValue(
                            Integer.toString(hourOfDay * 100 + minutes));
                }
            },
            hourOfDay,
            minutes,
            true);
    }

    protected String formatDisplayValue(String value) {
        int time = Integer.parseInt(value);
        final int hourOfDay = time / 100;
        final int minutes = time % 100;
        Calendar calendar = Alarms.getCalendarInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);
        return Alarms.formatDate("HH:mm", calendar);
    }
}
