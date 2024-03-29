package org.startsmall.openalarm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.*;

class HandlerInfo implements Comparable<HandlerInfo> {
    public static final String EXTRA_KEY_LABEL = AlarmColumns.LABEL;
    public static final String EXTRA_KEY_HANDLER = AlarmColumns.HANDLER;
    public static final String EXTRA_KEY_ICON = "icon";

    char sortKey;
    String label;
    String className;
    Drawable icon;

    public String toString() {
        return label;
    }

    static HashMap<String, HandlerInfo> getMap(Context context) {
        HashMap<String, HandlerInfo> map = new HashMap<String, HandlerInfo>();

        PackageManager pm = context.getPackageManager();

        Iterator<ResolveInfo> handlerInfos = Alarms.queryAlarmHandlers(pm, true).iterator();
        while (handlerInfos.hasNext()) {
            ActivityInfo activityInfo = handlerInfos.next().activityInfo;
            String key = activityInfo.name;

            HandlerInfo info = new HandlerInfo();
            String label = activityInfo.loadLabel(pm).toString();
            info.sortKey = label.charAt(0);
            info.label = label.substring(1);
            info.className = activityInfo.name;
            info.icon = activityInfo.loadIcon(pm);
            map.put(key, info);
        }

        return map;
    }

    public Intent getIntent() {
        Intent i = new Intent();
        i.putExtra(EXTRA_KEY_LABEL, label);
        i.putExtra(EXTRA_KEY_HANDLER, className);

        return i;
    }

    public int compareTo(HandlerInfo another) {
        if (sortKey < another.sortKey) {
            return -1;
        } else if (sortKey == another.sortKey) {
            return className.compareTo(another.className);
        } else {
            return 1;
        }
    }

    public boolean equals(HandlerInfo another) {
        return className.equals(another.className);
    }
}
