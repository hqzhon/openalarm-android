package org.startsmall.openalarm;

import android.content.Intent;
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
    private static final String EXTRA_KEY_IS_LOOPING = "is_looping";
    private static final String EXTRA_KEY_MINUTES = "minutes";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "===> MusicHandler.onReceive()");

        final String extra = intent.getStringExtra(AlarmColumns.EXTRA);
        putBundleIntoIntent(intent, getBundleFromExtra(extra));

        final int playlistId = intent.getIntExtra(EXTRA_KEY_PLAYLIST_ID, -1);
        // boolean isLooping = intent.getBooleanExtra(EXTRA_KEY_IS_LOOPING, false);
        // int minutes = intent.getIntExtra(EXTRA_KEY_MINUTES, -1);

        if (playlistId != -1) {
            // Now, we have playlist id, we should start a
            // service to play members of this playlist.
            Uri memberUri =
                MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
            Cursor cursor =
                context.getContentResolver().query(
                    memberUri,
                    new String[]{MediaStore.Audio.Playlists.Members.AUDIO_ID,
                                 MediaStore.Audio.Playlists.Members.DATA},
                    null,
                    null,
                    MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int audioId = cursor.getInt(0);
                        String audioData = cursor.getString(1);

                        Log.d(TAG, "===> " + audioId + ": " + audioData);
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
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

                    Log.d(TAG, "======> " + (String)newValue);

                    int id = Integer.parseInt((String)newValue);
                    p.setSummary(
                        getPlaylistName(p.getContext(), id));
                    return true;
                }
            });
        category.addPreference(playlistPref);

        // // SMS subject
        // EditTextPreference subjectPref = new EditTextPreference(context);
        // subjectPref.setKey(EXTRA_KEY_SUBJECT);
        // subjectPref.setPersistent(true);
        // subjectPref.setTitle(R.string.textforme_handler_subject_title);
        // subjectPref.setOnPreferenceChangeListener(prefChangeListener);
        // subjectPref.setDialogTitle(R.string.textforme_handler_subject_dialog_title);
        // category.addPreference(subjectPref);

        // // SMS body
        // EditTextPreference bodyPref = new EditTextPreference(context);
        // bodyPref.setKey(EXTRA_KEY_BODY);
        // bodyPref.setPersistent(true);
        // bodyPref.setTitle(R.string.textforme_handler_body_title);
        // bodyPref.setOnPreferenceChangeListener(prefChangeListener);
        // bodyPref.setDialogTitle(R.string.textforme_handler_body_dialog_title);
        // category.addPreference(bodyPref);
        Log.d(TAG, "===> 1: " + extra);

        // Get settings from extra.
        if (TextUtils.isEmpty(extra)) {
            Log.d(TAG, "===> 21");
            playlistPref.setValueIndex(0);
            playlistPref.setSummary("");
            // subjectPref.setText("");
            // subjectPref.setSummary("");
            // bodyPref.setText("");
            // bodyPref.setSummary("");
            Log.d(TAG, "===> 22");
        } else {
            Bundle result = getBundleFromExtra(extra);

            Log.d(TAG, "===> 31");
            final int playlistId = result.getInt(EXTRA_KEY_PLAYLIST_ID);
            Log.d(TAG, "===> 32");
            if(playlistId != -1) {
                playlistPref.setValue(String.valueOf(playlistId));
                String playlistName = getPlaylistName(context, playlistId);
                playlistPref.setSummary(playlistName);
            }

            // String subjectString = result.getString(EXTRA_KEY_SUBJECT);
            // if(!TextUtils.isEmpty(subjectString)) {
            //     subjectPref.setText(subjectString);
            //     subjectPref.setSummary(subjectString);
            // }

            // String bodyString = result.getString(EXTRA_KEY_BODY);
            // if(!TextUtils.isEmpty(bodyString)) {
            //     bodyPref.setText(bodyString);
            //     bodyPref.setSummary(bodyString);
            // }
        }
        Log.d(TAG, "===> 3");
    }

    @Override
    protected void putBundleIntoIntent(Intent intent, Bundle bundle) {
        final int playlistId = bundle.getInt(EXTRA_KEY_PLAYLIST_ID);
        if (playlistId != -1) {
            intent.putExtra(EXTRA_KEY_PLAYLIST_ID, playlistId);
        }

        // final String subject = bundle.getString(EXTRA_KEY_SUBJECT);
        // if (!TextUtils.isEmpty(subject)) {
        //     intent.putExtra(EXTRA_KEY_SUBJECT, subject);
        // }

        // final String body = bundle.getString(EXTRA_KEY_BODY);
        // if (!TextUtils.isEmpty(body)) {
        //     intent.putExtra(EXTRA_KEY_BODY, body);
        // }
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

                // else if (elems[0].equals(EXTRA_KEY_SUBJECT)) {
                //     if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                //         result.putString(EXTRA_KEY_SUBJECT, elems[1]);
                //     }
                // } else if (elems[0].equals(EXTRA_KEY_BODY)) {
                //     if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                //         result.putString(EXTRA_KEY_BODY, elems[1]);
                //     }
                // }
            }
        }
        return result;
    }

    private static ListPreference createPlaylistPreference(Context context) {
        ListPreference preference = new ListPreference(context);
        preference.setSummary(context.getString(R.string.music_handler_warning_no_playlists));

        Cursor cursor =
            context.getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Playlists.NAME,
                             MediaStore.Audio.Playlists._ID},
                null, null, null);
        final int count = cursor.getCount();
        if (count > 0) {
            CharSequence[] entries = new CharSequence[count];
            CharSequence[] entryValues = new CharSequence[count];
            try {
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
            } finally {
                cursor.close();
            }

            Log.e(TAG, "createPlaylistPreference failed with uri " + MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI);
            throw new IllegalArgumentException("Unable to create Preference for playlist");
        }

        // This is the case ListPreference is not usable. Disable it first.
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
                Log.d(TAG, "===> getPlaylistName returns " + cursor.getString(0));
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
