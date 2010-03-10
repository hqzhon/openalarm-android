package org.startsmall.openalarm;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException;
import java.io.FileDescriptor;
import java.util.Calendar;
import java.util.Random;

public class FireAlarm extends Activity
                       implements View.OnClickListener {
    private static final String TAG = "FireAlarm";

    private static final int MSGID_STOP_PLAYBACK = 1;

    private static final int DEFAULT_SNOOZE_DURATION = 2; // 2 minutes
    private static final float IN_CALL_VOLUME = 0.125f;
    private static final float NORMAL_VOLUME = 0.9f;
    private static final int PLAYBACK_TIMEOUT = 60000; // 1 minute

    private static final int AUTO_SNOOZE_COUNT_MAX = 3;
    private static int sAutoSnoozeCount = 0;

    private MediaPlayer mMediaPlayer;
    private Handler mHandler;
    private Vibrator mVibrator;
    private PowerManager mPowerManager;
    private TelephonyManager mTelephonyManager;
    private KeyguardManager mKeyguardManager;
    private PowerManager.WakeLock mWakeLock;
    private KeyguardManager.KeyguardLock mKeyguardLock;
    private long[] mVibratePattern;

    private TableLayout mMathPanel;
    private LinearLayout mButtonPanel;
    private TextView mEquationTextView;
    private TextView[] mAnswerTextView = new TextView[4];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mTelephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        mKeyguardManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);;
        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                    case MSGID_STOP_PLAYBACK:
                        Log.i(TAG, "===> Ringtone playing timed out.");
                        // This callback is executed because user doesn't
                        // tell me what to do, i.e., dimiss or snooze.
                        autoSnoozeOrDismissAlarm();
                        finish();
                        break;

                    default:
                        break;
                    }
                }
            };

        acquireWakeLock();

        setContentView(R.layout.fire_alarm);

        Intent intent = getIntent();
        setTimeFromIntent(intent);
        setLabelFromIntent(intent);

        prepareMathLock();

        // Snooze this alarm makes the alarm postponded and saved
        // as a SharedPreferences.
        Button snoozeButton = (Button)findViewById(R.id.snooze);
        snoozeButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snoozeAlarm();
                    finish();
                }
            });

        // Dismiss the alarm causes the ringtone playback of this
        // alarm stopped and reschudiling of this alarm happens.
        Button dismissButton = (Button)findViewById(R.id.dismiss);
        dismissButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissAlarm();
                    finish();
                }
            });

        // Prepare MediaPlayer for playing ringtone.
        prepareMediaPlayer();

        startVibration();

        setNotification(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdown();
    }

    @Override
    public void finish() {
        super.finish();
        shutdown();
    }

    // FireAlarm comes to the foreground
    @Override
    public void onResume() {
        super.onResume();

        // FireAlarm goes back to interact to user. But, Keyguard
        // may be in front.
        disableKeyguard();

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        findViewById(R.id.icon).startAnimation(shake);
    }

    @Override
    public void onPause() {
        super.onPause();

        // Returns to keyguarded mode if the phone was in this
        // mode.
        enableKeyguard();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
            event.getRepeatCount() == 0) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        if (!getIntent().filterEquals(newIntent)) {
            // Dismiss the old alarm. This only takes care of the
            // case that when two alarms are scheduled at the
            // same time, we should dismiss the old alarm and
            // override FireAlarm with new alarm.
            dismissAlarm();
            stopVibration();

            // New intent comes. Replace the old one.
            setIntent(newIntent);

            // Refresh UI of the existing instance.
            setTimeFromIntent(newIntent);
            setLabelFromIntent(newIntent);
            prepareMathLock();

            // The ringtone uri might be different and timeout of
            // playback needs to be recounted.
            mHandler.removeMessages(MSGID_STOP_PLAYBACK);
            prepareMediaPlayer();
            startVibration();
        }
    }

    private void shutdown() {
        releaseWakeLock();

        // Stop and release MediaPlayer object.
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        // When this activity is intended to finish, remove all
        // STOP_PLAYBACK messages from queue.
        if (mHandler != null) {
            mHandler.removeMessages(MSGID_STOP_PLAYBACK);
            mHandler = null;
        }

        stopVibration();
    }

    private void setLabelFromIntent(Intent intent) {
        String label = intent.getStringExtra(AlarmColumns.LABEL);
        TextView labelView = (TextView)findViewById(R.id.label);
        labelView.setText(label);
    }

    private void setTimeFromIntent(Intent intent) {
        final int hourOfDay = intent.getIntExtra(AlarmColumns.HOUR_OF_DAY, -1);
        final int minutes = intent.getIntExtra(AlarmColumns.MINUTES, -1);

        TimeAmPmView timeAmPmView = (TimeAmPmView)findViewById(R.id.time_am_pm);
        timeAmPmView.setTextAppearance(this, TimeAmPmView.TIME_TEXT, R.style.TextAppearanceHuge);
        timeAmPmView.setTime(hourOfDay, minutes);
    }

    private void prepareMathLock() {
        mMathPanel = (TableLayout)findViewById(R.id.math_panel);
        TextView tv;
        final int childCount = mMathPanel.getChildCount();
        for (int i = 1; i < childCount; i++) {
            TableRow row = (TableRow)mMathPanel.getChildAt(i);
            for (int j = 0; j < 2; j++) {
                tv = (TextView)row.getChildAt(j);
                tv.setOnClickListener(this);
                mAnswerTextView[(i - 1 )*2 + j] = tv;
            }
        }
        tv = mAnswerTextView[3];
        mAnswerTextView[3] = mAnswerTextView[2];
        mAnswerTextView[2] = tv;

        mButtonPanel = (LinearLayout)findViewById(R.id.button_panel);

        Intent intent = getIntent();
        boolean isMathLockOn = intent.getBooleanExtra(AlarmHandler.EXTRA_KEY_MATH_LOCK_ON, false);
        if (isMathLockOn) {
            mEquationTextView = (TextView)mMathPanel.findViewById(R.id.equation);

            mMathPanel.setVisibility(View.VISIBLE);
            mButtonPanel.setVisibility(View.GONE);

            // Generate a simple equation of the addition of two integers.
            requestNewEquation();
       } else {
            mMathPanel.setVisibility(View.GONE);
            mButtonPanel.setVisibility(View.VISIBLE);
        }
    }

    private void requestNewEquation() {
        final Random rand = new Random();
        int i1 = rand.nextInt(400);
        int i2 = rand.nextInt(600);
        final int answer = i1 + i2;
        final int answerTextViewId = rand.nextInt(3);

        mEquationTextView.setText(i1 + " + " + i2 + " = ?");
        TextView tv;
        for (int i = 0; i < 4; i++) {
            tv = mAnswerTextView[i];
            tv.setTag((Integer)answer);
            if (i == answerTextViewId) {
                tv.setText(Integer.toString(answer));
            } else {
                tv.setText(Integer.toString(rand.nextInt(300) + i * rand.nextInt(300)));
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
                mMathPanel.setVisibility(View.GONE);
                mButtonPanel.setVisibility(View.VISIBLE);
                wrongAnswerEntered = false;
            }
        } catch (NumberFormatException e) {
        }

        if (wrongAnswerEntered) {
            requestNewEquation();
        }
    }

    private void autoSnoozeOrDismissAlarm() {
        // Check to see if we have snoozed too many times!
        final int alarmId = getIntent().getIntExtra(AlarmColumns._ID, -1);
        Alarm alarm = Alarm.getInstance(this, alarmId);

        if (sAutoSnoozeCount > AUTO_SNOOZE_COUNT_MAX) {
            // have snooze too many times
            dismissAlarm();
            sAutoSnoozeCount = 0;
            return;
        }

        snoozeAlarm();
        sAutoSnoozeCount++;
    }

    private void snoozeAlarm() {
        Intent i = getIntent();
        final int alarmId = i.getIntExtra(AlarmColumns._ID, -1);
        final int snoozeDuration = i.getIntExtra(AlarmHandler.EXTRA_KEY_SNOOZE_DURATION,
                                                 DEFAULT_SNOOZE_DURATION);

        Alarm alarm = Alarm.getInstance(this, alarmId);
        alarm.snooze(this, snoozeDuration);

        setNotification(false);
    }

    private void dismissAlarm() {
        final int alarmId = getIntent().getIntExtra(AlarmColumns._ID, -1);
        Alarms.dismissAlarm(this, alarmId);
    }

    private void startVibration() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra(AlarmHandler.EXTRA_KEY_VIBRATE, false)) {
            if (mVibratePattern == null) {
                mVibratePattern = new long[]{500, 500};
            }
            mVibrator.vibrate(mVibratePattern, 0);
        }
    }

    private void stopVibration() {
        mVibrator.cancel();
    }

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            mWakeLock =
                mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP|
                                          PowerManager.FULL_WAKE_LOCK,
                                          TAG);
        }

        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void enableKeyguard() {
        if (mKeyguardLock == null) {
            mKeyguardLock = mKeyguardManager.newKeyguardLock(TAG);
        }
        mKeyguardLock.reenableKeyguard();
    }

    private void disableKeyguard() {
        if (mKeyguardLock == null) {
            mKeyguardLock = mKeyguardManager.newKeyguardLock(TAG);
        }
        mKeyguardLock.disableKeyguard();
    }

    private void prepareMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        } else {
            // Stop and reset MediaPlayer here is required
            // because onNewIntent() might override the old
            // settings. Use new settings for MediaPlayer.
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        }

        // Whether we should use fallback ringtone?
        Intent intent = getIntent();
        String uriString = "";
        boolean useFallbackRingtone = false;
        if (intent.hasExtra(AlarmHandler.EXTRA_KEY_RINGTONE)) {
            uriString = intent.getStringExtra(AlarmHandler.EXTRA_KEY_RINGTONE);
            if (TextUtils.isEmpty(uriString) ||
                mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                useFallbackRingtone = true;
            }
        } else {
            useFallbackRingtone = true;
        }

        try {
            if (useFallbackRingtone) {
                // This raw media must be supported by Android
                // and no errors thrown from it.
                AssetFileDescriptor afd =
                    getResources().openRawResourceFd(R.raw.in_call_ringtone);
                mMediaPlayer.setDataSource(afd.getFileDescriptor(),
                                           afd.getStartOffset(),
                                           afd.getLength());
                afd.close();

                if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                    Log.i(TAG, "===> We're in a call. Lower volume and use fallback ringtone!");
                    mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                }
            } else {
                mMediaPlayer.setDataSource(this, Uri.parse(uriString));
                mMediaPlayer.setVolume(NORMAL_VOLUME, NORMAL_VOLUME);
            }
        } catch (java.io.IOException e) {
            return;
        }

        mMediaPlayer.setLooping(true);

        // Prepare MediaPlayer into Prepared state and
        // MediaPlayer is ready to play.
        try {
            mMediaPlayer.prepare();
        } catch (java.io.IOException e) {
            return;
        }

        if (mHandler.sendEmptyMessageDelayed(MSGID_STOP_PLAYBACK,
                                             PLAYBACK_TIMEOUT)) {
            mMediaPlayer.start();
        }
    }

    private void setNotification(boolean enable) {
        if (enable) {
            NotificationManager nm =
                (NotificationManager)getSystemService(
                    Context.NOTIFICATION_SERVICE);
            nm.cancelAll();

            Intent intent = new Intent(getIntent());
            intent.setClass(this, FireAlarm.class);
            PendingIntent intentSender =
                PendingIntent.getActivity(this, 0,
                                          intent,
                                          PendingIntent.FLAG_CANCEL_CURRENT);

            final long when = intent.getLongExtra(AlarmColumns.TIME_IN_MILLIS, -1);
            Calendar calendar = Alarms.getCalendarInstance();
            calendar.setTimeInMillis(when);
            final String timeString = DateUtils.formatDateTime(
                this, when,
                DateUtils.FORMAT_SHOW_TIME|DateUtils.FORMAT_SHOW_DATE|DateUtils.FORMAT_CAP_AMPM|
                DateUtils.FORMAT_SHOW_WEEKDAY|DateUtils.FORMAT_SHOW_YEAR);

            String label = getIntent().getStringExtra(AlarmColumns.LABEL);
            String tickerText = getString(R.string.fire_alarm_set_notification_ticker, label);
            String contentText = getString(R.string.alarm_set_notification_content, timeString);

            android.app.Notification notification =
                new android.app.Notification(R.drawable.stat_notify_warning_alarm,
                                             tickerText, System.currentTimeMillis());
            notification.flags = android.app.Notification.FLAG_NO_CLEAR;
            notification.setLatestEventInfo(this,
                                            tickerText,
                                            contentText,
                                            intentSender);
            nm.notify(0, notification);
        } else {
            Notification.getInstance().set(this);
        }
    }

    private static class OnPlaybackErrorListener implements MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(TAG, "===> onError(): " + what + "====> " + extra);
            return true;
        }
    }
}
