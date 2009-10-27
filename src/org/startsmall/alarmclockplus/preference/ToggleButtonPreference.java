/**
 * @file   ToggleButtonPreference.java
 * @author yenliangl <yenliangl@gmail.com>
 * @date   Thu Oct  8 19:49:43 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.alarmclockplus.preference;

import org.startsmall.alarmclockplus.*;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.ToggleButton;

public class ToggleButtonPreference extends MyPreference {
    public ToggleButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(
            R.layout.alarm_toggle_button_preference_widget);
        setDefaultValue("false");
    }

    public void setChecked(boolean state) {
        setPreferenceValue(state);
    }

    public boolean isChecked() {
        return ((Boolean)getPreferenceValue()).booleanValue();
    }

    @Override
    protected Object parsePreferenceValue(String value) {
        return Boolean.parseBoolean(value);
    }

    @Override
    protected String toPreferenceValue(Object obj) {
        return ((Boolean)obj).toString();
    }

    @Override
    protected void onClick() {
        boolean newValue = !isChecked();
        // if (!callChangeListener(newValue)) {
        //     return;
        // }
        setChecked(newValue);
        super.onClick();
    }

    @Override
    protected void onBindView(View view) {
        final ToggleButton toggle =
            (ToggleButton)view.findViewById(R.id.toggle);
        toggle.setChecked(isChecked());
        super.onBindView(view);
    }

    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        view.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View view) {
                    ToggleButtonPreference.this.onClick();
                }
            });
        return view;
    }
}
