/**
 *  OpenAlarm - an extensible alarm for Android
 *  Copyright (C) 2010 Liu Yen-Liang (Josh)
 *
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @file   AlarmLabelPreference.java
 * @author yenliangl@gmail.com
 * @date   Wed Oct  7 17:13:07 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.openalarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;

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
        String label = (String)getPreferenceValue();
        View contentView =
            inflater.inflate(R.layout.text_input_dialog_widget, null);
        ((EditText)contentView).setText(label);
        builder.setView(contentView);
    }
}
