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
    private SparseArray<CursorHelper> mChildrenCursorHelpers = new SparseArray<CursorHelper>();
    private Handler mHandler;

    public ExpandableAlarmAdapter(Context context,
                                  List<? extends Map<String, ?>> groupData, String[] groupFrom, int[] groupTo) {
        mContext = context;
        mGroupData = groupData;
        mGroupFrom = groupFrom;
        mGroupTo = groupTo;
        mHandler = new Handler();
    }

    // public void setGroupData(List<? extends Map<String, ?>> groupData) {
    //     mGroupData = groupData;
    //     // no need to requery for cursor??
    //     notifyDataSetChanged();
    // }

    public boolean hasStableIds() {
        return true;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    // Group-related
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

    protected abstract View newGroupView(ViewGroup parent);

    protected abstract void bindGroupView(View view, Map<String, ?> data, int childrenCount, String[] from, int[] to);

    /**
     * Children-releated
     *
     */
    public Cursor getChild(int groupPosition, int childPosition) {
        // Get this group's children Cursor pointing to the particular position.
        return getChildrenCursorHelper(groupPosition).moveTo(childPosition);
    }

    /**
     *
     *
     */
    public long getChildId(int groupPosition, int childPosition) {
        return getChildrenCursorHelper(groupPosition).getId(childPosition);
    }

    /**
     * Return how many children this group has. This method will
     * be executed before getChildView() to instantiate a view or
     * bind a view with data.
     *
     * @param groupPosition Position of clicked group.
     *
     * @return number of children this group has.
     */
    public int getChildrenCount(int groupPosition) {
        int count = getChildrenCursorHelper(groupPosition).getCount();
        Log.d(TAG, "===> getChildrenCount(" + groupPosition + "): " + count + " alarms");
        return getChildrenCursorHelper(groupPosition).getCount();
    }

    /**
     *
     *
     */
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
        Log.d(TAG, "===> ExpandableAlarmAdapter.notifyDataSetChanged()");

        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        Log.d(TAG, "===> ExpandableAlarmAdapter.notifyDataSetInvalidated()");

        // Call every observer's onInvalidated()
        super.notifyDataSetInvalidated();
    }

    /**
     *
     *
     * @param groupPosition
     */
    @Override
    public void onGroupCollapsed(int groupPosition) {
        // Deactivate cursor of this group to save some resources
        CursorHelper cursorHelper = mChildrenCursorHelpers.get(groupPosition);
        if (cursorHelper != null) {
            cursorHelper.deactivate();
        }
    }

    /**
     *
     *
     * @param groupPosition
     */
    @Override
    public void onGroupExpanded(int groupPosition) {
        CursorHelper cursorHelper = mChildrenCursorHelpers.get(groupPosition);
        if (cursorHelper != null) {
            cursorHelper.activate();
        }
    }

    /**
     *
     *
     * @param groupPosition
     *
     * @return
     */
    protected CursorHelper getChildrenCursorHelper(int groupPosition) {
        CursorHelper cursorHelper = mChildrenCursorHelpers.get(groupPosition);
        if (cursorHelper == null) {
            cursorHelper = new CursorHelper(getChildrenCursor(groupPosition));
            mChildrenCursorHelpers.put(groupPosition, cursorHelper);
        }
        return cursorHelper;
    }

    private void notifyDataSetChanged(boolean releaseCursors) {
        if (releaseCursors) {
            releaseCursorHelpers();
        }

        // Call every observer's onChanged();
        super.notifyDataSetChanged();
    }

    private synchronized Cursor getChildrenCursor(int groupPosition) {
        ContentResolver cr = mContext.getContentResolver();

        Map<String, ?> data = mGroupData.get(groupPosition);
        String handler = (String)data.get(AlarmColumns.HANDLER);

        Cursor cursor =
            cr.query(
                Alarms.getAlarmUri(-1),
                AlarmColumns.QUERY_COLUMNS,
                AlarmColumns.HANDLER + "=?",
                new String[]{handler},
                AlarmColumns.DEFAULT_SORT_ORDER);
        return cursor;
    }

    private void releaseCursorHelpers() {
        final int len = mChildrenCursorHelpers.size();
        for (int i = 0; i < len; i++) {
            CursorHelper cursorHelper = mChildrenCursorHelpers.get(i);
            if (cursorHelper != null) {
                cursorHelper.deactivate();
            }
        }
        // mChildrenCursorHelpers.clear();
    }

    private class CursorHelper {
        private Cursor mCursor;
        private boolean mIsValid;
        private MyContentObserver mContentObserver;
        private MyDataSetObserver mDataSetObserver;

        CursorHelper(Cursor cursor) {
            mCursor = cursor;
            mContentObserver = new MyContentObserver();
            mDataSetObserver = new MyDataSetObserver();

            if (mCursor != null) {
                mCursor.registerDataSetObserver(mDataSetObserver);
                mCursor.registerContentObserver(mContentObserver);
                mIsValid =true;
            }
        }

        Cursor getCursor() {
            return mCursor;
        }

        Cursor moveTo(int position) {
            if (mCursor != null) {
                mCursor.moveToPosition(position);
                return mCursor;
            }
            return null;
        }

        long getId(int position) {
            if (mCursor != null) {
                int idIndex = mCursor.getColumnIndexOrThrow(AlarmColumns._ID);
                mCursor.moveToPosition(position);
                return mCursor.getLong(idIndex);
            }
            return -1;
        }

        void deactivate() {
            if (mCursor != null && mIsValid) {
                Log.d(TAG, "===> deactivate() : " + this);
                mCursor.unregisterContentObserver(mContentObserver);
                mCursor.unregisterDataSetObserver(mDataSetObserver);
                mCursor.deactivate();
                mIsValid = false;
            }
        }

        void activate() {
            if (mCursor != null && !mIsValid) {
                Log.d(TAG, "===> activate() : " + this);
                mCursor.registerContentObserver(mContentObserver);
                mCursor.registerDataSetObserver(mDataSetObserver);
                mCursor.requery();
                mIsValid = true;
            }
        }

        int getCount() {
            if (mCursor != null) {
                return mCursor.getCount();
            }
            return -1;
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
                if (mCursor != null && mIsValid) {
                    mCursor.requery();
                }

                // Notify outside that something has been changed.
                notifyDataSetChanged();
            }
        }

        private class MyDataSetObserver extends DataSetObserver {
            /**
             * This method is triggered by cursor.requery() in
             * ContentObserver.onChange().
             *
             */
            @Override
            public void onChanged() {
                Log.d(TAG, "===> MyDataSetObserver.onChange(): requery()");
                notifyDataSetChanged(false);
            }

            @Override
            public void onInvalidated() {
                Log.d(TAG, "===> MyDataSetObserver.onInvalidated(): deactivate() or close()");
                // notifyDataSetInvalidated();
            }
        }

    }
}
