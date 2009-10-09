/**
 * @file   AlarmActionPreference.java
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

public class AlarmActionPreference extends TextViewPreference implements DialogInterface.OnClickListener {
    private static final String TAG = "AlarmActionPreference";

    private AlertDialog.Builder mBuilder;
    private Dialog mDialog;
    private String[] mEntries;
    private String[] mEntryValues;

    public AlarmActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEntries(String[] entries) {
        mEntries = entries;
    }

    @Override
    protected void persistValue(Object value) {
        persistInt((Integer)value);
        notifyChanged();
    }


    // public void setEntryValues(String[] values) {
    //     mEntryValues = values;
    // }

    @Override
    public void onClick(DialogInterface d, int which) {
        Dialog dialog = (Dialog)d;
        switch(which) {
        case DialogInterface.BUTTON_POSITIVE:
            RadioGroup actionBtnGroup =
                (RadioGroup)dialog.findViewById(R.id.actions);
            int id = actionBtnGroup.getCheckedRadioButtonId();

            Log.d(TAG, "Checked " + id);

            setPreferenceValue(id);
            break;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        if(mDialog == null) {   // before showDialog()
            return;
        }

        int checkedButtonId = getPersistedInt(-1);
        if(checkedButtonId != -1) {
            RadioGroup group = (RadioGroup)mDialog.findViewById(R.id.actions);
            RadioButton button = (RadioButton)group.getChildAt(checkedButtonId);

            final TextView textView =
                (TextView)view.findViewById(R.id.text);
            textView.setText(button.getText().toString());
        }
    }

    /**
     * Bring up a TimePicker dialog.
     *
     */
    @Override
    protected void onClick() {
        // String label = getPersistedString("Alarm");
        showDialog();
    }

    private void showDialog() {
        Context context = getContext();

        // String label = getPersistedString("Alarm");

        mBuilder = new AlertDialog.Builder(context)
                   .setTitle(R.string.alarm_settings_action_dialog_title)
                   .setPositiveButton(R.string.ok, this)
                   .setNegativeButton(R.string.cancel, this);

        LayoutInflater inflater =
            (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View contentView =
            inflater.inflate(R.layout.action_pick_dialog_widget, null);
        populateEntries(contentView);
        mBuilder.setView(contentView);

        mDialog = mBuilder.create();
        mDialog.show();
    }

    private void populateEntries(View view) {
        RadioGroup group = (RadioGroup)view.findViewById(R.id.actions);
        group.removeAllViews();

        for(int i = 0; i < mEntries.length; i++) {
            RadioButton button = new RadioButton(getContext());
            button.setText(mEntries[i]);
            group.addView(button, i,
                          new ViewGroup.LayoutParams(
                              ViewGroup.LayoutParams.FILL_PARENT,
                              ViewGroup.LayoutParams.WRAP_CONTENT));
        }
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
        return a.getInt(index, -1);
    }

    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defValue) {
        if(restorePersistedValue) {

            // Restore


        } else {
            persistInt((Integer)defValue);
            notifyChanged();
        }
    }

    // protected Parcelable onSaveInstanceState() {

    // }

    // protected void onRestoreInstanceState(Parcelable state) {

    // }
}
