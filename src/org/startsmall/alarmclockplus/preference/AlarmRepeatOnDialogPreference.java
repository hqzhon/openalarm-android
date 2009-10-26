
package org.startsmall.alarmclockplus.preference;

import org.startsmall.alarmclockplus.Alarms;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.os.Parcel;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Arrays;
import java.util.Calendar;
import java.text.DateFormatSymbols;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class AlarmRepeatOnDialogPreference extends TextViewPreference
    implements DialogInterface.OnMultiChoiceClickListener,
               DialogInterface.OnClickListener {
    private static final String TAG = "AlarmRepeatOnDialogPreference";
    private Alarms.RepeatWeekdays mRepeatWeekdays = Alarms.RepeatWeekdays.getInstance();

    public AlarmRepeatOnDialogPreference(Context context,
                                         AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPreferenceValue(int daysCode) {
        mRepeatWeekdays.setCode(daysCode);
        setPreferenceValue(Integer.valueOf(daysCode).toString());
    }

    protected String formatValue(String value) {
        int code = Integer.parseInt(value);
        mRepeatWeekdays.setCode(code);
        return mRepeatWeekdays.toString();
    }

    protected void displayValueOnView(View view) {

        Log.d(TAG, "====> displayValueOnView(view)");


        setSummary(formatValue(getPreferenceValue()));
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
        builder
            .setMultiChoiceItems(weekdays, checked, this)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, null);
    }

    public void onClick(DialogInterface dialog,
                        int which,
                        boolean isChecked) {
        mRepeatWeekdays.set(which + 1, isChecked);
    }

    public void onClick(DialogInterface dialog, int which) {
        switch(which) {
        case DialogInterface.BUTTON_POSITIVE:
            setPreferenceValue(
                Integer.valueOf(mRepeatWeekdays.getCode()).toString());
            dialog.dismiss();
            break;
        }
    }
}
