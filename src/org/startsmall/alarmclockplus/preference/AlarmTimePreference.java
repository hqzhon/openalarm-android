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
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
//import android.preference.Preference;
import android.view.View;
//import android.view.View.BaseSavedState;
import android.util.AttributeSet;
//import android.util.Log;
import android.widget.TextView;

import java.util.Calendar;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;

public class AlarmTimePreference extends TextViewPreference {
    // private static final String TAG = "AlarmTimePreference";

    private int mTime;

    public AlarmTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void persistValue(Object value) {
        mTime = (Integer)value;
        persistInt(mTime);
    }

    protected Object getPersistedValue() {
        return mTime;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        final TextView textView = (TextView)view.findViewById(R.id.text);

        int hourOfDay = mTime / 100;
        int minutes = mTime % 100;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);
        textView.setText(Alarms.formatDate("HH:mm", calendar));
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 815); // default time is 08:15
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defValue) {
        setPreferenceValue(restorePersistedValue ?
                           getPersistedInt(mTime) : (Integer)defValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if(isPersistent()) {    // persistent preference
            return superState;
        }

        SavedState myState = new SavedState(superState);
        myState.time = mTime;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state != null && state.getClass().equals(SavedState.class)) {
            SavedState myState = (SavedState)state;
            super.onRestoreInstanceState(myState.getSuperState());
            setPreferenceValue(myState.time);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState extends BaseSavedState {
        int time;

        public SavedState(Parcelable in) {
            super(in);
        }

        public SavedState(Parcel in) {
            super(in);
            time = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(time);
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
