/**
 * @file   AlarmActionPreference.java
 * @author Josh Liu <yenliangl@gmail.com>
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
// import android.preference.Preference;
import android.view.View;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class AlarmActionPreference extends TextViewPreference {
    private static final String TAG = "AlarmActionPreference";

    private int mCheckedActionEntryIndex = -1;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    public AlarmActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEntries(CharSequence[] entries) {
        mEntries = entries;
    }

    public void setEntryValues(CharSequence[] values) {
        mEntryValues = values;
    }

    public CharSequence[] getEntries() {
        return mEntries;
    }

    public CharSequence getEntry() {
        return mEntries[mCheckedActionEntryIndex];
    }

    public CharSequence getValue() {
        return mEntryValues[mCheckedActionEntryIndex];
    }

    @Override
    protected void persistValue(Object value) {
        mCheckedActionEntryIndex = (Integer)value;
        persistInt(mCheckedActionEntryIndex);
    }

    @Override
    protected Object getPersistedValue() {
        return mCheckedActionEntryIndex;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        if(mEntries.length > 0 &&
           mCheckedActionEntryIndex != -1) { // before showDialog()
            final TextView textView =
                (TextView)view.findViewById(R.id.text);
            textView.setText(mEntries[mCheckedActionEntryIndex].toString());
        }
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, -1);
    }

    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defValue) {
        setPreferenceValue(restorePersistedValue ?
                           getPersistedInt(mCheckedActionEntryIndex) :
                           (Integer)defValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if(isPersistent()) {    // persistent preference
            return superState;
        }

        SavedState myState = new SavedState(superState);
        myState.checkedActionEntryIndex = mCheckedActionEntryIndex;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state != null && state.getClass().equals(SavedState.class)) {
            SavedState myState = (SavedState)state;
            super.onRestoreInstanceState(myState.getSuperState());
            setPreferenceValue(myState.checkedActionEntryIndex);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState extends BaseSavedState {
        int checkedActionEntryIndex;

        public SavedState(Parcelable in) {
            super(in);
        }

        public SavedState(Parcel in) {
            super(in);
            checkedActionEntryIndex = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(checkedActionEntryIndex);
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
