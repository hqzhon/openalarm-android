/**
 * @file   AlarmLabelPreference.java
 * @author  <josh@alchip.com>
 * @date   Wed Oct  7 17:13:07 2009
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
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.os.Parcel;
import android.view.View;
import android.util.AttributeSet;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.TextView;

public class AlarmLabelPreference extends TextViewPreference {
    public AlarmLabelPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder
            .setTitle(R.string.alarm_settings_input_label_dialog_title)
            .setPositiveButton(
                android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        Dialog dialog = (Dialog)d;
                        switch(which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            EditText editText =
                                (EditText)dialog.findViewById(R.id.input);
                            AlarmLabelPreference.this.
                                setPreferenceValue(
                                    editText.getText().toString());
                            break;
                        }
                    }
                })
            .setNegativeButton(android.R.string.cancel, null);

        LayoutInflater inflater =
            (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        String label = getPreferenceValue();
        View contentView =
            inflater.inflate(R.layout.text_input_dialog_widget, null);
        ((EditText)contentView).setText(label);
        builder.setView(contentView);
    }
}
