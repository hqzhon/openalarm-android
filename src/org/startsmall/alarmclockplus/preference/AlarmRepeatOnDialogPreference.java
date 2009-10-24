
package org.startsmall.alarmclockplus.preference;

import org.startsmall.alarmclockplus.Alarms;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;


import java.util.Arrays;
import java.util.Calendar;
import java.text.DateFormatSymbols;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class AlarmRepeatOnDialogPreference extends DialogPreference
    implements DialogInterface.OnMultiChoiceClickListener {

    public interface OnRepeatWeekdaysSetListener {
        public void onRepeatWeekdaysSet(Alarms.RepeatWeekdays weekdays);
    }

    private static final String TAG = "AlarmRepeatOnDialogPreference";
    private Alarms.RepeatWeekdays mRepeatWeekdays = Alarms.RepeatWeekdays.getInstance();
    private OnRepeatWeekdaysSetListener mListener;

    public AlarmRepeatOnDialogPreference(Context context,
                                         AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AlarmRepeatOnDialogPreference(Context context,
                                         AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnRepeatWeekdaysSetListener(OnRepeatWeekdaysSetListener listener) {
        mListener = listener;
    }

    public void setPreferenceValue(int daysCode) {
        mRepeatWeekdays.setCode(daysCode);
        setSummary(mRepeatWeekdays.toString());
    }

    public int getPreferenceValue() {
        return mRepeatWeekdays.getCode();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        boolean[] checked = new boolean[7];
        for(int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            if(mRepeatWeekdays.hasDay(i)) {
                checked[i-1] = true;
            }
        }

        SimpleDateFormat dateFormat =
            (SimpleDateFormat)DateFormat.getDateInstance(DateFormat.LONG);
        DateFormatSymbols dateFormatSymbols =
            dateFormat.getDateFormatSymbols();
        CharSequence[] weekdays = new CharSequence[7];
        System.arraycopy(dateFormatSymbols.getWeekdays(),
                         Calendar.SUNDAY,
                         weekdays,
                         0,
                         7);
        builder.setMultiChoiceItems(weekdays, checked, this);
    }

    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        mRepeatWeekdays.set(which + 1, isChecked);
    }

    public void onClick(DialogInterface dialog, int which) {
        switch(which) {
        case DialogInterface.BUTTON_POSITIVE:
            if(mListener != null) {
                mListener.onRepeatWeekdaysSet(mRepeatWeekdays);
            }
            dialog.dismiss();
            break;
        // default:
        //     dialog.cancel();
        //     break;
        }
    }
}
