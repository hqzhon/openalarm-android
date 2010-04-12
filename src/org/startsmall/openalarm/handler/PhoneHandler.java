package org.startsmall.openalarm;

import android.content.Intent;
import android.content.Context;
import android.content.ContentResolver;
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
import android.telephony.TelephonyManager;
import android.text.method.DialerKeyListener;
import android.text.TextUtils;
import android.util.Log;

public class PhoneHandler extends AbsHandler {
    private static final String TAG = "PhoneHandler";
    static final String EXTRA_KEY_PERSON_URI = "person_uri";
    static final String EXTRA_KEY_SPEAKERPHONE_MODE = "speakerphone_on";
    static final String EXTRA_KEY_VOICE_URI = "voice_uri";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Log.i(TAG, "===> PhoneHandler.onReceive()");

        final String extra = intent.getStringExtra("extra");
        putBundleIntoIntent(intent, getBundleFromExtra(extra));

        String personUriString = intent.getStringExtra(EXTRA_KEY_PERSON_URI);
        if (!TextUtils.isEmpty(personUriString)) {
            boolean speakerphoneOn = intent.getBooleanExtra(EXTRA_KEY_SPEAKERPHONE_MODE, false);
            if (speakerphoneOn) {
                String voiceUriString = intent.getStringExtra(EXTRA_KEY_VOICE_URI);
                if (!TextUtils.isEmpty(voiceUriString)) {
                    MediaPlayer mp = prepareMediaPlayer(context, Uri.parse(voiceUriString));
                    if (mp != null) {
                        AudioManager am =
                            (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                        am.setSpeakerphoneOn(speakerphoneOn);

                        TelephonyManager tm =
                            (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                        tm.listen(new PhoneStateListener(am, tm, mp), PhoneStateListener.LISTEN_CALL_STATE);
                    }
                }
            }

            // Make a call to the person.
            makeCall(context, personUriString);
        }

        final int alarmId = intent.getIntExtra(AlarmColumns._ID, -1);
        Alarms.dismissAlarm(context, alarmId);
    }

    @Override
    public void addMyPreferences(final Context context,
                                 final PreferenceCategory category,
                                 final String extra) {
        // Pick up a person to call
        PeoplePreference personPref = new PeoplePreference(context);
        personPref.setKey(EXTRA_KEY_PERSON_URI);
        personPref.setPersistent(true);
        personPref.setTitle(R.string.phone_number_title);
        personPref.setOnPersonSelectedListener(
            new PeoplePreference.OnPersonSelectedListener() {
                @Override
                public void onPersonSelected(Preference p, Uri personUri) {
                    PersonalInfo info = getPersonalInfo(context, personUri);
                    if (info != null) {
                        String summary =
                            info.name + " (" +
                            (TextUtils.isEmpty(info.number) ? context.getString(R.string.phone_handler_no_number) :
                                                              info.number) +
                            ")";
                        p.setSummary(summary);
                    }
                }
            });
        category.addPreference(personPref);

        // Speakerphone mode
        CheckBoxPreference speakerPhonePref = new CheckBoxPreference(context);
        speakerPhonePref.setKey(EXTRA_KEY_SPEAKERPHONE_MODE);
        speakerPhonePref.setPersistent(true);
        speakerPhonePref.setTitle(R.string.phone_handler_speakerphone_title);
        speakerPhonePref.setSummaryOn(R.string.on);
        speakerPhonePref.setSummaryOff(R.string.off);
        category.addPreference(speakerPhonePref);

        // Ringtone to play in earpiece or speaker
        RingtonePreference voicePref = new RingtonePreference(context);
        category.addPreference(voicePref);
        voicePref.setShowDefault(false);
        voicePref.setShowSilent(false);
        voicePref.setTitle(R.string.ringtone_title);
        voicePref.setKey(EXTRA_KEY_VOICE_URI);
        voicePref.setPersistent(true);
        voicePref.setRingtoneType(RingtoneManager.TYPE_ALL);
        voicePref.setDependency(EXTRA_KEY_SPEAKERPHONE_MODE);

        // Get settings from extra.
        if (TextUtils.isEmpty(extra)) {
            personPref.setSummary("");
            personPref.setPersonUri(null);
            speakerPhonePref.setChecked(false);
            voicePref.setRingtoneUri(null);
            voicePref.setSummary("");
        } else {
            Bundle result = getBundleFromExtra(extra);

            // Fill values in
            String personUriString = result.getString(EXTRA_KEY_PERSON_URI);
            if(!TextUtils.isEmpty(personUriString)) {
                Uri personUri = Uri.parse(personUriString);
                personPref.setPersonUri(personUri);

                PersonalInfo info = getPersonalInfo(context, personUri);
                if (info != null) {
                    String summary =
                        info.name + " (" +
                        (TextUtils.isEmpty(info.number) ? context.getString(R.string.phone_handler_no_number) :
                                                          info.number) +
                        ")";
                    personPref.setSummary(summary);
                }
            }

            boolean isSpeakerphoneOn = result.getBoolean(EXTRA_KEY_SPEAKERPHONE_MODE);
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
        final String personUriString = bundle.getString(EXTRA_KEY_PERSON_URI);
        if (!TextUtils.isEmpty(personUriString)) {
            intent.putExtra(EXTRA_KEY_PERSON_URI, personUriString);
        }

        final boolean isSpeakerphoneOn = bundle.getBoolean(EXTRA_KEY_SPEAKERPHONE_MODE);
        intent.putExtra(EXTRA_KEY_SPEAKERPHONE_MODE, isSpeakerphoneOn);

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
                if (elems[0].equals(EXTRA_KEY_PERSON_URI)) {
                    if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        result.putString(EXTRA_KEY_PERSON_URI, elems[1]);
                    }
                } else if (elems[0].equals(EXTRA_KEY_SPEAKERPHONE_MODE)) {
                    boolean isSpeakerphoneOn = false;
                    if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        isSpeakerphoneOn = Boolean.parseBoolean(elems[1]);
                    }
                    result.putBoolean(EXTRA_KEY_SPEAKERPHONE_MODE, isSpeakerphoneOn);
                } else if (elems[0].equals(EXTRA_KEY_VOICE_URI)) {
                    if(elems.length == 2 && !TextUtils.isEmpty(elems[1])) {
                        result.putString(EXTRA_KEY_VOICE_URI, elems[1]);
                    }
                }
            }
        }
        return result;
    }

    // Get default phone number of the person if it exists. If it
    // doesn't exist, get first number instead.
    private void makeCall(Context context, String personUriString) {
        PersonalInfo info = getPersonalInfo(context, Uri.parse(personUriString));
        if (info != null && !TextUtils.isEmpty(info.number)) {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + info.number));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            context.startActivity(intent);
        }
    }

    private class PhoneStateListener extends android.telephony.PhoneStateListener {
        private int mOldState = -1;
        private AudioManager mAudioManager;
        private MediaPlayer mMediaPlayer;
        private TelephonyManager mTelephonyManager;

        public PhoneStateListener(AudioManager am, TelephonyManager tm, MediaPlayer mp) {
            mAudioManager = am;
            mTelephonyManager = tm;
            mMediaPlayer = mp;
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            // Log.d(TAG, "===> (current state, old state)=(" + state + ", " + mOldState + ")");
            switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                if (mOldState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // Log.d(TAG, "===> hung up!!..");

                    // Stop playing voice and restore audio
                    // routing to speaker.
                    mMediaPlayer.stop();
                    mMediaPlayer.release();

                    // No need to revert speaker phone
                    // mode. Android should do it for you.

                    mTelephonyManager.listen(this, LISTEN_NONE);
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (mOldState == TelephonyManager.CALL_STATE_RINGING ||
                    mOldState == TelephonyManager.CALL_STATE_IDLE) {

                    // Log.d(TAG, "===> picked up!!.." + mAudioManager.isSpeakerphoneOn());
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
        if (audioUri == null || TextUtils.isEmpty(audioUri.toString())) {
            return null;
        }

        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(context, audioUri);
        } catch (java.io.IOException e) {
            return null;
        }

        mp.setLooping(true);

        try {
            mp.prepare();
        } catch (java.io.IOException e) {
            return null;
        }

        return mp;
    }

    private PersonalInfo getPersonalInfo(Context context, final Uri personUri) {
        boolean validUri = false;
        String name = null;
        int primaryNumberId = -1;
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(personUri,
                                 new String[]{Contacts.Phones.DISPLAY_NAME, Contacts.People.PRIMARY_PHONE_ID},
                                 null, null, Contacts.Phones.DEFAULT_SORT_ORDER);
        if (cursor == null) {
            return null;
        }

        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
            primaryNumberId = cursor.getInt(1);
        }
        cursor.close();

        Uri phoneNumberUri = null;
        // If there is no primary number for this person, loop
        // his/her numbers and pick up first one.
        if (primaryNumberId > 0) {
            // Synthesize phone number uri directly is far
            // efficient than querying phone number from phones
            // database.
            phoneNumberUri = Uri.parse(Contacts.Phones.CONTENT_URI + "/" + primaryNumberId);
        } else {                // No primary number.. call the first number I can find.
            phoneNumberUri = Uri.parse(personUri.toString() + "/" + Contacts.People.Phones.CONTENT_DIRECTORY);
        }

        String phoneNumber = null;
        cursor = cr.query(phoneNumberUri,
                          new String[]{Contacts.People.NUMBER},
                          null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String telNo = cursor.getString(0);
            cursor.close();
            if (!TextUtils.isEmpty(telNo)) {
                // phoneNumber = Uri.parse("tel:" + telNo);
                phoneNumber = telNo;
            }
        }

        if (!TextUtils.isEmpty(name)) {
            return new PersonalInfo(name, phoneNumber);
        }

        return null;
    }

    class PersonalInfo {
        public final String name;
        public final String number;
        PersonalInfo(final String name, final String phoneNumber) {
            this.name = name;
            this.number = phoneNumber;
        }
    }
}
