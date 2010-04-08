package org.startsmall.openalarm;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public class FireAlarmActivity extends Activity {
    private static final String TAG = "FireAlarmActivity";

    private static final int MSGID_STOP_PLAYBACK = 1;

    private static final int DEFAULT_SNOOZE_DURATION = 2; // 2 minutes
    private static final int PLAYBACK_TIMEOUT = 60000; // 1 minute

    private static final int AUTO_SNOOZE_COUNT_MAX = 3;
    private static int sAutoSnoozeCount = 0;

    private Ringtone mRingtone;
    private Handler mHandler;
    private Vibrator mVibrator;
    private PowerManager mPowerManager;
    private TelephonyManager mTelephonyManager;
    private KeyguardManager mKeyguardManager;
    private PowerManager.WakeLock mWakeLock;
    private KeyguardManager.KeyguardLock mKeyguardLock;
    private long[] mVibratePattern;

    private MathPanel mMathPanel;
    private LinearLayout mButtonPanel;
    private DigitsPanel mPasswordPanel;

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

        setContentView(R.layout.firealarm_activity);

        Intent intent = getIntent();
        setTimeFromIntent(intent);
        setLabelFromIntent(intent);

        // prepareMathLock();
        mButtonPanel = (LinearLayout)findViewById(R.id.button_panel);
        mMathPanel = (MathPanel)findViewById(R.id.math_panel);
        mMathPanel.setOnRightAnswerClickListener(
            new View.OnClickListener() {
                public void onClick(View v) {
                    setPanelVisible(AlarmHandler.LOCK_MODE_NONE);
                }
            });
        mPasswordPanel = (DigitsPanel)findViewById(R.id.password_panel);
        mPasswordPanel.setOnRightPasswordSetListener(
            new DigitsPanel.OnRightPasswordSetListener() {
                public void onSet() {
                    setPanelVisible(AlarmHandler.LOCK_MODE_NONE);
                }
            });
        prepareLockPanel();

        // Snooze this alarm makes the alarm postponded and saved
        // as a SharedPreferences.
        Button snoozeButton = (Button)mButtonPanel.findViewById(R.id.snooze);
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
        Button dismissButton = (Button)mButtonPanel.findViewById(R.id.dismiss);
        dismissButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissAlarm();
                    finish();
                }
            });

        // Prepare ringtone.
        openRingtone();
        startVibration();

        setVolumeControlStream(AudioManager.STREAM_ALARM);
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
    public void onStop() {
        super.onStop();
        // If this onStop() is called because of finish(), we
        // don't need to snooze alarm because it was already done
        // or it was dismissed.
        if (!isFinishing()) {
            snoozeAlarm();
            finish();
        }
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
            // prepareMathLock();
            prepareLockPanel();

            // The ringtone uri might be different and timeout of
            // playback needs to be recounted.
            mHandler.removeMessages(MSGID_STOP_PLAYBACK);
            openRingtone();
            startVibration();
        }
    }

    private void shutdown() {
        releaseWakeLock();

        if (mRingtone != null) {
            mRingtone.stop();
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

    private void prepareLockPanel() {
        Intent intent = getIntent();

        final String password = intent.getStringExtra(AlarmHandler.EXTRA_KEY_LOCK_MODE_PASSWORD);
        mPasswordPanel.setPassword(password);

        int lockMode = intent.getIntExtra(AlarmHandler.EXTRA_KEY_LOCK_MODE,
                                          AlarmHandler.LOCK_MODE_NONE);

        setPanelVisible(lockMode);
    }

    private void setPanelVisible(int index) {
        if (index == AlarmHandler.LOCK_MODE_NONE) {
            mButtonPanel.setVisibility(View.VISIBLE);
            mMathPanel.setVisibility(View.GONE);
            mPasswordPanel.setVisibility(View.GONE);
        } else if (index == AlarmHandler.LOCK_MODE_MATH) {
            mButtonPanel.setVisibility(View.GONE);
            mMathPanel.setVisibility(View.VISIBLE);
            mPasswordPanel.setVisibility(View.GONE);
        } else {
            mButtonPanel.setVisibility(View.GONE);
            mMathPanel.setVisibility(View.GONE);
            mPasswordPanel.setVisibility(View.VISIBLE);
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

        Toast.makeText(this,
                       getString(R.string.alarm_snoozed_toast, snoozeDuration),
                       Toast.LENGTH_LONG).show();
    }

    private void dismissAlarm() {
        final int alarmId = getIntent().getIntExtra(AlarmColumns._ID, -1);
        Alarms.dismissAlarm(this, alarmId);

        // If this alarm was snoozed before, clear it.
        Alarm alarm = Alarm.getInstance(this, alarmId);
        alarm.unsnooze(this);
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

    private void openRingtone() {
        if (mRingtone != null) {
            mRingtone.stop();
            mRingtone = null;
        }

        Uri ringtoneUri = null;
        Uri fallbackRingtoneUri = Uri.parse("android.resource://org.startsmall.openalarm/" +
                                            R.raw.in_call_ringtone);
        mRingtone = new Ringtone(this);
        if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
            // We're in call, we should use fallback ringtone
            // instead in order not to interfere the phone call
            // too much.
            ringtoneUri = fallbackRingtoneUri;
            mRingtone.setInCallMode();
        } else {
            Intent intent = getIntent();
            String uriString = intent.getStringExtra(AlarmHandler.EXTRA_KEY_RINGTONE);

            if (TextUtils.isEmpty(uriString)) {
                // No ringtone set in preference, try to use default ringtone.
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (ringtoneUri == null) {
                    // No default ringtone set either. use fallback ringtone instead.
                    Log.w(TAG, "===> No default ringtone either, use fallback ringtone instead");
                    ringtoneUri = fallbackRingtoneUri;
                }
            } else {
                ringtoneUri = Uri.parse(uriString);
            }
        }

        mRingtone.open(ringtoneUri);
        // Prepare MediaPlayer into Prepared state and play ringtone now.
        if (mRingtone != null &&
            mHandler.sendEmptyMessageDelayed(MSGID_STOP_PLAYBACK, PLAYBACK_TIMEOUT)) {
            mRingtone.play();
        }
    }

    private static class Ringtone {
        private static final float IN_CALL_VOLUME = 0.125f;
        private AudioManager mAudioManager;
        private MediaPlayer mAudio;
        private Uri mUri;
        private Context mContext;
        private boolean mInCallMode;

        public Ringtone(Context context) {
            mContext = context;
            mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        }

        public void open(Uri ringtoneUri) {
            mUri = ringtoneUri;
            try {
                openMediaPlayer();
            } catch (Exception ex) {
                Log.e(TAG, "Failed to open ringtone " + ringtoneUri);
            }
        }

        public void play() {
            if (mAudio == null) {
                try {
                    openMediaPlayer();
                } catch (Exception ex) {
                    Log.e(TAG, "play() caught ", ex);
                    mAudio = null;
                }
            }

            if (mAudio != null) {
                // do not ringtones if stream volume is 0
                // (typically because ringer mode is silent).
                if (mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                    mAudio.start();
                }
            }
        }

        public void stop() {
            if (mAudio != null) {
                mAudio.reset();
                mAudio.release();
                mAudio = null;
            }
        }

        public void setInCallMode() {
            mInCallMode = true;
        }

        private void openMediaPlayer() throws IOException {
            mAudio = new MediaPlayer();
            if (mUri != null) {
                mAudio.setDataSource(mContext, mUri);
            } else {
                throw new IOException("No data source set.");
            }
            mAudio.setAudioStreamType(AudioManager.STREAM_ALARM);
            mAudio.setLooping(true);
            if (mInCallMode) {
                // This line must be called after setAudioStreamType().
                mAudio.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
            }
            mAudio.prepare();
        }
    }
}
