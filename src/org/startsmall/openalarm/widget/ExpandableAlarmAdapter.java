package org.startsmall.openalarm;

import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.util.SparseArray;
import android.widget.BaseExpandableListAdapter;

import java.util.*;

abstract class ExpandableAlarmAdapter extends BaseExpandableListAdapter {
    public static final String GROUP_DATA_KEY_LABEL = "label";
    public static final String GROUP_DATA_KEY_HANDLER = "handler";
    public static final String GROUP_DATA_KEY_ICON = "icon";

    private static final String TAG = "ExpandableAlarmAdapter";
    private Context mContext;
    private List<? extends Map<String, ?>> mGroupData;
    private int mGroupLayout;
    private String[] mGroupFrom;
    private int[] mGroupTo;
    private SparseArray<CursorHelper> mChildCursorHelpers = new SparseArray<CursorHelper>();
    private Handler mHandler;

    public ExpandableAlarmAdapter(Context context,
                                  List<? extends Map<String, ?>> groupData, String[] groupFrom, int[] groupTo) {
        mContext = context;
        mGroupData = groupData;
        mGroupFrom = groupFrom;
        mGroupTo = groupTo;
        mHandler = new Handler();
    }

    public boolean hasStableIds() {
        return true;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public Object getGroup(int groupPosition) {
        return mGroupData.get(groupPosition);
    }

    public int getGroupCount() {
        return mGroupData.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = newGroupView(parent);
        } else {
            v = convertView;
        }

        Map<String, ?> groupData = mGroupData.get(groupPosition);
        bindGroupView(v, groupData, getChildrenCount(groupPosition), mGroupFrom, mGroupTo);
        return v;
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        deactivateChildCursorHelper(groupPosition);
    }

    protected abstract View newGroupView(ViewGroup parent);
    protected abstract void bindGroupView(View view, Map<String, ?> data, int childrenCount, String[] from, int[] to);

    public Cursor getChild(int groupPosition, int childPosition) {
        return getChildCursorHelper(groupPosition, true).moveTo(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return getChildCursorHelper(groupPosition, true).getId(childPosition);
    }

    public int getChildrenCount(int groupPosition) {
        return getChildCursorHelper(groupPosition, true).getCount();
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        Cursor childCursor = getChild(groupPosition, childPosition);
        View v = convertView;
        if (convertView == null) {
            v = newChildView(mContext, childCursor, isLastChild, parent);
        }
        bindChildView(v, mContext, childCursor, isLastChild);
        return v;
    }

    protected abstract View newChildView(Context context, Cursor childCursor, boolean isLastChild, ViewGroup parent);
    protected abstract void bindChildView(View v, Context context, Cursor childCursor, boolean isLastChild);

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        super.notifyDataSetInvalidated();
    }

    /**
     * Deactivate all children cursors.
     *
     */
    public void deactivateChildCursors() {
        Log.d(TAG, "===> deactivateChildCursors()");

        final int len = mChildCursorHelpers.size();
        for (int i = 0; i < len; i++) {
            CursorHelper cursorHelper = getChildCursorHelper(i, false);
            cursorHelper.deactivate();
        }
        mChildCursorHelpers.clear();
    }

    private void deactivateChildCursorHelper(int groupPosition) {
        CursorHelper cursorHelper = getChildCursorHelper(groupPosition, false);
        cursorHelper.deactivate();
        mChildCursorHelpers.remove(groupPosition);
    }

    private CursorHelper getChildCursorHelper(int groupPosition, boolean createCursorOnDemand) {
        CursorHelper cursorHelper = mChildCursorHelpers.get(groupPosition);
        if (cursorHelper == null && createCursorOnDemand) {
            cursorHelper = new CursorHelper(getChildCursor(groupPosition));
            mChildCursorHelpers.put(groupPosition, cursorHelper);
        }
        return cursorHelper;
    }

    private void notifyDataSetChanged(boolean deactivateCursors) {
        if (deactivateCursors) {
            deactivateChildCursors();
        }
        notifyDataSetChanged();
    }

