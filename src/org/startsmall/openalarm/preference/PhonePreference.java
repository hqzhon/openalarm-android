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

public class PhonePreference extends RingtonePreference {

    public interface OnPhonePickedListener {
        void onPhonePicked(Preference preference, Uri uri);
    }

    private OnPhonePickedListener mPhonePickedListener;

    public PhonePreference(Context context) {
        super(context);
    }

    public void setOnPhonePickedListener(OnPhonePickedListener listener) {
        if (listener != null) {
            mPhonePickedListener = listener;
        }
    }

    @Override
    protected void onPrepareRingtonePickerIntent(Intent intent) {
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType(Contacts.Phones.CONTENT_ITEM_TYPE);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (super.onActivityResult(requestCode, resultCode, data)) {
            if (data != null) {
                Uri uri = data.getData();
                if (mPhonePickedListener != null) {
                    mPhonePickedListener.onPhonePicked(this, uri);
                }
            }
            return true;
        }
        return false;
    }

    public void setPhoneUri(Uri uri) {
        onSaveRingtone(uri);
    }

    public Uri getPhoneUri() {
        String uriString = getPersistedString("");
        return TextUtils.isEmpty(uriString) ? null : Uri.parse(uriString);
    }
}

