package org.startsmall.openalarm;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.text.TextUtils;
import android.util.Log;

public class PeoplePreference extends RingtonePreference {
    private static final String TAG = "PhoneNumberPreference";
    private OnPersonSelectedListener mPersonSelectedListener;

    public PeoplePreference(Context context) {
        super(context);
    }

    public void setOnPersonSelectedListener(OnPersonSelectedListener listener) {
        if (listener != null) {
            mPersonSelectedListener = listener;
        }
    }

    @Override
    protected void onPrepareRingtonePickerIntent(Intent intent) {
        intent.setAction(Intent.ACTION_PICK);
        intent.setType(Contacts.People.CONTENT_TYPE);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (super.onActivityResult(requestCode, resultCode, data)) {
            if (data != null) {
                Uri uri = data.getData();
                setPersonUri(uri);
                if (mPersonSelectedListener != null) {
                    mPersonSelectedListener.onPersonSelected(this, uri);
                }
                return true;
            }
        }
        return false;
    }

    public void setPersonUri(Uri uri) {
        onSaveRingtone(uri);
    }

    public Uri getPersonUri() {
        String uriString = getPersistedString("");
        return TextUtils.isEmpty(uriString) ? null : Uri.parse(uriString);
    }

    static interface OnPersonSelectedListener {
        void onPersonSelected(Preference preference, Uri uri);
    }
}
