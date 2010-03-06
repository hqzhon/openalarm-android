package org.startsmall.openalarm;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
// import java.util.Calendar;

public class MusicService extends Service {
    public static final String EXTRA_KEY_AUDIO_ID_ARRAY = "audio_id_array";
    private static final String TAG = "MusicService";

    @Override
    public void onStart(Intent intent, int startId) {
        final long[] playlist = intent.getLongArrayExtra(EXTRA_KEY_AUDIO_ID_ARRAY);
        if (playlist != null && playlist.length > 0) {
            // Start service
            bindService(
                new Intent().setClassName("com.android.music",
                                          "com.android.music.MediaPlaybackService"),
                new ServiceConnection() {
                    public void onServiceConnected(ComponentName comp,
                                                   IBinder binder) {
                        Log.d(TAG, "====> onServiceConnected(): " + comp);

                        com.android.music.IMediaPlaybackService s =
                            com.android.music.IMediaPlaybackService.Stub.asInterface(binder);
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
                }, Context.BIND_AUTO_CREATE);
        }
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent i) {return null;}
}
