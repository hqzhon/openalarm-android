/**
 * @file   AlarmActionPreference.java
 * @author Josh Liu <yenliangl@gmail.com>
 * @date   Wed Oct  7 17:13:07 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.alarmclockplus.preference;

import org.startsmall.alarmclockplus.*;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

public class AlarmActionPreference extends TextViewPreference {
    public interface OnSelectActionListener {
        void onSelectAction(String handlerClassName);
    }

    private static final String TAG = "AlarmActionPreference";

    private int mCheckedActionEntryIndex = -1;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private OnSelectActionListener mOnSelectActionListener;
    private DialogInterface.OnClickListener mOnClickListener =
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                AlarmActionPreference.this.setPreferenceValueIndex(which);
                if(AlarmActionPreference.this.mOnSelectActionListener != null) {
                    mOnSelectActionListener.onSelectAction(
                        AlarmActionPreference.this.getPreferenceValue());
                }
                dialog.dismiss();
            }
        };

    public AlarmActionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        loadHandlers();
    }

    public CharSequence[] getEntries() {
        return mEntries;
    }

    public CharSequence[] getEntryValues() {
        return mEntryValues;
    }

    @Override
    public void setPreferenceValue(String value) {
        int index = findIndexOfValue(value);

        Log.d(TAG, "==================> found " + value + " + at index " + index);

        if(index != -1) {
            mCheckedActionEntryIndex = index;
            super.setPreferenceValue(value);
        }
    }

    public final void setPreferenceValueIndex(int index) {
        mCheckedActionEntryIndex = index;
        setPreferenceValue(mEntryValues[index].toString());
    }

    public int findIndexOfValue(String value) {
        if(mEntryValues != null) {
            // Just search from start to the end.
            for(int i = 0; i < mEntryValues.length; i++) {
                if(value.equals(mEntryValues[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void setOnSelectActionListener(OnSelectActionListener listener) {
        mOnSelectActionListener = listener;
    }

    protected String formatDisplayValue(String value) {
        if(mCheckedActionEntryIndex < 0)  {
            return value;
        }

        Log.d(TAG, "===> before format" + value);
        Log.d(TAG, "===> after format" + mEntries[mCheckedActionEntryIndex].toString());

        return mEntries[mCheckedActionEntryIndex].toString();
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder
            .setTitle(R.string.alarm_settings_action_dialog_title)
            .setSingleChoiceItems(
                mEntries,
                mCheckedActionEntryIndex,
                mOnClickListener);
    }

    private void loadHandlers() {
        Intent queryIntent = new Intent(Alarms.ALARM_ACTION);
        queryIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);

        PackageManager pm = getContext().getPackageManager();
        List<ResolveInfo> actions =
            pm.queryBroadcastReceivers(queryIntent, 0);

        mEntries = new CharSequence[actions.size()];
        mEntryValues = new CharSequence[actions.size()];
        for(int i = 0; i < actions.size(); i++) {
            ResolveInfo resInfo = actions.get(i);
            ActivityInfo info = actions.get(i).activityInfo;
            mEntries[i] = info.loadLabel(pm);
            mEntryValues[i] = info.name;
        }
    }
}
