package org.startsmall.alarmclockplus.receiver;

import org.startsmall.alarmclockplus.R;
import org.startsmall.alarmclockplus.preference.TextViewPreference;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.util.Log;

public class WifiActionHandler extends ActionHandler {
    private static final String TAG = "WifiActionHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "=========> My WifiActionHandler.onReceive() haha");

    }

    public void addMyPreferences(Context context,
                                 PreferenceCategory category,
                                 String defaultValue) {
        class MyPreference extends TextViewPreference {
            public MyPreference(Context context, AttributeSet attrs) {
                super(context, attrs);
            }

            protected void onClick() {
                getDialog().show();
            }

            protected void onPrepareDialogBuilder(
                AlertDialog.Builder builder) {
                int checkedItemIndex = -1;
                if(getPreferenceValue() != null) {
                    checkedItemIndex = getPreferenceValue() == "On" ? 0 : 1;
                }

                builder
                    .setSingleChoiceItems(
                        new CharSequence[] {"On", "Off"},
                        checkedItemIndex,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                MyPreference.this.setPreferenceValue(
                                    which == 0? "On" : "Off");
                                dialog.dismiss();
                            }
                        });
            }
        }

        // class MyPreference extends Preference {
        //     public MyPreference(Context context, AttributeSet attrs) {
        //         super(context, attrs);

        //         setWidgetLayoutResource(R.layout.alarm_toggle_button_preference_widget);
        //         setDefaultValue("On");
        //     }

            // protected void onPrepareDialogBuilder(
            //     AlertDialog.Builder builder) {
            //     int checkedItemIndex = -1;
            //     if(getPreferenceValue() != null) {
            //         checkedItemIndex = getPreferenceValue() == "On" ? 0 : 1;
            //     }

            //     builder
            //         .setSingleChoiceItems(
            //             new CharSequence[] {"On", "Off"},
            //             checkedItemIndex,
            //             new DialogInterface.OnClickListener() {
            //                 public void onClick(DialogInterface dialog,
            //                                     int which) {
            //                     MyPreference.this.setPreferenceValue(
            //                         which == 0? "On" : "Off");
            //                     dialog.dismiss();
            //                 }
            //             });
            // }
        // }

        MyPreference onOffPref = new MyPreference(context, null);
        onOffPref.setKey("wifi_state");
        onOffPref.setPersistent(true);
        onOffPref.setTitle(R.string.alarm_extra_settings_wifi_title);
        onOffPref.setPreferenceValue("On");


        Log.d(TAG, "==========> default value=" + defaultValue);


        category.addPreference(onOffPref);
    }

    private void setWifiEnabled(Context context, boolean toggle) {
        WifiManager wm =
            (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if(wm.isWifiEnabled() != toggle) {
            wm.setWifiEnabled(toggle);
        }
    }
}
