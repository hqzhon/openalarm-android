/**
 * @file   AlarmLabelPreference.java
 * @author  <josh@alchip.com>
 * @date   Wed Oct  7 17:13:07 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.alarmclockplus;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Parcel;
import android.preference.Preference;
import android.view.View;
import android.view.View.BaseSavedState;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class AlarmLabelPreference extends TextViewPreference {
    private static final String TAG = "AlarmLabelPreference";

    private String mLabel;

    public AlarmLabelPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void persistValue(Object value) {
        mLabel = (String)value;
        persistString(mLabel);
    }

    @Override
    protected Object getPersistedValue() {
        return mLabel;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        final TextView textView = (TextView)view.findViewById(R.id.text);
        textView.setText(mLabel);
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
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defValue) {
        setPreferenceValue(
            restorePersistedValue ? getPersistedString(mLabel) :
            (String)defValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        if(isPersistent()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.label = mLabel;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Not the state we saved. Leave it to super.
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState)state;
        super.onRestoreInstanceState(myState.getSuperState());
        setPreferenceValue(myState.label);
    }

    private static class SavedState extends BaseSavedState {
        String label;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            label = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(label);
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
