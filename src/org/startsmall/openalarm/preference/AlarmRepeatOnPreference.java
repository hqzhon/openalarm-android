/**
 * @file   AlarmRepeatOnPreference.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Thu Oct 29 11:18:32 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.openalarm;

import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.text.DateFormatSymbols;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class AlarmRepeatOnPreference extends MyPreference
    implements DialogInterface.OnMultiChoiceClickListener,
               DialogInterface.OnClickListener {
    private int mWhichDaysChecked;

    public AlarmRepeatOnPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected Object parsePreferenceValue(String value) {
        return Integer.parseInt(value);
    }

    protected String toPreferenceValue(Object value) {
        return ((Integer)value).toString();
    }

    public Dialog getDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        onPrepareDialogBuilder(builder);
        mWhichDaysChecked = (Integer)getPreferenceValue();
        return builder.create();
    }

    @Override
    protected void onBindView(View view) {
        setSummary(
            Alarms.RepeatWeekdays.toString(getContext(), (Integer)getPreferenceValue()));
        super.onBindView(view);
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        boolean[] checked = new boolean[7];
        for(int i = Alarms.RepeatWeekdays.MONDAY; i <= Alarms.RepeatWeekdays.SUNDAY; i++) {
            if(Alarms.RepeatWeekdays.isSet((Integer)getPreferenceValue(), i)) {
                checked[i] = true;
            }
        }

        CharSequence[] weekdays = new CharSequence[7];
        System.arraycopy(getContext().getResources().getStringArray(R.array.days_of_week),
                         Alarms.RepeatWeekdays.MONDAY,
                         weekdays,
                         0,
                         Alarms.RepeatWeekdays.SUNDAY + 1);
        builder
            .setTitle(R.string.alarm_settings_repeat_days_dialog_title)
            .setMultiChoiceItems(weekdays, checked, this)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, null);
    }

    public void onClick(DialogInterface dialog,
                        int which,
                        boolean isChecked) {
        mWhichDaysChecked = Alarms.RepeatWeekdays.set(mWhichDaysChecked,
                                                      which,
                                                      isChecked);
    }

    public void onClick(DialogInterface dialog, int which) {
        switch(which) {
        case DialogInterface.BUTTON_POSITIVE:
            setPreferenceValue(mWhichDaysChecked);
            break;
        case DialogInterface.BUTTON_NEGATIVE:
            break;
        }
        dialog.dismiss();
    }
}
