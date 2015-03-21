package com.ilsecondodasinistra.workitout.utils;

import org.joda.time.Duration;

import it.lucichkevin.cip.preferencesmanager.PreferencesManager;

/**
 * @author Kevin Lucich (29/08/14)
 */
public class SettingsWorkitout extends PreferencesManager {

//    public static void setDefaultPreferences(){
//        PreferencesManager.setDefaultPreferences();
//    }

    //////////////////////////////////////
    //  Settings

    public static final String WORK_TIME = "work_time";
    public static final String PAUSE_DURATION = "pause_duration";  //  Minutes
    public static final String MINIMUM_PAUSE_DURATION = "minimum_pause_duration";  //  Minutes
    public static final String NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String PAUSE_COUNTED_FOR_EXIT = "pause_counted_for_exit";

    public static int getPauseDuration(int default_name) {
        return getPreferences().getInt(PAUSE_DURATION, default_name);
    }

    public static int getPauseDuration() {
        return getPauseDuration(15);
    }

    public static void setWorkTime(Duration work_time_duration) {
        //  Save in minutes
        setPreferences(WORK_TIME, ((int) work_time_duration.getMillis() / 60000));
    }

    public static Duration getWorkTime(int default_value) {
        int minutes = getPreferences().getInt(WORK_TIME, default_value);
        return new Duration(minutes * 60000);
    }

    public static Duration getWorkTime() {
        //  Default 8 ore :)
        return getWorkTime(480);
    }

    /**
     * Return the minimum duration of lunch pause
     */
    public static Duration getMinimumLunchPause(int default_minute) {
        int minutes = getPreferences().getInt(MINIMUM_PAUSE_DURATION, default_minute);
        return new Duration(minutes * 60000);
    }

    public static Duration getMinimumLunchPause() {
        return getMinimumLunchPause(30);
    }

    public static boolean isNotificationsEnabled(boolean default_value) {
        return getPreferences().getBoolean(NOTIFICATIONS_ENABLED, default_value);
    }

    public static boolean isNotificationsEnabled() {
        return isNotificationsEnabled(true);
    }

    public static boolean isPauseCounted(boolean default_value) {
        return getPreferences().getBoolean(PAUSE_COUNTED_FOR_EXIT, default_value);
    }

    public static boolean isPauseCounted() {
        return isNotificationsEnabled(false);
    }

}