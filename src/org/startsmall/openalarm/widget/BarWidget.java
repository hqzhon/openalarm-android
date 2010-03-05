package org.startsmall.openalarm;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

class BarWidget extends LinearLayout
                implements View.OnFocusChangeListener {
    private static final String TAG = "BarWidget";
    private int mSelectedChild = 0;
    private OnChildSelectionChangedListener mOnChildSelectionChangedListener;

    public BarWidget(Context context) {
        this(context, null);
    }

    public BarWidget(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.tabWidgetStyle);
    }

    public BarWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);

        init();
    }

    public void setOnChildSelectionChangedListener(OnChildSelectionChangedListener listner) {
        mOnChildSelectionChangedListener = listner;
    }

    @Override
    public void addView(View child) {
        if (child.getLayoutParams() == null) {
            final LinearLayout.LayoutParams lp =
                new LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 0);
            child.setLayoutParams(lp);
        }

        // Ensure you can navigate to the tab with the keyboard, and you can touch it
        child.setFocusable(true);
        child.setClickable(true);

        super.addView(child);

        // TODO: detect this via geometry with a tabwidget listener rather
        // than potentially interfere with the view's listener
        child.setOnClickListener(new OnChildClickListener(getChildCount() - 1));
        child.setOnFocusChangeListener(this);
    }

    public void onFocusChange(View v, boolean hasFocus) {

        Log.d(TAG, "===> onFocusChange(): v=" + v +", hasFocus=" + hasFocus);

        if (v == this && hasFocus) {
            getChildAt(mSelectedChild).requestFocus();
            return;
        }

        if (hasFocus) {
            int i = 0;
            int numChildren = getChildCount();
            while (i < numChildren) {
                if (getChildAt(i) == v) {
                    setCurrentChild(i);
                    mOnChildSelectionChangedListener.onChildSelectionChanged(i, false);
                    break;
                }
                i++;
            }
        }
    }

    private void init() {
        setFocusable(true);
        setOnFocusChangeListener(this);
    }

    private void setCurrentChild(int index) {
        if (index < 0 || index >= getChildCount()) {
            return;
        }

        getChildAt(mSelectedChild).setSelected(false);
        mSelectedChild = index;
        getChildAt(mSelectedChild).setSelected(true);
    }

    private void focusChild(int index) {
        int oldChildView = mSelectedChild;

        // Selected new child at index.
        setCurrentChild(index);

        // Change focus to new child view.
        if (oldChildView != index) {
            getChildAt(index).requestFocus();
        }
    }

    private class OnChildClickListener implements OnClickListener {
        private final int mIndex;
        private OnChildClickListener(int childIndex) {
            mIndex = childIndex;
        }

        public void onClick(View v) {

            focusChild(mIndex);

            if (mOnChildSelectionChangedListener != null) {
                mOnChildSelectionChangedListener.onChildSelectionChanged(mIndex, true);
            }
        }
    }

    static interface OnChildSelectionChangedListener {
        void onChildSelectionChanged(int childIndex, boolean click);
    }
}
