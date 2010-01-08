/**
 * @file   TextViewPreference.java
 * @author yenliangl <yenliangl@gmail.com>
 * @date   Thu Oct  8 19:49:43 2009
 *
 * @brief
 *
 *
 */
package org.startsmall.openalarm;

import android.content.Context;
import android.util.AttributeSet;
//import android.util.Log;
import android.view.View;
import android.widget.TextView;

class TextViewPreference extends MyPreference {
    protected TextViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.alarm_text_view_preference_widget);
    }

    protected Object parsePreferenceValue(String value) {
        return value;
    }

    protected String toPreferenceValue(Object obj) {
        return (String)obj;
    }

    protected String transformValueBeforeDisplay(Object value) {
        return (String)value;
    }

    protected String formatPersistedValue(String value) {
        return value;
    }

    @Override
    protected void onBindView(View view) {
        // Summary related MUST be dealt with before super.onBindView();

        super.onBindView(view);

        final TextView textView = (TextView)view.findViewById(R.id.text);
        textView.setText(
            transformValueBeforeDisplay(getPreferenceValue()));
    }
}
