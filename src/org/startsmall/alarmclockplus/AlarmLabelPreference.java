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
import android.os.Parcelable;
import android.preference.Preference;
import android.view.View;
import android.view.LayoutInflater;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import android.widget.EditText;

public class AlarmLabelPreference extends TextViewPreference implements DialogInterface.OnClickListener {
    private static final String TAG = "AlarmLabelPreference";

    private AlertDialog.Builder mBuilder;

    public AlarmLabelPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void persistValue(Object value) {
        persistString((String)value);
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

        Log.d(TAG, "onBindView(view)");

        final TextView textView =
            (TextView)view.findViewById(R.id.text);

        String text = getPersistedString("Alarm");
        textView.setText(text);
    }

    @Override
    protected void onClick() {
        String label = getPersistedString("Alarm");
        showDialog();
    }

    private void showDialog() {
        Context context = getContext();

        String label = getPersistedString("Alarm");

        mBuilder = new AlertDialog.Builder(context)
                   .setTitle(R.string.alarm_settings_input_label_dialog_title)
                   .setPositiveButton(R.string.ok, this)
                   .setNegativeButton(R.string.cancel, this);

        LayoutInflater inflater =
            (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View contentView =
            inflater.inflate(R.layout.text_input_dialog_widget, null);
        mBuilder.setView(contentView);

        mBuilder.create().show();
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
        return a.getString(index); // default time is 08:15
    }

    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defValue) {
        if(restorePersistedValue) {
            // Restore
        } else {
            setPreferenceValue(defValue);
        }
    }

    // protected Parcelable onSaveInstanceState() {

    // }

    // protected void onRestoreInstanceState(Parcelable state) {

    // }
}
