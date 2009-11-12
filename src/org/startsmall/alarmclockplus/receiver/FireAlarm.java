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
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
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
            // MediaPlay encountered problems on
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

    /// MediaPlayer object used to play ringtone.
    private MediaPlayer mMediaPlayer;

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
            String rtUri = intent.getStringExtra("ringtone");
            Log.d(TAG, "============> Before playing ringtone....." + rtUri);

            // Prepares the MediaPlayer
            mMediaPlayer = new MediaPlayer();
            if (mMediaPlayer == null) {
                return;
            }

            mMediaPlayer.setLooping(true);
            mMediaPlayer.setOnErrorListener(new OnPlaybackErrorListener());

            try {
                mMediaPlayer.setDataSource(this, Uri.parse(rtUri));
                mMediaPlayer.prepare();
            } catch (IllegalArgumentException e) {
                return;
            } catch (IOException e) {
                return;
            } catch (IllegalStateException e) {
                return;
            }

            // Set a 3-minutes one-shot timer to stop the
            // playback of ringtone.
            Handler handler = new Handler(new StopPlayback());
            Message stopPlaybackMessage =
                handler.obtainMessage(STOP_PLAYBACK, mMediaPlayer);
            if (handler.sendMessageDelayed(stopPlaybackMessage,
                                           180000)) {
                // Play ringtone now.
                mMediaPlayer.start();
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
}
