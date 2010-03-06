package org.startsmall.openalarm;

import android.content.Intent;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.Preference;
import android.provider.MediaStore;
import android.text.method.DialerKeyListener;
import android.text.TextUtils;
import android.util.Log;
import java.util.*;

public class MusicHandler extends AbsHandler {
    private static final String TAG = "MusicHandler";
    private static final String EXTRA_KEY_PLAYLIST_ID = "playlist_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "===> MusicHandler.onReceive()");

        final String extra = intent.getStringExtra(AlarmColumns.EXTRA);
        putBundleIntoIntent(intent, getBundleFromExtra(extra));

        final int playlistId = intent.getIntExtra(EXTRA_KEY_PLAYLIST_ID, -1);
        if (playlistId != -1) {
            // Now, we have playlist id, we should start a
            // service to play members of this playlist. We also
            // need to consider the situation when external
            // storage is not inserted.
            Uri memberUri =
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
            Cursor cursor = null;
            try {
                cursor =
                    context.getContentResolver().query(
                        memberUri,
                        new String[]{MediaStore.Audio.Playlists.Members.AUDIO_ID},
                        null,
                        null,
                        MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
                if (cursor != null && cursor.moveToFirst()) {
                    int[] audioIds = new int[cursor.getCount()];
                    do {
                        int audioId = cursor.getInt(0);
                        audioIds[cursor.getPosition()] = audioId;
                    } while (cursor.moveToNext());

                    // Can't use bindService in
                    // BroadcastRecevier. Redirect to a service
                    // and bind to MediaPlaybackService
                    // overthere.
                    Intent musicIntent = new Intent(intent);
                    musicIntent.setClass(context, MusicService.class);
                    musicIntent.putExtra(MusicService.EXTRA_KEY_AUDIO_ID_ARRAY,
                                         audioIds);
                    context.startService(musicIntent);
                }
            } catch (Exception e) {
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        // Reshedule this alarm.
        final int alarmId = intent.getIntExtra(AlarmColumns._ID, -1);
        Alarms.dismissAlarm(context, alarmId);
    }

    @Override
    public void addMyPreferences(final Context context,
                                 final PreferenceCategory category,
                                 final String extra) {
        Preference.OnPreferenceChangeListener prefChangeListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference p,
                                                  Object newValue) {
                    p.setSummary((String)newValue);
                    return true;
                }
            };

        ListPreference playlistPref = createPlaylistPreference(context);
        playlistPref.setKey(EXTRA_KEY_PLAYLIST_ID);
        playlistPref.setPersistent(true);
        playlistPref.setTitle(R.string.music_handler_playlist_title);
        playlistPref.setDialogTitle(R.string.music_handler_playlist_title);
        playlistPref.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference p, Object newValue) {
                    int id = Integer.parseInt((String)newValue);
                    p.setSummary(
                        getPlaylistName(p.getContext(), id));
                    return true;
                }
            });
        category.addPreference(playlistPref);
        if (!playlistPref.isEnabled()) {
            return;
        }

        // Get settings from extra.
        if (TextUtils.isEmpty(extra)) {
            playlistPref.setValueIndex(0);
            playlistPref.setSummary("");
        } else {
            Bundle result = getBundleFromExtra(extra);

            final int playlistId = result.getInt(EXTRA_KEY_PLAYLIST_ID);
            if(playlistId != -1) {
                playlistPref.setValue(String.valueOf(playlistId));
                String playlistName = getPlaylistName(context, playlistId);
                playlistPref.setSummary(playlistName);
            }
        }
    }

    @Override
    protected void putBundleIntoIntent(Intent intent, Bundle bundle) {
        final int playlistId = bundle.getInt(EXTRA_KEY_PLAYLIST_ID);
        if (playlistId != -1) {
            intent.putExtra(EXTRA_KEY_PLAYLIST_ID, playlistId);
        }
    }

    @Override
    protected Bundle getBundleFromExtra(String extra) {
        Bundle result = new Bundle();
        if (!TextUtils.isEmpty(extra)) {
            String[] values = TextUtils.split(extra, SEPARATOR);
            for (String value : values) {
                if (TextUtils.isEmpty(value) ||
                    !value.matches("(\\w+)=.*")) {
                    continue;
                }

                String[] elems = value.split("=");
                if (elems[0].equals(EXTRA_KEY_PLAYLIST_ID)) {
                    if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        try {
                            result.putInt(EXTRA_KEY_PLAYLIST_ID, Integer.parseInt(elems[1]));
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "===> unable to parse playlist id " + elems[1]);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static ListPreference createPlaylistPreference(Context context) {
        ListPreference preference = new ListPreference(context);
        preference.setSummary(context.getString(R.string.music_handler_warning_no_playlists));

        Cursor cursor = null;
        try {
            cursor =
                context.getContentResolver().query(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Playlists.NAME,
                                 MediaStore.Audio.Playlists._ID},
                    null, null, null);
            // No SD card inserted might happens

            final int count = cursor.getCount();
            if (count > 0) {
                CharSequence[] entries = new CharSequence[count];
                CharSequence[] entryValues = new CharSequence[count];
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int position = cursor.getPosition();
                        entries[position] = cursor.getString(0);
                        entryValues[position] = String.valueOf(cursor.getInt(1));
                    } while (cursor.moveToNext());

                    preference.setEntries(entries);
                    preference.setEntryValues(entryValues);
                    preference.setSummary("");
                    return preference;
                }
            }
        } catch (Exception e) {
            preference.setSummary(
                context.getString(R.string.no_external_storage_found));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.e(TAG, "createPlaylistPreference failed with uri " + MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI);
        preference.setEnabled(false);
        return preference;
    }

    private String getPlaylistName(Context context, final int id) {
        Cursor cursor =
            context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Playlists.NAME},
                MediaStore.Audio.Playlists._ID + "=" + id,
                null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            } else {
                Log.e(TAG, "getPlaylistName returned no rows.");
            }
        } finally {
            cursor.close();
        }

        // Log.e(TAG, "getPlaylistName failed with uri " + uri);
        throw new IllegalArgumentException("Unable to find a playlist ID");
    }

}
