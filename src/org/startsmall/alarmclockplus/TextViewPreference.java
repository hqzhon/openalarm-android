/**
 * @file   TextViewPreference.java
 * @author yenliangl <josh@alchip.com>
 * @date   Thu Oct  8 19:49:43 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.alarmclockplus;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;

public abstract class TextViewPreference extends Preference {
    private static final String TAG = "TextViewPreference";

    public TextViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.alarm_text_view_preference_widget);
    }

    public void setPreferenceValue(Object value) {
        persistValue(value);
        notifyChanged();
    }

    protected abstract void persistValue(Object value);

    // @Override
    // public void onClick(DialogInterface d, int which) {
    //     Dialog dialog = (Dialog)d;
    //     switch(which) {
    //     case DialogInterface.BUTTON_POSITIVE:
    //         EditText editText = (EditText)dialog.findViewById(R.id.input);
    //         persistString(editText.getText().toString());
    //         notifyChanged();
    //         break;
    //     }
    // }

    // @Override
    // protected void onBindView(View view) {
    //     super.onBindView(view);

    //     Log.d(TAG, "onBindView(view)");

    //     final TextView textView =
    //         (TextView)view.findViewById(R.id.text);

    //     String text = getPersistedString("Alarm");
    //     textView.setText(text);
    // }

    /**
     * Bring up a TimePicker dialog.
     *
     */
    // @Override
    // protected void onClick() {
    //     String label = getPersistedString("Alarm");
    //     showDialog();
    // }

    // private void showDialog() {
    //     Context context = getContext();

    //     String label = getPersistedString("Alarm");

    //     mBuilder = new AlertDialog.Builder(context)
    //                .setTitle("Enter label for Alarm " + label)
    //                .setPositiveButton("Ok", this)
    //                .setNegativeButton("Cancel", this);

    //     LayoutInflater inflater =
    //         (LayoutInflater)context.getSystemService(
    //             Context.LAYOUT_INFLATER_SERVICE);
    //     View contentView =
    //         inflater.inflate(R.layout.text_input_dialog_widget, null);
    //     mBuilder.setView(contentView);

    //     mBuilder.create().show();
    // }

    /**
     * Return default value of this Preference
     *
     * @param a
     * @param index
     *
     * @return
     */
    // protected Object onGetDefaultValue(TypedArray a, int index) {
    //     return a.getString(index); // default time is 08:15
    // }

    // protected void onSetInitialValue(boolean restorePersistedValue,
    //                                  Object defValue) {
    //     if(restorePersistedValue) {

    //         // Restore


    //     } else {
    //         persistString((String)defValue);
    //         notifyChanged();
    //     }
    // }

    // protected Parcelable onSaveInstanceState() {

    // }

    // protected void onRestoreInstanceState(Parcelable state) {

    // }
}
