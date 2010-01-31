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
import android.util.SparseArray;
import android.widget.BaseExpandableListAdapter;

import java.util.*;

abstract class ExpandableAlarmAdapter extends BaseExpandableListAdapter {
    public static final String GROUP_DATA_KEY_LABEL = "label";
    public static final String GROUP_DATA_KEY_HANDLER = "handler";
    public static final String GROUP_DATA_KEY_ICON = "icon";

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

    public void setGroupData(List<? extends Map<String, ?>> groupData) {
        mGroupData = groupData;

        // no need to requery for cursor??
        notifyDataSetChanged();
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
        bindGroupView(v, groupData, mGroupFrom, mGroupTo);
        return v;
    }

    protected abstract View newGroupView(ViewGroup parent);

    protected abstract void bindGroupView(View view, Map<String, ?> data, String[] from, int[] to);

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
     *
     *
     */
    public int getChildrenCount(int groupPosition) {
        return getChildrenCursorHelper(groupPosition).getCount();
    }

    /**
     *
     *
     */
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        Cursor childCursor =
            getChildrenCursorHelper(groupPosition).moveTo(childPosition);
        View v = convertView;
        if (convertView == null) {
            v = newChildView(mContext, childCursor, isLastChild, parent);
        }
        bindChildView(v, mContext, childCursor, isLastChild);
        return v;
    }

    protected abstract View newChildView(Context context, Cursor childCursor, boolean isLastChild,
                                         ViewGroup parent);

    // The binding might be too complex for this class to
    // implement.
    protected abstract void bindChildView(View v, Context context, Cursor childCursor, boolean isLastChild);

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        notifyDataSetChanged(true);
    }

    public void notifyDataSetChanged(boolean releaseCursors) {
        if (releaseCursors) {
            releaseCursorHelpers();
        }
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        // Cursors were invalidated, release them
        releaseCursorHelpers();
        super.notifyDataSetInvalidated();
    }

    protected CursorHelper getChildrenCursorHelper(int groupPosition) {
        CursorHelper cursorHelper = mChildrenCursorHelpers.get(groupPosition);
        if (cursorHelper == null) {
            cursorHelper = new CursorHelper(getChildrenCursor(groupPosition));
            mChildrenCursorHelpers.put(groupPosition, cursorHelper);
        }
        return cursorHelper;
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
            CursorHelper cursorHelper =
                mChildrenCursorHelpers.get(i);
            if (cursorHelper != null) {
                cursorHelper.getCursor().close();
                mChildrenCursorHelpers.remove(i);
            }
        }
    }

    class CursorHelper {
        private Cursor mCursor;
        private MyContentObserver mContentObserver;
        private MyDataSetObserver mDataSetObserver;

        CursorHelper(Cursor cursor) {
            mCursor = cursor;
            mContentObserver = new MyContentObserver();
            mDataSetObserver = new MyDataSetObserver();

            if (mCursor != null) {
                mCursor.registerDataSetObserver(mDataSetObserver);
                mCursor.registerContentObserver(mContentObserver);
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

            public void onChange(boolean selfChange) {
                notifyDataSetChanged();
            }
        }

        private class MyDataSetObserver extends DataSetObserver {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                notifyDataSetInvalidated();
            }
        }

    }

    // private class My extends BroadcastReceiver {
    //     public void onReceive(Context context, Intent intent) {




    //     }
    // }
}
