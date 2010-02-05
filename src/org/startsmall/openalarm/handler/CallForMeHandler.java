package org.startsmall.openalarm;

import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceCategory;
import android.preference.Preference;
import android.provider.Contacts;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.method.DigitsKeyListener;
import android.text.TextUtils;
import android.util.Log;
import java.util.Calendar;

public class CallForMeHandler extends AbsHandler {
    private static final String TAG = "CallForMeHandler";
    private static final String EXTRA_KEY_PHONE_URI = "phone_uri";
    private static final String EXTRA_KEY_SPEAKERPHONE = "speakerphone_on";
    private static final String EXTRA_KEY_VOICE_URI = "voice_uri";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "===> CallForMeHandler.onReceive() start:" + Calendar.getInstance());

        final String extra = intent.getStringExtra("extra");
        putBundleIntoIntent(intent, getBundleFromExtra(extra));

        if (intent.hasExtra(EXTRA_KEY_PHONE_URI)) {
            // Prepare MediaPlayer first;
            if (intent.hasExtra(EXTRA_KEY_VOICE_URI)) {
                String voiceUriString = intent.getStringExtra(EXTRA_KEY_VOICE_URI);
                MediaPlayer mp = prepareMediaPlayer(context,
                                                    Uri.parse(voiceUriString));
                if (mp != null) {
                    AudioManager am =
                        (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                    if (intent.hasExtra(EXTRA_KEY_SPEAKERPHONE)) {
                        boolean isSpeakerphoneOn =
                            intent.getBooleanExtra(EXTRA_KEY_SPEAKERPHONE, false);
                        am.setSpeakerphoneOn(isSpeakerphoneOn);
                    }

                    TelephonyManager tm =
                        (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                    tm.listen(new MyPhoneStateListener(am, tm, mp),
                              PhoneStateListener.LISTEN_CALL_STATE);
                }
            }

            // Make a phone call to specified person.
            Uri phoneUri =
                Uri.parse(intent.getStringExtra(EXTRA_KEY_PHONE_URI));
            callPhone(context, phoneUri);
        }

        // Reshedule this alarm.
        Intent scheduleIntent = new Intent(intent);
        scheduleIntent.setAction(Alarm.ACTION_SCHEDULE);
        // Clear previously defined component name so that this
        // BroadcastReceiver isn't called recursively.
        scheduleIntent.setComponent(null);
        context.sendBroadcast(scheduleIntent);

        Log.v(TAG, "===> CallForMeHandler.onReceive() end:" + Calendar.getInstance());
    }

    @Override
    public void addMyPreferences(final Context context,
                                 final PreferenceCategory category,
                                 final String extra) {
        // Phone number to call
        PhonePreference phonePref = new PhonePreference(context);
        phonePref.setKey(EXTRA_KEY_PHONE_URI);
        phonePref.setPersistent(true);
        phonePref.setTitle(R.string.callforme_handler_phone_number_title);
        phonePref.setOnPhonePickedListener(
            new PhonePreference.OnPhonePickedListener() {
                public void onPhonePicked(Preference preference, Uri uri) {
                    PhonePreference pref = (PhonePreference)preference;
                    pref.setPhoneUri(uri);
                    updateSummary(context, pref);
                }
            });
        category.addPreference(phonePref);

        // Speakerphone mode
        CheckBoxPreference speakerPhonePref = new CheckBoxPreference(context);
        speakerPhonePref.setKey(EXTRA_KEY_SPEAKERPHONE);
        speakerPhonePref.setPersistent(true);
        speakerPhonePref.setTitle(R.string.callforme_handler_speakerphone_title);
        speakerPhonePref.setSummaryOn(R.string.on);
        speakerPhonePref.setSummaryOff(R.string.off);
        category.addPreference(speakerPhonePref);

        // Ringtone to play in earpiece or speaker
        RingtonePreference voicePref = new RingtonePreference(context);
        voicePref.setShowDefault(false);
        voicePref.setShowSilent(false);
        voicePref.setTitle(R.string.ringtone_title);
        voicePref.setKey(EXTRA_KEY_VOICE_URI);
        voicePref.setPersistent(true);
        voicePref.setRingtoneType(RingtoneManager.TYPE_ALL);
        category.addPreference(voicePref);

        // Get settings from extra.
        if (TextUtils.isEmpty(extra)) {
            phonePref.setPhoneUri(null);
            phonePref.setSummary("");
            speakerPhonePref.setChecked(false);
            voicePref.setRingtoneUri(null);
            voicePref.setSummary("");
        } else {
            Bundle result = getBundleFromExtra(extra);

            String phoneUriString = result.getString(EXTRA_KEY_PHONE_URI);
            if(phoneUriString != null) {
                Uri phoneUri = Uri.parse(phoneUriString);
                phonePref.setPhoneUri(phoneUri);
                updateSummary(context, phonePref);
            }

            boolean isSpeakerphoneOn = result.getBoolean(EXTRA_KEY_SPEAKERPHONE);
            speakerPhonePref.setChecked(isSpeakerphoneOn);

            String voiceString = result.getString(EXTRA_KEY_VOICE_URI);
            if(voiceString != null) {
                Uri voiceUri = Uri.parse(voiceString);
                voicePref.setRingtoneUri(voiceUri);
                Ringtone ringtone = RingtoneManager.getRingtone(context, voiceUri);
                voicePref.setSummary(ringtone.getTitle(context));
            }
        }
    }

    @Override
    protected void putBundleIntoIntent(Intent intent, Bundle bundle) {
        final String phoneUriString = bundle.getString(EXTRA_KEY_PHONE_URI);
        if (!TextUtils.isEmpty(phoneUriString)) {
            intent.putExtra(EXTRA_KEY_PHONE_URI, phoneUriString);
        }

        final boolean isSpeakerphoneOn = bundle.getBoolean(EXTRA_KEY_SPEAKERPHONE);
        intent.putExtra(EXTRA_KEY_SPEAKERPHONE, isSpeakerphoneOn);

        final String voiceUriString = bundle.getString(EXTRA_KEY_VOICE_URI);
        if (!TextUtils.isEmpty(voiceUriString)) {
            intent.putExtra(EXTRA_KEY_VOICE_URI, voiceUriString);
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
                if (elems[0].equals(EXTRA_KEY_PHONE_URI)) {
                    if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        result.putString(EXTRA_KEY_PHONE_URI, elems[1]);
                    }
                } else if (elems[0].equals(EXTRA_KEY_SPEAKERPHONE)) {
                    boolean isSpeakerphoneOn = false;
                    if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        isSpeakerphoneOn = Boolean.parseBoolean(elems[1]);
                    }
                    result.putBoolean(EXTRA_KEY_SPEAKERPHONE, isSpeakerphoneOn);
                } else if (elems[0].equals(EXTRA_KEY_VOICE_URI)) {
                    if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        result.putString(EXTRA_KEY_VOICE_URI, elems[1]);
                    }
                }
            }
        }
        return result;
    }

    private void updateSummary(Context context, PhonePreference preference) {
        Uri phoneUri = preference.getPhoneUri();
        if (phoneUri == null) {
            return;
        }
        Cursor c = context.getContentResolver()
                   .query(phoneUri,
                          new String[]{Contacts.Phones.NAME,
                                       Contacts.Phones.NUMBER,},
                          null,
                          null,
                          Contacts.Phones.DEFAULT_SORT_ORDER);
        if (c.moveToFirst()) {
            do {
                final String name = c.getString(0);
                final String number = c.getString(1);
                preference.setSummary(name + " (" + number + ")");

                Log.d(TAG, "===> get " + name + "(" + number + ")");

            } while (c.moveToNext());
        }
        c.close();
    }

    private void callPhone(Context context, Uri phoneUri) {
        Intent intent = new Intent(Intent.ACTION_CALL, phoneUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        context.startActivity(intent);
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        private int mOldState = -1;
        private AudioManager mAudioManager;
        private MediaPlayer mMediaPlayer;
        private TelephonyManager mTelephonyManager;

        public MyPhoneStateListener(AudioManager am, TelephonyManager tm, MediaPlayer mp) {
            mAudioManager = am;
            mTelephonyManager = tm;
            mMediaPlayer = mp;
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            Log.d(TAG, "===> (current state, old state)=(" + state + ", " + mOldState + ")");
            switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                if (mOldState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    Log.d(TAG, "===> hung up!!..");

                    // Stop playing voice and restore audio
                    // routing to speaker.
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                    mAudioManager.setSpeakerphoneOn(false);
                    mTelephonyManager.listen(this,
                                             PhoneStateListener.LISTEN_NONE);                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (mOldState == TelephonyManager.CALL_STATE_RINGING ||
                    mOldState == TelephonyManager.CALL_STATE_IDLE) {
                    Log.d(TAG, "===> picked up!!..");

                    // Start play voice in earpiece, not in speaker.
                    // mAudioManager.setSpeakerphoneOn(false);
                    mMediaPlayer.start();
                }
                break;

            default:
                break;
            }
            mOldState = state;
        }
    }

    private MediaPlayer prepareMediaPlayer(Context context, Uri audioUri) {
        // final float IN_CALL_VOLUME = 0.125f;
        if (audioUri == null || TextUtils.isEmpty(audioUri.toString())) {
            return null;
        }

        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(context, audioUri);
        } catch (java.io.IOException e) {
            return null;
        }

        // mp.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
        mp.setLooping(true);

        try {
            mp.prepare();
        } catch (java.io.IOException e) {
            return null;
        }

        return mp;
    }
}
