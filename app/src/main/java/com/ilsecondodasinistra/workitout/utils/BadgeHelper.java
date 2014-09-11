package com.ilsecondodasinistra.workitout.utils;

import com.ilsecondodasinistra.workitout.database.Entity_PauseWorking;
import com.ilsecondodasinistra.workitout.database.Entity_SessionWorking;
import com.ilsecondodasinistra.workitout.database.Entity_SessionWorkingDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.lucichkevin.cip.preferencesmanager.PreferencesManager;

/**
 * Created by kevin on 03/09/2014.
 */
public class BadgeHelper {

    private static Date estimatedExitTime = new Date();		//  Hour of the day you should leave the office
    private static Date extraTime = new Date();				//  Extra time elapsed, or to pass before end of the day
    private static boolean extraTimeSign = false;			//  true -> extratime positive ; false -> extratime negative
    private static boolean isTimerMarching = true;

    private static Entity_SessionWorking CURRENT_SESSION = null;

    ////////////////////////////////////////////////////
    //  Helpers


    public static Entity_SessionWorking getNewSessionWorking(){
        Entity_SessionWorking session = Entity_SessionWorking.newInstance();

        //  Insert new new session into the database
        DatabaseHelper.getDaoSession().getEntity_SessionWorkingDao().insert(session);

        //  Do something... init?
        return session;
    }

    //  Finchè non memorizziamo i dati nel Database non possiamo salvare più di una sessione alla volta
    public static Entity_SessionWorking getLastSessionWorking(){

        //  This is first run
        if( PreferencesManager.isFirstRun() ){
            return getNewSessionWorking();
        }

        Entity_SessionWorkingDao sessionDao = DatabaseHelper.getDaoSession().getEntity_SessionWorkingDao();

        List<Entity_SessionWorking> sessions = sessionDao.queryBuilder().orderDesc(Entity_SessionWorkingDao.Properties.Id).limit(1).list();

        Entity_SessionWorking last = sessions.get(0);

        if( last.getPauses() == null ){
            last.setPauses( new ArrayList<Entity_PauseWorking>() );
        }

        //  Get last row of sessionWoriking
        return last;
    }

    //  Now the session must build manually (vars there are in SettingsWorkitout: SharedPreferences), in the future this will be get from the SQLite
    public static Entity_SessionWorking getCurrentSessionWorking(){

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
        DatabaseHelper.getDaoSession().getEntity_PauseWorkingDao().updateInTx(CURRENT_SESSION.getPauses());

        //  Save Session
        DatabaseHelper.getDaoSession().getEntity_SessionWorkingDao().update(CURRENT_SESSION);
    }

    /////////////////////
    //  Getters and Setters

    public static Entity_PauseWorking getPauseOfLunch(){
        return getCurrentSessionWorking().getPauseOfLunch();
    }

    public static Date getEntranceTime() {
        return getCurrentSessionWorking().getEntranceDate();
    }
    public static void setEntranceTime( Date entranceTime ){
        CURRENT_SESSION.setEntranceDate(entranceTime);
    }

    public static Date getLunchInTime() {
        return getPauseOfLunch().getEndDate();
    }
    public static void setLunchInTime( Date lunchInTime ){
        Entity_PauseWorking lunchPause = CURRENT_SESSION.getPauseOfLunch();
        lunchPause.setEndDate(lunchInTime);
        CURRENT_SESSION.setPauseOfLunch( lunchPause );
    }

    public static Date getLunchOutTime() {
        return getPauseOfLunch().getStartDate();
    }
    public static void setLunchOutTime( Date lunchOutTime ){
        Entity_PauseWorking lunchPause = CURRENT_SESSION.getPauseOfLunch();
        lunchPause.setStartDate( lunchOutTime );
        CURRENT_SESSION.setPauseOfLunch(lunchPause);
    }

    public static Date getExitTime() {
        return getCurrentSessionWorking().getExitDate();
    }
    public static void setExitTime(Date exitTime) {
        CURRENT_SESSION.setExitDate(exitTime);
    }


    public static boolean isTimerMarching() {
        return isTimerMarching;
    }
    public static void setTimerMarching(boolean isTimerMarching) {
        BadgeHelper.isTimerMarching = isTimerMarching;
    }

    public static void addPause( Date start ){
        long pauseInMillis = SettingsWorkitout.getPauseDuration() * 60 * 1000;  //   break duration
        BadgeHelper.addPause( start, new Date(start.getTime() + pauseInMillis) );
    }
    public static void addPause( Date start, Date end ){
        Entity_PauseWorking pause = Entity_PauseWorking.newInstance();
        pause.setStartDate(start);
        pause.setEndDate(end);
        BadgeHelper.addPause(pause);
    }
    public static void addPause( Entity_PauseWorking pause ){
        CURRENT_SESSION.addPause(pause);
    }

}
