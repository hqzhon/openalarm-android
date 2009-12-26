/**
 * @file   ApplicationSettings.java
 * @author josh <yenliangl at gmail dot com>
 * @date   Mon Dec  7 16:58:51 2009
 *
 * @brief
 *
 *
 */

package org.startsmall.openalarm;

import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;

public class ApplicationSettings extends PreferenceActivity {
    private static final String TAG = "AlarmSettings";

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        addPreferencesFromResource(R.xml.application_settings);

        PreferenceManager ps = getPreferenceManager();
        ListPreference themePreference =
            (ListPreference)ps.findPreference(
                getString(R.string.application_settings_set_theme_key));
        themePreference.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                                                  Object newValue) {
                    Toast.makeText(ApplicationSettings.this,
                                   R.string.application_settings_theme_changed_message,
                                   Toast.LENGTH_LONG)
                        .show();
                    return true;
                }
            });
    }
}
