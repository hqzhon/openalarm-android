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

public class TimeAmPmView extends LinearLayout {
    public static final int TIME_TEXT = 0;
    public static final int AMPM_TEXT = 1;

    private final TextView mTimeTextView;
    private final TextView mAmTextView;
    private final TextView mPmTextView;

    public TimeAmPmView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(0);       // horizontal

        LayoutInflater.from(context).inflate(R.layout.time_am_pm, this, true);

        // Time text view;
        mTimeTextView = (TextView)findViewById(R.id.time);
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
        if (Alarms.is24HourMode) {
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

        mTimeTextView.setText(formatTime(hourOfDay, minutes));
    }

    private String formatTime(final int hourOfDay, final int minutes) {
        Calendar calendar = Alarms.getCalendarInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minutes);

        if (Alarms.is24HourMode) {
            return Alarms.formatTime("HH:mm", calendar);
        }
        return Alarms.formatTime("hh:mm", calendar);
    }
}
