package org.startsmall.openalarm;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.Arrays;

public class MusicService extends Service {
    public static final String EXTRA_KEY_AUDIO_ID_ARRAY = "audio_id_array";
    private static final String TAG = "MusicService";
    private static final String ACTION_HTC_MUSIC_PLAYBACKCOMPLETE = "com.htc.music.playbackcomplete";
    private static final String ACTION_ANDROID_MUSIC_PLAYBACKCOMPLETE = "com.android.music.playbackcomplete";
    private BroadcastReceiver mPlaybackCompletedReceiver;
    private ServiceConnection mConnection;

    @Override
    public void onStart(Intent intent, int startId) {
        handleStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleStart(intent, startId);
        return START_NOT_STICKY;
    }

    private void handleStart(Intent intent, int startId) {
        int[] playlist = intent.getIntArrayExtra(EXTRA_KEY_AUDIO_ID_ARRAY);
        if (playlist != null && playlist.length > 0) {
            boolean success = bindToHtcMediaService(playlist);
            if (!success) {
                success = bindToAndroidMediaService(playlist);
            }

            if (success) {
                mPlaybackCompletedReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            String action = intent.getAction();
                            if (action.equals(ACTION_HTC_MUSIC_PLAYBACKCOMPLETE) ||
                                action.equals(ACTION_ANDROID_MUSIC_PLAYBACKCOMPLETE)) {
                                unbindService(mConnection);
                                stopSelf();
                            }
                        }
                    };

                IntentFilter iFilter = new IntentFilter();
                iFilter.addAction(ACTION_ANDROID_MUSIC_PLAYBACKCOMPLETE);
                iFilter.addAction(ACTION_HTC_MUSIC_PLAYBACKCOMPLETE);
                registerReceiver(mPlaybackCompletedReceiver, iFilter);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mPlaybackCompletedReceiver != null) {
            unregisterReceiver(mPlaybackCompletedReceiver);
        }
    }

    @Override
    public IBinder onBind(Intent i) {return null;}

    public boolean bindToHtcMediaService(final int[] playlist) {
        mConnection =
            new ServiceConnection() {
                public void onServiceConnected(ComponentName comp,
                                               IBinder binder) {
                    com.htc.music.IMediaPlaybackService s =
                        com.htc.music.IMediaPlaybackService.Stub.asInterface(binder);
                    try {
                        if (s.isPlaying()) {
                            s.stop();
                        }
                        s.open(playlist, 0);
                        s.play();
                        Log.d(TAG, "===> Connected com.htc.music.MediaPlaybackService");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                public void onServiceDisconnected(ComponentName comp) {}
            };

        boolean success =
            bindService(
                new Intent().setClassName("com.htc.music",
                                          "com.htc.music.MediaPlaybackService"),
                mConnection, Context.BIND_AUTO_CREATE);
        if (!success) {
            unbindService(mConnection);
            mConnection = null;
        }
        return success;
    }

    public boolean bindToAndroidMediaService(final int[] playlist) {
        // convert to a list of longs
        final int length = playlist.length;
        final long[] data = new long[length];
        for (int i = 0; i < length; i++) {
            data[i] = playlist[i];
        }

        mConnection =
            new ServiceConnection() {
                public void onServiceConnected(ComponentName comp,
                                               IBinder binder) {
                    com.android.music.IMediaPlaybackService s =
                        com.android.music.IMediaPlaybackService.Stub.asInterface(binder);
                    try {
                        if (s.isPlaying()) {
                            s.stop();
                        }
                        s.open(data, 0);
                        s.play();
                        Log.d(TAG, "===> Connected to com.android.music.MediaPlaybackService");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                public void onServiceDisconnected(ComponentName comp) {}
            };

        boolean success =
            bindService(
                new Intent().setClassName("com.android.music",
                                          "com.android.music.MediaPlaybackService"),
                mConnection, Context.BIND_AUTO_CREATE);
        if (!success) {
            unbindService(mConnection);
            mConnection = null;
        }
        return success;
    }

}
