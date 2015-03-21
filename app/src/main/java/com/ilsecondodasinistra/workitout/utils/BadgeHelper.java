package com.ilsecondodasinistra.workitout.utils;

import com.ilsecondodasinistra.workitout.database.PauseWorking;
import com.ilsecondodasinistra.workitout.database.SessionWorking;
import com.ilsecondodasinistra.workitout.database.SessionWorkingDao;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.ArrayList;
import java.util.List;

import it.lucichkevin.cip.Utils;
import it.lucichkevin.cip.preferencesmanager.PreferencesManager;

/**
 *  @author     Kevin Lucich (03/09/2014).
 */
public class BadgeHelper {

    private static boolean isTimerMarching = true;
    private static SessionWorking CURRENT_SESSION = null;

    //  Così da non dover ricalcorare OGNI SECONDO (timer per l'uscita/straordinari) il tempo che lavoro ogni giorno
    private static Long WORK_TIME_CACHE = null;



    ////////////////////////////////////////////////////
    //  Helpers

    /**
     * Check if date provided is yesterday
     * @returns true if it's yesterday (or before), false otherwise
     */
    public static boolean isYesterday( DateTime dateToCheck ){

        DateTime yesterday = DateTime.now().minusDays(1);
        return !(yesterday.getYear() == dateToCheck.getYear()  && yesterday.getDayOfYear() < dateToCheck.getDayOfYear() );
    }

    public static SessionWorking getNewSessionWorking(){
        SessionWorking session = SessionWorking.newInstance();

        //  Insert new new session into the database
        DatabaseHelper.getDaoSession().getSessionWorkingDao().insert(session);

        //  Do something... init?
        return session;
    }

    //  Finchè non memorizziamo i dati nel Database non possiamo salvare più di una sessione alla volta
    public static SessionWorking getLastSessionWorking(){

        //  This is first run
        if( PreferencesManager.isFirstRun() ){
            return getNewSessionWorking();
        }

        SessionWorkingDao sessionDao = DatabaseHelper.getDaoSession().getSessionWorkingDao();

        List<SessionWorking> sessions = sessionDao.queryBuilder().orderDesc(SessionWorkingDao.Properties.Id).limit(1).list();

        SessionWorking last = sessions.get(0);

        if( last.getPauses() == null ){
            last.setPauses( new ArrayList<PauseWorking>() );
        }

        //  Get last row of sessionWoriking
        return last;
    }

    //  Now the session must build manually (vars there are in SettingsWorkitout: SharedPreferences), in the future this will be get from the SQLite
    public static SessionWorking getCurrentSessionWorking(){

        if( CURRENT_SESSION == null ){
            CURRENT_SESSION = getLastSessionWorking();
        }

        return CURRENT_SESSION;
    }

    public static void clearAllInput(){
        CURRENT_SESSION = getNewSessionWorking();
    }

    //  Quando tocco una qualsiasi variabile di una mia sessione lavorativa (SessionWorking), aggiorno la sorgente (SQLite o SharedPreferences)
    public static void saveCurrentSession() {
        //  Save pauses
        DatabaseHelper.getDaoSession().getPauseWorkingDao().updateInTx(CURRENT_SESSION.getPauses());

        //  Save Session
        DatabaseHelper.getDaoSession().getSessionWorkingDao().update(CURRENT_SESSION);
    }

    public static long getWorkTimeInMillis() {
        if( BadgeHelper.WORK_TIME_CACHE == null ){
            BadgeHelper.WORK_TIME_CACHE = SettingsWorkitout.getWorkTime().getMillis();
        }
        return BadgeHelper.WORK_TIME_CACHE;
    }

    public static void setWorkTime( Duration duration ){
        SettingsWorkitout.setWorkTime(duration);
        BadgeHelper.WORK_TIME_CACHE = null;
    }


    /////////////////////
    //  Getters and Setters

    public static PauseWorking getPauseOfLunch(){
        return getCurrentSessionWorking().getPauseOfLunch();
    }

    public static DateTime getEntranceTime() {
        return new DateTime(getCurrentSessionWorking().getEntranceDate());
    }
    public static void setEntranceTime( DateTime entranceTime ){
        BadgeHelper.setEntranceTime(entranceTime.getMillis());
    }
    public static void setEntranceTime( long entranceTimeInMillis ){
        CURRENT_SESSION.setEntranceDate(entranceTimeInMillis);
    }

    public static DateTime getLunchInTime() {
        return new DateTime(getPauseOfLunch().getEndDate());
    }
    public static void setLunchInTime( DateTime lunchInTime ){
        BadgeHelper.setLunchInTime( lunchInTime.getMillis() ) ;
    }
    public static void setLunchInTime( long lunchInTimeInMillis ){
        PauseWorking lunchPause = CURRENT_SESSION.getPauseOfLunch();
        lunchPause.setEndDate( lunchInTimeInMillis );
        CURRENT_SESSION.setPauseOfLunch( lunchPause );
    }

    public static DateTime getLunchOutTime() {
        return new DateTime(getPauseOfLunch().getStartDate());
    }
    public static void setLunchOutTime( DateTime lunchOutTime ){
        BadgeHelper.setLunchOutTime(lunchOutTime.getMillis());
    }
    public static void setLunchOutTime( long lunchOutTimeInMillis ){
        PauseWorking lunchPause = CURRENT_SESSION.getPauseOfLunch();
        lunchPause.setStartDate( lunchOutTimeInMillis );
        CURRENT_SESSION.setPauseOfLunch(lunchPause);
    }

    public static DateTime getExitTime() {
        return new DateTime(getCurrentSessionWorking().getExitDate());
    }
    public static void setExitTime( DateTime exitTime ){
        BadgeHelper.setExitTime(exitTime.getMillis());
    }
    public static void setExitTime( long exitTimeInMillis ){
        CURRENT_SESSION.setExitDate( exitTimeInMillis );
    }


    public static boolean isTimerMarching() {
        return isTimerMarching;
    }
    public static void setTimerMarching(boolean isTimerMarching) {
        BadgeHelper.isTimerMarching = isTimerMarching;
    }

    public static void addPause( DateTime start ){
        long pauseInMillis = SettingsWorkitout.getPauseDuration() * 60000;  //   break duration
        BadgeHelper.addPause( start, new DateTime(start.getMillis() + pauseInMillis) );
    }
    public static void addPause( DateTime start, DateTime end ){
        PauseWorking pause = PauseWorking.newInstance();
        pause.setStartDate( start.getMillis() );
        pause.setEndDate( end.getMillis() );
        BadgeHelper.addPause(pause);
    }
    public static void addPause( PauseWorking pause ){
        CURRENT_SESSION.addPause(pause);
    }

}
