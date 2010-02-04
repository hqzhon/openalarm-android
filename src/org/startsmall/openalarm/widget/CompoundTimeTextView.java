/**
 * @file   CompoundTimeTextView.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Fri Nov 27 11:32:13 2009
 *
 * @brief
 *
 *
 */

package org.startsmall.openalarm;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

public class CompoundTimeTextView extends LinearLayout {
    public static final int TIME_TEXT = 0;
    public static final int AMPM_TEXT = 1;

    private final TextView mTimeTextView;
    private final TextView mAmTextView;
    private final TextView mPmTextView;

    public CompoundTimeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(0);       // horizontal

        // Time text view;
        mTimeTextView = new TextView(context);
        mTimeTextView.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
        mTimeTextView.setTextAppearance(context, android.R.style.TextAppearance_Large);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                               LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        this.addView(mTimeTextView, params);

        LayoutInflater inflater =
            (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View amPmView = inflater.inflate(R.layout.am_pm_widget, this, false);
        addView(amPmView);

        mAmTextView = (TextView)findViewById(R.id.am);
        mAmTextView.setText(DateUtils.getAMPMString(Calendar.AM).toUpperCase());
        mPmTextView = (TextView)findViewById(R.id.pm);
        mPmTextView.setText(DateUtils.getAMPMString(Calendar.PM).toUpperCase());
    }

    public void setTextAppearance(Context context, int id, int style) {
        switch (id) {
        case TIME_TEXT:
            mTimeTextView.setTextAppearance(context, style);
            break;
        case AMPM_TEXT:
            mAmTextView.setTextAppearance(context, style);
            mPmTextView.setTextAppearance(context, style);
            break;
        }
    }

    public void setTime(final int hourOfDay, final int minutes) {
        boolean is24HourFormat = DateFormat.is24HourFormat(getContext());

        if (is24HourFormat) {
            mAmTextView.setVisibility(View.GONE);
            mPmTextView.setVisibility(View.GONE);
        } else {
            final int time = hourOfDay * 100 + minutes;

            if (time >= 1200) {
                mAmTextView.setVisibility(View.INVISIBLE);
                mPmTextView.setVisibility(View.VISIBLE);
            } else {
                mAmTextView.setVisibility(View.VISIBLE);
                mPmTextView.setVisibility(View.INVISIBLE);
            }
        }

        mTimeTextView.setText(formatTime(hourOfDay, minutes, is24HourFormat));
    }

    private String formatTime(final int hourOfDay, final int minutes,
                              final boolean is24HourMode) {
        Calendar calendar = Alarms.getCalendarInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);

        if (is24HourMode) {
            return Alarms.formatTime("HH:mm", calendar);
        }
        return Alarms.formatTime("hh:mm", calendar);
    }
}
