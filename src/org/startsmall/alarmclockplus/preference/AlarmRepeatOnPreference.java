
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

public class AlarmRepeatOnPreference extends TextViewPreference
    implements DialogInterface.OnMultiChoiceClickListener,
               DialogInterface.OnClickListener {
    private static final String TAG = "AlarmRepeatOnDialogPreference";
    private int mCode = -1;

    public AlarmRepeatOnPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPreferenceValue(int daysCode) {
        mCode = daysCode;
        setPreferenceValue(Integer.valueOf(mCode).toString());
    }

    protected String formatDisplayValue(String value) {
        return Alarms.RepeatWeekdays.toString(mCode);
    }

    protected String formatPersistedValue(String value) {
        mCode = Integer.parseInt(value);
        return value;
    }

    protected void displayValueOnView(View view) {
        setSummary(formatDisplayValue(getPreferenceValue()));
        Log.d(TAG, "====> displayValueOnView(view): "
              + formatDisplayValue(getPreferenceValue()));
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        boolean[] checked = new boolean[7];
        for(int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
            if(Alarms.RepeatWeekdays.isSet(mCode, i)) {
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
        mCode = Alarms.RepeatWeekdays.set(mCode, which+1, isChecked);
        Log.d(TAG, "=======> current selected days: "
              + Alarms.RepeatWeekdays.toString(mCode));
    }

    public void onClick(DialogInterface dialog, int which) {
        switch(which) {
        case DialogInterface.BUTTON_POSITIVE:
            setPreferenceValue(mCode);
            dialog.dismiss();
            break;
        }
    }
}
