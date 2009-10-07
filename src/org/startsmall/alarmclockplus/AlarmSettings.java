package org.startsmall.alarmclockplus;


import android.os.Bundle;
import android.preference.PreferenceActivity;

public class AlarmSettings extends PreferenceActivity {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.alarm_settings);
    }
}
