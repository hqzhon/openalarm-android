package org.startsmall.openalarm;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class MusicService extends Service {
    public static final String EXTRA_KEY_AUDIO_ID_ARRAY = "audio_id_array";
    private static final String TAG = "MusicService";

    @Override
    public void onStart(Intent intent, int startId) {
        int[] playlist = intent.getIntArrayExtra(EXTRA_KEY_AUDIO_ID_ARRAY);
        if (playlist != null && playlist.length > 0) {
            if (!bindToHtcMediaService(playlist)) {
                bindToAndroidMediaService(playlist);
            }
        }
    }

    @Override
    public IBinder onBind(Intent i) {return null;}

    public boolean bindToHtcMediaService(final int[] playlist) {
        ServiceConnection conn =
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
                    unbindService(this);
                }

                public void onServiceDisconnected(ComponentName comp) {}
            };

        boolean success =
            bindService(
                new Intent().setClassName("com.htc.music",
                                          "com.htc.music.MediaPlaybackService"),
                conn, Context.BIND_AUTO_CREATE);
        if (!success) {
            unbindService(conn);
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

        ServiceConnection conn =
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
                    unbindService(this);
                }

                public void onServiceDisconnected(ComponentName comp) {}
            };

        boolean success =
            bindService(
                new Intent().setClassName("com.android.music",
                                          "com.android.music.MediaPlaybackService"),
                conn, Context.BIND_AUTO_CREATE);
        if (!success) {
            unbindService(conn);
        }

        return success;
    }
}
