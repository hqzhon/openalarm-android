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

public class AlarmActionPreference extends Preference implements DialogInterface.OnClickListener {
    private static final String TAG = "AlarmActionPreference";

    private AlertDialog.Builder mBuilder;
    private Dialog mDialog;
    private String[] mEntries;
    private String[] mEntryValues;

    public AlarmActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.alarm_text_view_preference_widget);
    }

    public void setEntries(String[] entries) {
        mEntries = entries;









    }

    // public void setEntryValues(String[] values) {
    //     mEntryValues = values;
    // }

    // public void setLabel(String label) {
    //     persistString(label);
    //     notifyChanged();
    // }

    public void addActionButton(RadioButton button, int id) {
        RadioGroup btnGroup =
            (RadioGroup)mDialog.findViewById(R.id.actions);

        btnGroup.addView(button, id, new ViewGroup.LayoutParams(
                             ViewGroup.LayoutParams.FILL_PARENT,
                             ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void onClick(DialogInterface d, int which) {
        Dialog dialog = (Dialog)d;
        switch(which) {
        case DialogInterface.BUTTON_POSITIVE:
            RadioGroup actionBtnGroup =
                (RadioGroup)dialog.findViewById(R.id.actions);
            int id = actionBtnGroup.getCheckedRadioButtonId();

            persistInt(id);
            notifyChanged();
            break;
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        Log.d(TAG, "onBindView(view)");

        // final TextView textView =
        //     (TextView)view.findViewById(R.id.text);

        // String text = getPersistedString("Alarm");
        // textView.setText(text);
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
                   .setTitle("Complete action using")
                   .setPositiveButton("Ok", this)
                   .setNegativeButton("Cancel", this);

        LayoutInflater inflater =
            (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View contentView =
            inflater.inflate(R.layout.action_pick_dialog_widget, null);
        mBuilder.setView(contentView);

        mDialog = mBuilder.create();
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
