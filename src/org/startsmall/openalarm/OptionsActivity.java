package org.startsmall.openalarm;

import android.app.Dialog;
import android.os.Bundle;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class OptionsActivity extends PreferenceActivity {
    private static final String TAG = "OptionsActivity";

    private SharedPreferences mSharedPreferences;
    private ListPreference mSideButtonBehavior;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Intent i = getIntent();

        addPreferencesFromResource(R.xml.options);

        // setVolumeControlStream(AudioManager.STREAM_ALARM);

        PreferenceManager pm = getPreferenceManager();
        mSharedPreferences = pm.getSharedPreferences();
    }
}
