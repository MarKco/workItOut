package com.ilsecondodasinistra.workitout.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.ilsecondodasinistra.workitout.NotificationService;

import java.util.Calendar;
import java.util.Date;

import it.lucichkevin.cip.Utils;

/**
 * Created by kevin on 03/09/2014.
 */
public class BadgeHelper {

    //  init => set all vars to the correct (se sono già entrato allora le setto all'ultimo valore, altrimenti cancello tutto)
    //  -   Se modifico l'ora di entrata e la differenza tra quella vecchia e quella nuova è superiore a X ore, prendo come se fosse un giorno nuovo

    private static Date entranceTime = new Date();          //  Where we'll be saving entrance time
    private static Date lunchInTime = new Date();
    private static Date lunchOutTime = new Date();
    private static Date exitTime = new Date();				//  Time you declare you're leaving
    private static Date estimatedExitTime = new Date();		//  Hour of the day you should leave the office
    private static Date extraTime = new Date();				//  Extra time elapsed, or to pass before end of the day
    private static boolean extraTimeSign = false;			//  true -> extratime positive ; false -> extratime negative
    private static boolean isTimerMarching = true;

    ////////////////////////////////////////////////////
    //  Helpers

    private static boolean isYesterday( Date dateToCheck ){
        Calendar c1 = Calendar.getInstance(); // today
        c1.add(Calendar.DAY_OF_YEAR, -1); // yesterday

        Calendar c2 = Calendar.getInstance();
        c2.setTime(dateToCheck); // your date

        return !(c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) < c2.get(Calendar.DAY_OF_YEAR));
    }

    private static void removeAlarm(){
        Intent i = new Intent( Utils.getContext(), NotificationService.class);
        PendingIntent pi = PendingIntent.getService(Utils.getContext(), 0, i, 0);
        AlarmManager mAlarm = (AlarmManager) Utils.getContext().getSystemService(Context.ALARM_SERVICE);

        mAlarm.cancel(pi);
        pi.cancel();
    }


    public static void updateEstimatedTimeOfExit(){

        Date work_time = SettingsWorkitout.getWorkTime();

        Calendar cal = Calendar.getInstance(); // creates calendar
        cal.setTime(new Date( BadgeHelper.getEntranceTime().getTime() )); // sets calendar time/date
        cal.add(Calendar.HOUR_OF_DAY, work_time.getHours()); // adds one hour
        cal.add(Calendar.MINUTE, work_time.getMinutes() );
        cal.add(Calendar.HOUR_OF_DAY, BadgeHelper.getLunchInTime().getHours());
        cal.add(Calendar.MINUTE, BadgeHelper.getLunchInTime().getMinutes());
        cal.add(Calendar.HOUR_OF_DAY, -(BadgeHelper.getLunchOutTime().getHours()));
        cal.add(Calendar.MINUTE, -(BadgeHelper.getLunchOutTime().getMinutes()));

        Utils.logger("updateEstimatedTimeOfExit() = "+ cal.get(Calendar.HOUR_OF_DAY) +" ore e "+ cal.get(Calendar.MINUTE) +" minuti", Utils.LOG_DEBUG );
        BadgeHelper.setEstimatedExitTime(cal.getTime());    // returns new date object, one hour in the future
    }

    public static void clearAllInput(){

        Date date = new Date(0);

        BadgeHelper.setEstimatedExitTime(date);
        BadgeHelper.setEntranceTime(date);
        BadgeHelper.setLunchInTime(date);
        BadgeHelper.setLunchOutTime(date);
        BadgeHelper.setExitTime(date);

//        SettingsWorkitout.setEntraceTime(0);
//        SettingsWorkitout.setLunchInTime(0);
//        SettingsWorkitout.setLunchOutTime(0);
//        SettingsWorkitout.setExitTime(0);
//        SettingsWorkitout.setExtraTimeHours(-1);
//        SettingsWorkitout.setExtraTimeMinutes(-1);
//        SettingsWorkitout.setExtraTimeSeconds(-1);
    }


    /////////////////////
    //  Getters and Setters

    public static Date getEntranceTime() {
        return entranceTime;
//        return new Date(SettingsWorkitout.getEntraceTime());
    }
    public static void setEntranceTime( Date entranceTime ){
        BadgeHelper.entranceTime = entranceTime;
//        SettingsWorkitout.setEntraceTime(entranceTime.getTime());
    }

    public static boolean isTimerMarching() {
        return isTimerMarching;
    }
    public static void setTimerMarching(boolean isTimerMarching) {
        BadgeHelper.isTimerMarching = isTimerMarching;
    }

    public static Date getLunchInTime() {
        return lunchInTime;
//        return new Date(SettingsWorkitout.getLunchInTime());
    }
    public static void setLunchInTime( Date lunchInTime ){
        BadgeHelper.lunchInTime = lunchInTime;
//        SettingsWorkitout.setLunchInTime(lunchInTime.getTime());
    }

    public static Date getLunchOutTime() {
        return lunchOutTime;
//        return new Date(SettingsWorkitout.getLunchOutTime());
    }
    public static void setLunchOutTime(Date lunchOutTime) {
        BadgeHelper.lunchOutTime = lunchOutTime;
//        SettingsWorkitout.setLunchOutTime(lunchOutTime.getTime());
    }

    public static Date getExitTime() {
        return BadgeHelper.exitTime;
//        return new Date(SettingsWorkitout.getExitTime());
    }
    public static void setExitTime(Date exitTime) {
        BadgeHelper.exitTime = exitTime;
//        SettingsWorkitout.setExitTime(exitTime.getTime());
    }

    public static Date getEstimatedExitTime() {
        return estimatedExitTime;
    }
    public static void setEstimatedExitTime(Date estimatedExitTime) {
        BadgeHelper.estimatedExitTime = estimatedExitTime;
    }

    public static Date getExtraTime() {
        return extraTime;
    }
    public static void setExtraTime(Date extraTime) {
        BadgeHelper.extraTime = extraTime;
    }

    public static boolean isExtraTimeSign() {
        return BadgeHelper.extraTimeSign;
//        return SettingsWorkitout.isExtraTimeSign();
    }
    public static void setExtraTimeSign( boolean extraTimeSign ){
        BadgeHelper.extraTimeSign = extraTimeSign;
//        SettingsWorkitout.isExtraTimeSign(extraTimeSign);
    }

}