    private Cursor getChildCursor(int groupPosition) {
        ContentResolver cr = mContext.getContentResolver();

        Map<String, ?> data = mGroupData.get(groupPosition);
        String handler = (String)data.get(AlarmColumns.HANDLER);

        Cursor cursor =
            cr.query(Alarms.getAlarmUri(-1),
                     AlarmColumns.QUERY_COLUMNS,
                     AlarmColumns.HANDLER + "=?",
                     new String[]{handler},
                     AlarmColumns.DEFAULT_SORT_ORDER);
        return cursor;
    }

    private class CursorHelper {
        private Cursor mCursor;
        private boolean mIsDataValid;
        private MyContentObserver mContentObserver;
        private MyDataSetObserver mDataSetObserver;

        CursorHelper(Cursor cursor) {
            mCursor = cursor;
            mContentObserver = new MyContentObserver();
            mDataSetObserver = new MyDataSetObserver();

            if (mCursor != null && !mCursor.isClosed()) {
                mCursor.registerDataSetObserver(mDataSetObserver);
                mCursor.registerContentObserver(mContentObserver);
                mIsDataValid = true;
            }
        }

        Cursor getCursor() {
            return mCursor;
        }

        Cursor moveTo(int position) {
            if (mCursor != null && mIsDataValid && mCursor.moveToPosition(position)) {
                return mCursor;
            }
            return null;
        }

        long getId(int position) {
            if (mCursor != null && mIsDataValid) {
                int idIndex = mCursor.getColumnIndexOrThrow(AlarmColumns._ID);
                mCursor.moveToPosition(position);
                return mCursor.getLong(idIndex);
            }
            return 0;
        }

        void deactivate() {
            if (mCursor != null && mIsDataValid) {
                mCursor.unregisterContentObserver(mContentObserver);
                mCursor.unregisterDataSetObserver(mDataSetObserver);
                mCursor.deactivate();
                mIsDataValid = false;
                // Log.d(TAG, "===> deactivate(): deactivated, mIsDataValid=" + mIsDataValid);
            }
        }

        void activate() {
            if (mCursor != null && !mIsDataValid) {
                mCursor.registerContentObserver(mContentObserver);
                mCursor.registerDataSetObserver(mDataSetObserver);
                mIsDataValid = mCursor.requery();
                // Log.d(TAG, "===> activate(): activated, mIsDataValid=" + mIsDataValid);
            }
        }

        void close() {
            if (mCursor != null && !mCursor.isClosed()) {
                // Log.d(TAG, "===> close(): mIsDataValid=" + mIsDataValid);
                if (mIsDataValid) {
                    mCursor.unregisterContentObserver(mContentObserver);
                    mCursor.unregisterDataSetObserver(mDataSetObserver);
                }
                mCursor.close();
                mIsDataValid = false;
                mCursor = null;
            }
        }

        int getCount() {
            // Log.d(TAG, "===> before getCount(): mCursor=" + mCursor + ", mIsDataValid=" + mIsDataValid);
            if (mCursor != null) {
                return mCursor.getCount();
            }
            return 0;
        }

        private class MyContentObserver extends ContentObserver {
            public MyContentObserver() {
                super(mHandler);
            }

            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            /**
             * This method gets called when an alarm is inserted,
             * updated and deleted. We should do an auto-requery
             * when these situations happens because we need an
             * group updates its child views immediately if it is
             * expanded already.
             *
             * @param selfChange
             */
            public void onChange(boolean selfChange) {
                if (mCursor != null) {
                    // Cause MyDataSetObserver.onChange() to be
                    // called.
                    mIsDataValid = mCursor.requery();

                    // Log.d(TAG, "===> MyContentObserver.onChange(): mIsDataValid=" + mIsDataValid);

                    // Notify outside that something has been
                    // changed.
                    notifyDataSetChanged();
                }
            }
        }

        private class MyDataSetObserver extends DataSetObserver {
            /**
             * Trigger by Cursor.requery()
             *
             */
            @Override
            public void onChanged() {
                // Log.d(TAG, "===> MyDataSetObserver.onChanged()");
                notifyDataSetChanged(false);
            }

            /**
             * Triggered by Cursor.deactivate() or Cursor.close()
             *
             */
            @Override
            public void onInvalidated() {
                mIsDataValid = false;
                // Log.d(TAG, "===> MyDataSetObserver.onInvalidated()" + mIsDataValid);
                notifyDataSetInvalidated();
            }
        }

    }
}
