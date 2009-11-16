/**
 * @file   FireAlarm.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Tue Nov  3 20:33:08 2009
 *
 * @brief An activity that is launched when an alarm goes off.
 *
 *
 */
package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.R;
import org.startsmall.alarmclockplus.Alarms;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
            // Stop playing and release the MediaPlayer.
            switch (msg.what) {
            case STOP_PLAYBACK:
                // This callback is executed because user doesn't
                // tell me what to do, dimiss or snooze. I decide
                // to snooze it.
                FireAlarm.this.snoozeAlarm();
                return true;
            default:
                break;
            }
            return true;
        }
    }

    private class OnPlaybackErrorListener implements MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            // The FireAlarm should prompt user when
            // MediaPlayer encountered problems on
            // playing ringtone.
            AlertDialog.Builder builder =
                new AlertDialog.Builder(FireAlarm.this);
            builder.setCancelable(false)
                .setTitle(R.string.media_player_error_dialog_title)
                .setMessage(
                    String.format(
                        FireAlarm.this.getString(R.string.media_player_error_dialog_message), what))
                .setPositiveButton(
                    R.string.isee,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            // QUESTION? What do you gonna do when this happens????
                            Log.d(TAG, "======> I see!");
                        }
                    })
                .show();
            return true;
        }
    }

    private static final String TAG = "FireAlarm";

    /// An ID used to identify the Message sent to stop the playback of ringtone in the Handler.Callback.
    private static final int STOP_PLAYBACK = 1;

    /// Copied from Android's source code which is claimed recommended values by media team.
    private static final float IN_CALL_VOLUME = 0.125f;

    private static final int PLAYBACK_TIMEOUT = 18000; // 3 minutes

    /// MediaPlayer object used to play ringtone.
    private MediaPlayer mMediaPlayer;

    private Vibrator mVibrator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fire_alarm);

        // Snooze this alarm makes the alarm postponded and saved
        // as a SharedPreferences.
        Button snoozeButton = (Button)findViewById(R.id.snooze);
        snoozeButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FireAlarm.this.snoozeAlarm();
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
                }
            });

        // Start playing ringtone loop.
        Intent intent = getIntent();
        if(intent.hasExtra("ringtone")) {

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(new OnPlaybackErrorListener());

            try {
                // Detects if we are in a call.
                TelephonyManager tm = (TelephonyManager)getSystemService(
                    Context.TELEPHONY_SERVICE);
                if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                    String rtUri = intent.getStringExtra("ringtone");
                    mMediaPlayer.setDataSource(this, Uri.parse(rtUri));
                } else {
                    Log.d(TAG, "===> We're in a call. lower volume and use fallback ringtone!");
                    // This raw media must be supported by
                    // Android and no errors thrown from it.
                    FileDescriptor fd =
                        getResources().openRawResourceFd(R.raw.in_call_ringtone).getFileDescriptor();
                    mMediaPlayer.setDataSource(fd);
                    mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                }
            } catch (Exception e1) {
                // mMediaPlayer had entered Error state and
                // OnErrorListener was called asynchronously.

                //
                mMediaPlayer.reset();
                FileDescriptor fd =
                    getResources().openRawResourceFd(R.raw.in_call_ringtone).getFileDescriptor();
                try {
                    mMediaPlayer.setDataSource(fd);
                } catch (Exception e2) {
                    return;
                }
            }

            mMediaPlayer.setLooping(true);

            try {
                mMediaPlayer.prepare();
            } catch (Exception e) {
                Log.d(TAG, "=========> mMediaPlayer.prepare(): " + e);
                return;
            }

            // Set a 3-minutes one-shot timer to stop the
            // playback of ringtone.
            Handler handler = new Handler(new StopPlayback());
            Message stopPlaybackMessage =
                handler.obtainMessage(STOP_PLAYBACK, mMediaPlayer);
            if (handler.sendMessageDelayed(stopPlaybackMessage,
                                           PLAYBACK_TIMEOUT)) {
                // Play ringtone now.
                mMediaPlayer.start();

                vibrate();
            } else {
                Log.d(TAG, "===> Unable to enqueue message");
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            Log.d(TAG, "===> MediaPlayer stopped and released");
        }
    }

    private void snoozeAlarm() {
        Alarms.snoozeAlarm(FireAlarm.this, getIntent(), 2);
        finish();
    }

    private void dismissAlarm() {
        final Intent intent = getIntent();
        final int alarmId = intent.getIntExtra(Alarms.AlarmColumns._ID, -1);

        Log.d(TAG, "===> dismissAlarm(): alarm id=" + alarmId);

        // The snoozing alarm is enabled during this lifetime of
        // this activity. Its settings remain the same. Dimiss
        // this alarm means we should try to calculate the new
        // time of the alarm again.

        // Deactivate the old alarm. The explicit class field of
        // the Intent was set to this activity when setting alarm
        // in AlarmManager..
        Alarms.setAlarm(this, intent, false);

        // Activate the alarm according to the new time.
        intent.setClassName(this,
                            getPackageName() + ".receiver.ActionDispatcher");

        final int hourOfDay = intent.getIntExtra(Alarms.AlarmColumns.HOUR, -1);
        // If user clicks dimiss button in this minute, the
        // calculateAlarmAtTimeInMillis() will return the same
        // hour and minutes which causes this Activity to show up
        // continuously.
        final int minutes = intent.getIntExtra(Alarms.AlarmColumns.MINUTES, -1) - 1;
        final int repeatOnDaysCode = intent.getIntExtra(Alarms.AlarmColumns.REPEAT_DAYS, -1);

        Log.d(TAG, "===> hourOfDay=" + hourOfDay
              + ", minutes=" + minutes
              + ", repeat=" + Alarms.RepeatWeekdays.toString(repeatOnDaysCode));

        long atTimeInMillis =
            Alarms.calculateAlarmAtTimeInMillis(hourOfDay, minutes,
                                                repeatOnDaysCode);
        intent.putExtra(Alarms.AlarmColumns.AT_TIME_IN_MILLIS, atTimeInMillis);
        Alarms.setAlarm(this, intent, true);
        Calendar calendar = Alarms.getCalendarInstance();
        calendar.setTimeInMillis(atTimeInMillis);
        Log.d(TAG, "===> dismissAlarm(): new alarm at " +
              Alarms.formatDate("HH:mm", calendar));


        ContentValues newValues = new ContentValues();
        newValues.put(Alarms.AlarmColumns.AT_TIME_IN_MILLIS, atTimeInMillis);
        Alarms.updateAlarm(this, Alarms.getAlarmUri(alarmId), newValues);
        Alarms.setNotification(this, intent, true);

        finish();
    }

    private void vibrate() {
        final long[] vibratePattern = new long[] {500, 500};

        Intent intent = getIntent();
        if (intent.hasExtra("vibrate") &&
            intent.getBooleanExtra("vibrate", false)) {
            if (mVibrator == null) {
                mVibrator =
                    (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            }
            mVibrator.vibrate(vibratePattern, 0);
        }
    }
}
