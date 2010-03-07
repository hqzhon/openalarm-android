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
    private ServiceConnection mConnection;

    @Override
    public void onStart(Intent intent, int startId) {
        int[] playlist = intent.getIntArrayExtra(EXTRA_KEY_AUDIO_ID_ARRAY);
        if (playlist != null && playlist.length > 0) {
            if (!bindToHtcMediaService(playlist)) {
                bindToAndroidMediaService(playlist);
            }
        }

        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction("com.android.music.playbackcomplete");
        iFilter.addAction("com.htc.music.playbackcomplete");
        registerReceiver(mPlaybackCompletedReceiver, iFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mPlaybackCompletedReceiver);
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
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    // In Android 2.1, we can call
                    // unbindService() right here. But, below,
                    // unbindService decrements the reference
                    // count of service record which makes
                    // Android decide to kill this
                    // MediaPlaybackService immediately because
                    // no one refers to it!
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

    private BroadcastReceiver mPlaybackCompletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.htc.music.playbackcomplete") ||
                action.equals("com.android.music.playbackcomplete")) {
                unbindService(mConnection);
                stopSelf();
            }
        }
    };
}
