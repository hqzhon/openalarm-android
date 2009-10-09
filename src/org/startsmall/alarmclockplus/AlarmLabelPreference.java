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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Parcel;
import android.preference.Preference;
import android.view.View;
import android.view.LayoutInflater;
import android.view.View.BaseSavedState;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import android.widget.EditText;

public class AlarmLabelPreference extends TextViewPreference implements DialogInterface.OnClickListener {
    private static final String TAG = "AlarmLabelPreference";

    private AlertDialog.Builder mBuilder;
    private String mLabel;
    private Dialog mDialog;

    public AlarmLabelPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void persistValue(Object value) {
        mLabel = (String)value;
        persistString(mLabel);
    }

    @Override
    public void onClick(DialogInterface d, int which) {
        Dialog dialog = (Dialog)d;
        switch(which) {
        case DialogInterface.BUTTON_POSITIVE:
            EditText editText = (EditText)dialog.findViewById(R.id.input);
            setPreferenceValue(editText.getText().toString());
            break;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        final TextView textView = (TextView)view.findViewById(R.id.text);
        textView.setText(mLabel);
    }

    @Override
    protected void onClick() {
        showDialog(null);
    }

    private void showDialog(Bundle bundle) {
        Context context = getContext();

        mBuilder =
            new AlertDialog.Builder(context)
            .setTitle(R.string.alarm_settings_input_label_dialog_title)
            .setPositiveButton(R.string.ok, this)
            .setNegativeButton(R.string.cancel, this);

        LayoutInflater inflater =
            (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View contentView =
            inflater.inflate(R.layout.text_input_dialog_widget, null);
        mDialog = mBuilder.setView(contentView).create();
        if(bundle != null) {
            mDialog.onRestoreInstanceState(bundle);
        } else {
            ((EditText)contentView).setText(mLabel);
        }
        mDialog.show();
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
        if(mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.label = mLabel;
        myState.isDialogShowing = mDialog.isShowing() ? 1 : 0;
        myState.dialogBundle = mDialog.onSaveInstanceState();
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

        if(myState.isDialogShowing == 1) {
            showDialog(myState.dialogBundle);
        }
    }

    private static class SavedState extends BaseSavedState {
        String label;
        int isDialogShowing;
        Bundle dialogBundle;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            label = source.readString();
            isDialogShowing = source.readInt();
            dialogBundle = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(label);
            dest.writeInt(isDialogShowing);
            dest.writeBundle(dialogBundle);
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
