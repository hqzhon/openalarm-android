/**
 * @file   TimeTextView.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Fri Nov 27 11:32:13 2009
 *
 * @brief
 *
 *
 */

package org.startsmall.alarmclockplus.widget;

import org.startsmall.alarmclockplus.Alarms;
import org.startsmall.alarmclockplus.R;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

public class TimeTextView extends LinearLayout {
    private final boolean mIs24HourFormat;
    private final TextView mTimeTextView;
    private final TextView mAmTextView;
    private final TextView mPmTextView;

    public TimeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Time text view;
        mTimeTextView = new TextView(context);
        mTimeTextView.setTextAppearance(context, android.R.style.TextAppearance_Large);
        mTimeTextView.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                               LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        this.addView(mTimeTextView, params);

        LayoutInflater inflater =
            (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View amPmView = inflater.inflate(R.layout.am_pm_widget, this, false);
        addView(amPmView);

        mIs24HourFormat = DateFormat.is24HourFormat(context);
        mAmTextView = (TextView)findViewById(R.id.am);
        mPmTextView = (TextView)findViewById(R.id.pm);
    }

    public void setTime(final int hourOfDay, final int minutes) {
        String pattern;
        if (mIs24HourFormat) {
            mAmTextView.setVisibility(View.GONE);
            mPmTextView.setVisibility(View.GONE);
            pattern = "HH:mm";
        } else {
            pattern = "hh:mm";
            final int time = hourOfDay * 100 + minutes;

            if (time >= 1200) {
                mAmTextView.setVisibility(View.INVISIBLE);
                mPmTextView.setVisibility(View.VISIBLE);
            } else {
                mAmTextView.setVisibility(View.VISIBLE);
                mPmTextView.setVisibility(View.INVISIBLE);
            }
        }

        Calendar calendar = Alarms.getCalendarInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);
        mTimeTextView.setText(Alarms.formatDate(pattern, calendar));
    }
}
