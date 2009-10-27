
package org.startsmall.alarmclockplus.preference;

import org.startsmall.alarmclockplus.Alarms;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.os.Parcel;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.Calendar;
import java.text.DateFormatSymbols;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class AlarmRepeatOnPreference extends Preference
    implements DialogInterface.OnMultiChoiceClickListener,
               DialogInterface.OnClickListener {
    private static final String TAG = "AlarmRepeatOnDialogPreference";
    private int mCode;
    private int mWhichDaysChecked;

    public AlarmRepeatOnPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPreferenceValue(int daysCode) {
        mCode = daysCode;
        if(shouldPersist()) {
            persistInt(mCode);
            notifyChanged();
        }
    }

    public int getPreferenceValue() {
        return mCode;
    }

    public Dialog getDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        onPrepareDialogBuilder(builder);
        mWhichDaysChecked = mCode;
        return builder.create();
    }

    @Override
    protected void onBindView(View view) {
        setSummary(Alarms.RepeatWeekdays.toString(mCode));
        super.onBindView(view);
    }

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
        mWhichDaysChecked = Alarms.RepeatWeekdays.set(mWhichDaysChecked,
                                                      which+1,
                                                      isChecked);
    }

    public void onClick(DialogInterface dialog, int which) {
        switch(which) {
        case DialogInterface.BUTTON_POSITIVE:
            mCode = mWhichDaysChecked;
            setPreferenceValue(mCode);
            break;
        case DialogInterface.BUTTON_NEGATIVE:
            break;
        }
        dialog.dismiss();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, -1);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defValue) {
        if(restorePersistedValue &&
           shouldPersist()) {
            setPreferenceValue(getPersistedInt(mCode));
            return;
        }
        setPreferenceValue((Integer)defValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if(isPersistent()) {    // persistent preference
            return superState;
        }

        SavedState myState = new SavedState(superState);
        myState.code = mCode;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state != null && state.getClass().equals(SavedState.class)) {
            SavedState myState = (SavedState)state;
            super.onRestoreInstanceState(myState.getSuperState());
            setPreferenceValue(myState.code);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState extends BaseSavedState {
        int code;

        public SavedState(Parcelable in) {
            super(in);
        }

        public SavedState(Parcel in) {
            super(in);
            code = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(code);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
            new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
