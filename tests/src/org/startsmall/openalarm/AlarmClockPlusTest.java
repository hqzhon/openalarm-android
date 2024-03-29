package org.startsmall.alarmclockplus;

import android.test.ActivityInstrumentationTestCase;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class org.startsmall.alarmclockplus.AlarmClockPlusTest \
 * org.startsmall.alarmclockplus.tests/android.test.InstrumentationTestRunner
 */
public class AlarmClockPlusTest extends ActivityInstrumentationTestCase<AlarmClockPlus> {

    public AlarmClockPlusTest() {
        super("org.startsmall.alarmclockplus", AlarmClockPlus.class);
    }

}
