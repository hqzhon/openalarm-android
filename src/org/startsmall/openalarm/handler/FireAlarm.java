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
import android.widget.TextView;

import java.io.IOException;
import java.io.FileDescriptor;
import java.util.Calendar;

public class FireAlarm extends Activity {
    // MediaPlayer enters Prepared state and now a
    // Handler should be setup to stop playback of
    // ringtone after some period of time.
    private class StopPlayback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_ID_STOP_PLAYBACK:
                Log.i(TAG, "===> Ringtone playing timed out.");
                // This callback is executed because user doesn't
                // tell me what to do, i.e., dimiss or snooze.
                FireAlarm.this.autoSnoozeOrDismissAlarm();
                FireAlarm.this.finish();
                return true;
            default:
                break;
            }
            return true;
        }
    }

    private static class OnPlaybackErrorListener implements MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(TAG, "===> onError(): " + what + "====> " + extra);
            return true;
        }
    }

    private static final String TAG = "FireAlarm";

    private static final String EXTRA_KEY_VIBRATE = "vibrate";
    private static final String EXTRA_KEY_RINGTONE = "ringtone";
    private static final String EXTRA_KEY_SNOOZE_DURATION = "snooze_duration";
    private static final int DEFAULT_SNOOZE_DURATION = 2; // 2 minutes
    private static final int MESSAGE_ID_STOP_PLAYBACK = 1;
    private static final float IN_CALL_VOLUME = 0.125f;
    private static final int PLAYBACK_TIMEOUT = 60000; // 1 minute
    private static final int TITLE_MAX_LENGTH = 18;

    private static final int AUTO_SNOOZE_COUNT_MAX = 3;
    private static int sAutoSnoozeCount = 0;

    private static MediaPlayer sMediaPlayer;
    private static Handler sHandler;
    private Vibrator mVibrator;
    private PowerManager mPowerManager;
    private TelephonyManager mTelephonyManager;
    private KeyguardManager mKeyguardManager;
    private PowerManager.WakeLock mWakeLock;
    private KeyguardManager.KeyguardLock mKeyguardLock;
    private long[] mVibratePattern;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mTelephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        mKeyguardManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);;
        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        acquireWakeLock();

        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.fire_alarm);

        setLabelFromIntent();
        setWindowTitleFromIntent();

        // Snooze this alarm makes the alarm postponded and saved
        // as a SharedPreferences.
        Button snoozeButton = (Button)findViewById(R.id.snooze);
        snoozeButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FireAlarm.this.snoozeAlarm();
                    FireAlarm.this.finish();
                }
            });

        // Dismiss the alarm causes the ringtone playback of this
        // alarm stopped and reschudiling of this alarm happens.
        Button dismissButton = (Button)findViewById(R.id.dismiss);
        dismissButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FireAlarm.this.dismissAlarm();
                    FireAlarm.this.finish();
                }
            });

        // Prepare MediaPlayer for playing ringtone.
        boolean ok = prepareMediaPlayer();
        if (savedInstanceState == null && ok) {
            if (sHandler == null) {
                sHandler = new Handler(new StopPlayback());
            }
            ok = sHandler.sendEmptyMessageDelayed(MESSAGE_ID_STOP_PLAYBACK, PLAYBACK_TIMEOUT);
        }

        if (ok) {
            sMediaPlayer.start();
        }

        startVibration();

        setNotification(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        releaseWakeLock();
    }

    @Override
    public void finish() {
        super.finish();

        stopVibration();

        // Stop and release MediaPlayer object.
        if (sMediaPlayer != null) {
            sMediaPlayer.stop();
            sMediaPlayer.release();
            sMediaPlayer = null;
        }

        // When this activity is intended to finish, remove all
        // STOP_PLAYBACK messages from queue.
        if (sHandler != null) {
            sHandler.removeMessages(MESSAGE_ID_STOP_PLAYBACK);
        }

        setNotification(false);
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
    public boolean onKeyUp(int keyCode, KeyEvent event)  {
        return super.onKeyUp(keyCode, event);
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
            // Dismiss the old alarm (when two alarms are
            // scheduled at the same time, the only difference
            // between two alarms is the data they are operating
            // on).
            dismissAlarm();
            stopVibration();

            // New intent comes. Replace the old one.
            setIntent(newIntent);

            // Refresh UI of the existing instance.
            setLabelFromIntent();
            setWindowTitleFromIntent();

            // The ringtone uri might be different and timeout of
            // playback needs to be recounted.
            if (prepareMediaPlayer()) {
                sHandler.removeMessages(MESSAGE_ID_STOP_PLAYBACK);
                // A new TIME_OUT message
                if (sHandler.sendEmptyMessageDelayed(MESSAGE_ID_STOP_PLAYBACK, PLAYBACK_TIMEOUT)) {
                    sMediaPlayer.start();
                }
            }

            startVibration();
        }

        // If two Intent objects are equal, they are the same
        // thing. FireAlarm is restored from notification bar.
    }

    private void setWindowTitleFromIntent() {
        Intent i = getIntent();
        String label = i.getStringExtra(AlarmColumns.LABEL);
        if (label.length() > TITLE_MAX_LENGTH) {
            label = label.substring(0, TITLE_MAX_LENGTH - 3) + " ...";
        }
        setTitle(label);
    }

    private void setLabelFromIntent() {
        Intent i = getIntent();
        final int hourOfDay = i.getIntExtra(AlarmColumns.HOUR_OF_DAY, -1);
        final int minutes = i.getIntExtra(AlarmColumns.MINUTES, -1);

        CompoundTimeTextView timeWithAMPM = (CompoundTimeTextView)findViewById(R.id.time);
        timeWithAMPM.setTextAppearance(this, CompoundTimeTextView.TIME_TEXT, R.style.HugeTextView);
        timeWithAMPM.setTime(hourOfDay, minutes);
    }

    private void autoSnoozeOrDismissAlarm() {
        // Check to see if we have snoozed too many times!
        final int alarmId = getIntent().getIntExtra(AlarmColumns._ID, -1);
        Alarm alarm = Alarm.getInstance(alarmId);

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
        final int snoozeDuration = i.getIntExtra(EXTRA_KEY_SNOOZE_DURATION, DEFAULT_SNOOZE_DURATION);

        Alarm alarm = Alarm.getInstance(alarmId);
        alarm.snooze(this, snoozeDuration);
    }

    private void dismissAlarm() {
        Intent scheduleIntent = new Intent(getIntent());
        scheduleIntent.setFlags(
            scheduleIntent.getFlags() ^ Intent.FLAG_ACTIVITY_NEW_TASK);
        scheduleIntent.setAction(Alarm.ACTION_SCHEDULE);
        scheduleIntent.setComponent(null);
        sendBroadcast(scheduleIntent);
    }

    private void startVibration() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra("vibrate", false)) {
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

    private boolean prepareMediaPlayer() {
        if (sMediaPlayer == null) {
            sMediaPlayer = new MediaPlayer();
        } else {
            // Stop and reset MediaPlayer here is required
            // because onNewIntent() might override the old
            // settings. Use new settings for MediaPlayer.
            sMediaPlayer.stop();
            sMediaPlayer.reset();
        }

        // Whether we should use fallback ringtone?
        Intent intent = getIntent();
        String uriString = "";
        boolean useFallbackRingtone = false;
        if (intent.hasExtra(EXTRA_KEY_RINGTONE)) {
            uriString = intent.getStringExtra(EXTRA_KEY_RINGTONE);
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
                sMediaPlayer.setDataSource(afd.getFileDescriptor(),
                                           afd.getStartOffset(),
                                           afd.getLength());
                afd.close();

                if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                    Log.i(TAG, "===> We're in a call. Lower volume and use fallback ringtone!");
                    sMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                }
            } else {
                sMediaPlayer.setDataSource(this, Uri.parse(uriString));
            }
        } catch (java.io.IOException e) {
            return false;
        }

        sMediaPlayer.setLooping(true);

        // Prepare MediaPlayer into Prepared state and
        // MediaPlayer is ready to play.
        try {
            sMediaPlayer.prepare();
        } catch (java.io.IOException e) {
            return false;
        }

        return true;
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
}
