package org.startsmall.openalarm;

import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceCategory;
import android.preference.Preference;
import android.provider.Contacts;
import android.text.method.DigitsKeyListener;
import android.text.TextUtils;
import android.util.Log;
import java.util.Calendar;

public class CallForMeHandler extends AbsHandler {

    private static final String TAG = "CallForMeHandler";
    private static final String EXTRA_KEY_PHONE_URI = "phone_uri";
    private static final String EXTRA_KEY_RING_UNTIL = "hangup_duration";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "===> CallForMeHandler.onReceive() start:" + Calendar.getInstance());

        final String extra = intent.getStringExtra("extra");
        putBundleIntoIntent(intent, getBundleFromExtra(extra));

        if (intent.hasExtra(EXTRA_KEY_PHONE_URI)) {
            Uri phoneUri = Uri.parse(intent.getStringExtra(EXTRA_KEY_PHONE_URI));
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
        PhonePreference phonePref = new PhonePreference(context);
        phonePref.setKey(EXTRA_KEY_PHONE_URI);
        phonePref.setPersistent(true);
        phonePref.setTitle("Phone number");
        phonePref.setOnPhonePickedListener(
            new PhonePreference.OnPhonePickedListener() {
                public void onPhonePicked(Preference preference, Uri uri) {
                    PhonePreference pref = (PhonePreference)preference;
                    pref.setPhoneUri(uri);
                    updateSummary(context, pref);
                }
            });
        category.addPreference(phonePref);

        // Hangup
        EditTextPreference ringUntilPref = new EditTextPreference(context);
        ringUntilPref.setPersistent(true);
        ringUntilPref.setTitle("Ring until ");
        ringUntilPref.setKey(EXTRA_KEY_RING_UNTIL);
        ringUntilPref.setDefaultValue("2");
        ringUntilPref.getEditText().setKeyListener(
            DigitsKeyListener.getInstance(false, false));
        ringUntilPref.setDialogTitle("Rings until minutes later");
        ringUntilPref.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference p, Object newValue) {
                    ((EditTextPreference)p).setSummary(
                        Integer.parseInt((String)newValue) + " minutes later");
                    return true;
                }
            });
        category.addPreference(ringUntilPref);

        // Get settings from extra.
        if(!TextUtils.isEmpty(extra)) {
            Bundle result = getBundleFromExtra(extra);

            String phoneUriString = result.getString(EXTRA_KEY_PHONE_URI);
            if(phoneUriString != null) {
                Uri phoneUri = Uri.parse(phoneUriString);
                phonePref.setPhoneUri(phoneUri);
                updateSummary(context, phonePref);
            }
        }
    }

    @Override
    protected void putBundleIntoIntent(Intent intent, Bundle bundle) {
        final String phoneUriString = bundle.getString(EXTRA_KEY_PHONE_URI);
        if (!TextUtils.isEmpty(phoneUriString)) {
            intent.putExtra(EXTRA_KEY_PHONE_URI, phoneUriString);
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
            } while (c.moveToNext());
        }
        c.close();
    }

    private void callPhone(Context context, Uri phoneUri) {
        Intent intent = new Intent(Intent.ACTION_CALL, phoneUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}


