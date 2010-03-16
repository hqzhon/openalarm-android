package org.startsmall.openalarm;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import java.util.Random;

public class MathPanel extends LinearLayout implements View.OnClickListener {
    private static final String TAG = "MathPanel";

    private View.OnClickListener mRightAnswerClickListener;

    private TextView mEquationView;
    private Button[] mButtons = new Button[4];

    public MathPanel(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(
            R.layout.math_panel, this, true);

        mEquationView = (TextView)findViewById(R.id.equation);
        mButtons[0] = (Button)findViewById(R.id.button_ul);
        mButtons[1] = (Button)findViewById(R.id.button_ur);
        mButtons[2] = (Button)findViewById(R.id.button_lr);
        mButtons[3] = (Button)findViewById(R.id.button_ll);
        for (int i = 0; i < 4; i++) {
            mButtons[i].setOnClickListener(this);
        }
        requestNewEquation();
    }

    public void setOnRightAnswerClickListener(View.OnClickListener listener) {
        mRightAnswerClickListener = listener;
    }

    private void requestNewEquation() {
        final Random rand = new Random();

        // Generate two numbers and calculate answer according to
        // difficulty level.
        int i1 = rand.nextInt(400);
        int i2 = rand.nextInt(600);
        final int answer = i1 + i2;
        mEquationView.setText(i1 + " + " + i2 + " = ?");
        final int buttonIndexForAnswer = rand.nextInt(300) % 4;

        Button button;
        for (int i = 0; i < 4; i++) {
            button = mButtons[i];
            button.setTag((Integer)answer);
            if (i == buttonIndexForAnswer) {
                button.setText(Integer.toString(answer));
            } else {
                button.setText(Integer.toString(rand.nextInt(300) + i * rand.nextInt(300)));
            }
        }
    }

    public void onClick(View view) {
        TextView tv = (TextView)view;
        boolean wrongAnswerEntered = true;
        int answer = (Integer)tv.getTag();
        int userAnswer = -1;
        try {
            userAnswer = Integer.parseInt(tv.getText().toString());
            if (userAnswer == answer) {
                wrongAnswerEntered = false;
            }
        } catch (NumberFormatException e) {
        }

        if (wrongAnswerEntered) {
            requestNewEquation();
        } else {
            if (mRightAnswerClickListener != null) {
                mRightAnswerClickListener.onClick(view);
            }
        }
    }
}
