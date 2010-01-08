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
 * @file   AlarmActionPreference.java
 * @author Josh Liu <yenliangl@gmail.com>
 * @date   Wed Oct  7 17:13:07 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.openalarm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.AttributeSet;
//import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class AlarmActionPreference extends ListPreference {
    public interface IOnSelectActionListener {
        void onSelectAction(String handlerClassName);
    }

    private IOnSelectActionListener mOnSelectActionListener;
    private DialogInterface.OnClickListener mOnClickListener =
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                AlarmActionPreference.this.setPreferenceValueIndex(which);
                if(mOnSelectActionListener != null) {
                    mOnSelectActionListener.onSelectAction(
                        (String)AlarmActionPreference.this.getPreferenceValue());
                }
                setSummary(null);
                dialog.dismiss();
            }
        };

    public AlarmActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnSelectActionListener(IOnSelectActionListener listener) {
        mOnSelectActionListener = listener;
    }

    @Override
    protected void onBindView(View view) {
        if(TextUtils.isEmpty((String)getPreferenceValue())) {
            setSummary("What do you want me to do?");
        }
        super.onBindView(view);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder
            .setTitle(R.string.alarm_settings_action_dialog_title)
            .setSingleChoiceItems(
                getEntries(),
                getCheckedEntryIndex(),
                mOnClickListener)
            .setNegativeButton(android.R.string.cancel, null);
    }

    @Override
    protected void generateListItems(ArrayList<CharSequence> entries,
                                     ArrayList<CharSequence> entryValues) {
        Intent queryIntent = prepareQueryHandlerIntent();

        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> actions =
            pm.queryBroadcastReceivers(queryIntent, 0);

        final int numberOfHandlers = actions.size();
        if(numberOfHandlers > 0) {
            entries.ensureCapacity(numberOfHandlers);
            entryValues.ensureCapacity(numberOfHandlers);
            for(int i = 0; i < numberOfHandlers; i++) {
                ActivityInfo info = actions.get(i).activityInfo;
                entries.add(info.loadLabel(pm));
                entryValues.add(info.name);
            }
        }
    }

    protected Intent prepareQueryHandlerIntent() {
        Intent intent = new Intent(Alarms.HANDLE_ALARM);
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        return intent;
    }
}
