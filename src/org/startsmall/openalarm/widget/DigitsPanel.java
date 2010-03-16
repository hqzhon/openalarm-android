package org.startsmall.openalarm;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;

public class DigitsPanel extends LinearLayout {
    private static final String TAG = "DigitsPanel";
    private OnRightPasswordSetListener mRightPasswordSetListener;
    private OnDigitsClearListener mDigitsClearListener;
    private String mPassword;
    private TextView mEnteredPassword;

    public DigitsPanel(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.digits_panel, this, true);

        mEnteredPassword = (TextView)findViewById(R.id.entered);

        final Button setButton = (Button)findViewById(R.id.set);
        setButton.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    onPasswordSet();
                }
            });

        Button clearButton = (Button)findViewById(R.id.clear);
        clearButton.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    onDigitsClear();
                }
            });

        View.OnClickListener digitClickListener =
            new View.OnClickListener() {
                public void onClick(View v) {
                    onDigitClick(v);
                }
            };

        Button digit1 = (Button)findViewById(R.id.digit1);
        digit1.setOnClickListener(digitClickListener);
        Button digit2 = (Button)findViewById(R.id.digit2);
        digit2.setOnClickListener(digitClickListener);
        Button digit3 = (Button)findViewById(R.id.digit3);
        digit3.setOnClickListener(digitClickListener);
        Button digit4 = (Button)findViewById(R.id.digit4);
        digit4.setOnClickListener(digitClickListener);
        Button digit5 = (Button)findViewById(R.id.digit5);
        digit5.setOnClickListener(digitClickListener);
        Button digit6 = (Button)findViewById(R.id.digit6);
        digit6.setOnClickListener(digitClickListener);
        Button digit7 = (Button)findViewById(R.id.digit7);
        digit7.setOnClickListener(digitClickListener);
        Button digit8 = (Button)findViewById(R.id.digit8);
        digit8.setOnClickListener(digitClickListener);
        Button digit9 = (Button)findViewById(R.id.digit9);
        digit9.setOnClickListener(digitClickListener);
    }

    public void setOnRightPasswordSetListener(OnRightPasswordSetListener listener) {
        mRightPasswordSetListener = listener;
    }

    public void setOnDigitsClearListener(OnDigitsClearListener listener) {
        mDigitsClearListener = listener;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    private void onPasswordSet() {
        String userPassword = mEnteredPassword.getText().toString();
        if (userPassword.equals(mPassword) && mRightPasswordSetListener != null) {
            mRightPasswordSetListener.onSet();
        }
    }

    private void onDigitsClear() {
        mEnteredPassword.setText("");
        if (mDigitsClearListener != null) {
            mDigitsClearListener.onClear();
        }
    }

    private void onDigitClick(View v) {
        mEnteredPassword.setText(mEnteredPassword.getText().toString() + ((Button)v).getText());
    }

    static interface OnDigitsClearListener {
        public void onClear();
    }

    static interface OnRightPasswordSetListener {
        public void onSet();
    }

    static interface OnDigitClickListener {
        public void onClick(View v);
    }
}
