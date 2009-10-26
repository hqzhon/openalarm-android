/**
 * @file   TextViewPreference.java
 * @author yenliangl <yenliangl@gmail.com>
 * @date   Thu Oct  8 19:49:43 2009
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
import android.widget.TextView;

public class TextViewPreference extends Preference {
    private static final String TAG = "TextViewPreference";
    private String mValue;

    protected TextViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.alarm_text_view_preference_widget);
    }

    public void setPreferenceValue(String value) {
        mValue = value;
        if(shouldPersist()) {
            persistString(mValue);
            notifyChanged();
        }
    }

    public String getPreferenceValue() {
        return mValue;
    }

    public Dialog getDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        onPrepareDialogBuilder(builder);
        return builder.create();
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        throw new IllegalArgumentException("onPrepareDialogBuilder() must be defined");
    }

    protected String formatValue(String value) {
        return value;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        displayValueOnView(view);
    }

    protected void displayValueOnView(View view) {
        final TextView textView = (TextView)view.findViewById(R.id.text);
        textView.setText(formatValue(mValue));
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defValue) {
        if(restorePersistedValue &&
           shouldPersist()) {
            setPreferenceValue(getPersistedString(mValue));
            return;
        }
        setPreferenceValue((String)defValue);
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
            setPreferenceValue(myState.value);
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
