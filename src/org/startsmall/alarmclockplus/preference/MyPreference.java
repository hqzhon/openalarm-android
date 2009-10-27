/**
 * @file   MyPreference.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Tue Oct 27 17:00:52 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.alarmclockplus.preference;

import org.startsmall.alarmclockplus.*;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public abstract class MyPreference extends Preference {
    private String mTag;
    private String mValue;

    protected MyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTag = getClass().getSimpleName();
    }

    public void setPreferenceValue(Object obj) {
        mValue = toPreferenceValue(obj);
        persistPreferenceValue(mValue);
    }

    public Object getPreferenceValue() {
        return parsePreferenceValue(mValue);
    }

    public Dialog getDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        onPrepareDialogBuilder(builder);
        return builder.create();
    }

    protected abstract Object parsePreferenceValue(String value);
    protected abstract String toPreferenceValue(Object obj);

    protected String getTag() {
        return mTag;
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        throw new IllegalArgumentException("onPrepareDialogBuilder() must be defined");
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        Log.d(mTag,
              "====> onBindView(this=" + this + ", view=" + view + ")");
    }

    private void persistPreferenceValue(String value) {
        if(shouldPersist()) {
            persistString(value);
            notifyChanged();
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defValue) {
        persistPreferenceValue(
            restorePersistedValue ?
            getPersistedString(mValue) : (String)defValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if(isPersistent()) {    // persistent preference
            return superState;
        }

        SavedState myState = new SavedState(superState);
        myState.value = mValue;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if(state != null && state.getClass().equals(SavedState.class)) {
            SavedState myState = (SavedState)state;
            super.onRestoreInstanceState(myState.getSuperState());
            persistPreferenceValue(myState.value);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState extends BaseSavedState {
        String value;

        public SavedState(Parcelable in) {
            super(in);
        }

        public SavedState(Parcel in) {
            super(in);
            value = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(value);
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
