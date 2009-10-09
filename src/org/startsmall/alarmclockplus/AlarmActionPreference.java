/**
 * @file   AlarmActionPreference.java
 * @author Josh Liu <yenliangl@gmail.com>
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
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.LayoutInflater;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class AlarmActionPreference extends TextViewPreference {
    private static final String TAG = "AlarmActionPreference";

    private AlertDialog.Builder mBuilder;
    private Dialog mDialog;
    private int mCheckedActionEntryIndex;
    private CharSequence[] mEntries;

    public AlarmActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEntries(CharSequence[] entries) {
        mEntries = entries;
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

        if(mDialog != null && mEntries.length > 0) { // before showDialog()
            final TextView textView =
                (TextView)view.findViewById(R.id.text);
            textView.setText(mEntries[mCheckedActionEntryIndex].toString());
        }
    }

    @Override
    protected void onClick() {
        Context context = getContext();

        mBuilder =
            new AlertDialog.Builder(context)
            .setTitle(R.string.alarm_settings_action_dialog_title)
            // .setPositiveButton(R.string.ok, this)
            // .setNegativeButton(R.string.cancel, this)
            .setSingleChoiceItems(
                mEntries,
                mCheckedActionEntryIndex,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        setPreferenceValue(which);
                        dialog.dismiss();
                    }
                });

        mDialog = mBuilder.create();
        mDialog.show();
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
