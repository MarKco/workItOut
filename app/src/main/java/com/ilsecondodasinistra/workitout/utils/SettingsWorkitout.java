package com.ilsecondodasinistra.workitout.utils;

import org.joda.time.Duration;

import it.lucichkevin.cip.preferencesmanager.PreferencesManager;

/**
 * Created by kevin on 29/08/14.
 */
public class SettingsWorkitout extends PreferencesManager {

//    public static void setDefaultPreferences(){
//        PreferencesManager.setDefaultPreferences();
//    }

    private static final String WORK_TIME = "work_time";

    //////  TEMPORANEE
    private static final String TIMER_MARCHING = "timer_marching";

//    private static final String EXTRA_TIME_HOURS = "extra_time_hours";
//    private static final String EXTRA_TIME_MINUTES = "extra_time_minutes";
//    private static final String EXTRA_TIME_SECONDS = "extra_time_seconds";

    ////////////////////////////////////////////



    //////////////////////////////////////
    //  Settings

    public static final String PAUSE_DURATION = "pause_duration";  //  Minutes

    public static void setPauseDuration( int pauseDuration ){
        setPreferences(PAUSE_DURATION, pauseDuration );
    }
    public static int getPauseDuration( int default_name ){
        return getPreferences().getInt(PAUSE_DURATION, default_name );
    }
    public static int getPauseDuration(){
        return getPauseDuration(15);
    }

    public static void setWorkTime( Duration work_time_duration ){
        //  Save in milliseconds
        setPreferences(WORK_TIME, work_time_duration.getMillis() );
    }
    public static Duration getWorkTime( long default_value ){
        long millis = getPreferences().getLong(WORK_TIME, default_value );
        return new Duration(millis);
    }
    public static Duration getWorkTime(){
        //  Default 8 ore :)
        return getWorkTime(28800000);
    }

    public static void setTimerMarching( boolean is_timer_marching ){
        setPreferences(TIMER_MARCHING,is_timer_marching);
    }
    public static boolean isTimerMarching( boolean default_value ){
        return getPreferences().getBoolean(TIMER_MARCHING, default_value);
    }
    public static boolean isTimerMarching(){
        return isTimerMarching(true);
    }

    /////////////////////////////////////////////////
    //  Extra time

//    public static void setExtraTimeHours( long extra_time_hours ){
//        setPreferences(EXTRA_TIME_HOURS,extra_time_hours);
//    }
//    public static long getExtraTimeHours( long default_value ){
//        return getPreferences().getLong(EXTRA_TIME_HOURS, default_value);
//    }
//    public static long getExtraTimeHours(){
//        return getExtraTimeHours(0);
//    }

//    public static void setExtraTimeMinutes( long extra_time_minutes ){
//        setPreferences(EXTRA_TIME_MINUTES,extra_time_minutes);
//    }
//    public static long getExtraTimeMinutes( long default_value ){
//        return getPreferences().getLong(EXTRA_TIME_MINUTES, default_value);
//    }
//    public static long getExtraTimeMinutes(){
//        return getExtraTimeMinutes(-1);
//    }

//    public static void setExtraTimeSeconds( long extra_time_seconds ){
//        setPreferences(EXTRA_TIME_SECONDS,extra_time_seconds);
//    }
//    public static long getExtraTimeSeconds( long default_value ){
//        return getPreferences().getLong(EXTRA_TIME_SECONDS, default_value);
//    }
//    public static long getExtraTimeSeconds(){
//        return getExtraTimeSeconds(-1);
//    }

//    public static void setLunchOutTime( long lunch_out_time ){
//        setPreferences(LUNCH_OUT_TIME,lunch_out_time);
//    }
//    public static long getLunchOutTime( long default_value ){
//        return getPreferences().getLong(LUNCH_OUT_TIME, default_value);
//    }
//    public static long getLunchOutTime(){
//        return getLunchOutTime(0);
//    }

//    public static void setLunchInTime( long lunch_in_time ){
//        setPreferences(LUNCH_IN_TIME,lunch_in_time);
//    }
//    public static long getLunchInTime( long default_value ){
//        return getPreferences().getLong(LUNCH_IN_TIME, default_value);
//    }
//    public static long getLunchInTime(){
//        return getLunchInTime(0);
//    }

//    public static void setEntranceTime( long v ){
//        setPreferences(ENTRANCE_TIME,v);
//    }
//    public static long getEntranceTime( long default_value ){
//        return getPreferences().getLong(ENTRANCE_TIME, default_value);
//    }
//    public static long getEntranceTime(){
//        return getEntranceTime(0);
//    }

//    public static void setExitTime( long v ){
//        setPreferences(EXIT_TIME,v);
//    }
//    public static long getExitTime( long default_value ){
//        return getPreferences().getLong(EXIT_TIME, default_value);
//    }
//    public static long getExitTime(){
//        return getExitTime(0);
//    }

}
