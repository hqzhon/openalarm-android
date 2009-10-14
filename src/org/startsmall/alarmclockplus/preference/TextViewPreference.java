/**
 * @file   TextViewPreference.java
 * @author yenliangl <josh@alchip.com>
 * @date   Thu Oct  8 19:49:43 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.alarmclockplus.preference;

import org.startsmall.alarmclockplus.*;
import android.content.Context;
//import android.content.res.TypedArray;
//import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
//import android.util.Log;

public abstract class TextViewPreference extends Preference {
    public TextViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.alarm_text_view_preference_widget);
    }

    public void setPreferenceValue(Object value) {
        persistValue(value);
        notifyChanged();
    }

    public Object getPreferenceValue() {
        return getPersistedValue();
    }

    protected abstract void persistValue(Object value);
    protected abstract Object getPersistedValue();
}
