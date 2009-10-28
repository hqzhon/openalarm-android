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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AlarmActionPreference extends ListPreference {
    public interface OnSelectActionListener {
        void onSelectAction(String handlerClassName);
    }

    private OnSelectActionListener mOnSelectActionListener;
    private DialogInterface.OnClickListener mOnClickListener =
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                AlarmActionPreference.this.setPreferenceValueIndex(which);
                if(mOnSelectActionListener != null) {
                    mOnSelectActionListener.onSelectAction(
                        (String)AlarmActionPreference.this.getPreferenceValue());
                }
                dialog.dismiss();
            }
        };

    public AlarmActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnSelectActionListener(OnSelectActionListener listener) {
        mOnSelectActionListener = listener;
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
        Intent queryIntent = new Intent(Alarms.ALARM_ACTION);
        queryIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);

        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> actions =
            pm.queryBroadcastReceivers(queryIntent, 0);

        final int numberOfHandlers = actions.size();
        entries.ensureCapacity(numberOfHandlers);
        entryValues.ensureCapacity(numberOfHandlers);
        for(int i = 0; i < numberOfHandlers; i++) {
            ActivityInfo info = actions.get(i).activityInfo;

            Log.d(getTag(), "=======> wawa " + info.toString()
                  + ", package=" + info.packageName
                  + ", name=" + info.name
                );


            entries.add(info.loadLabel(pm));
            entryValues.add(info.name);
        }
    }
}
