package com.ilsecondodasinistra.workitout.classes;

import com.ilsecondodasinistra.workitout.database.PauseWorking;
import com.ilsecondodasinistra.workitout.database.SessionWorking;
import com.ilsecondodasinistra.workitout.utils.BadgeHelper;

import org.joda.time.Duration;
import org.joda.time.Period;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.lucichkevin.cip.Utils;

/**
 * Created by kevin on 01/09/2014.
 *
 * IMPORTANTE: COSA FARE DOPO AVER MODIFICATO LE CLASSI DI GreenDAO:
 * 1) Commentare le varie variabili all'inizio di ogni entity creata! :D
 *
 */
public class SuperSessionWorking {

//    protected transient long id_sessionworking;
    protected Long id;
    protected long entranceDate;
    protected long exitDate;
    protected List<PauseWorking> pauses;



    public static SessionWorking newInstance(){
        SessionWorking session = new SessionWorking();
        session.setEntranceDate(0);
        session.setExitDate(0);
        session.setPauses(new ArrayList<PauseWorking>());

        return session;
    }

    public PauseWorking getPauseOfLunch(){

        int indexOfLunchPause = getIndexOfPauseOfLunch();

        if( indexOfLunchPause == -1 ){
            PauseWorking pause = PauseWorking.newInstance(this.getId(), true);
            pauses.add(pause);
            return pause;
        }

        return getPauses().get(indexOfLunchPause);
    }

    public void setPauseOfLunch( PauseWorking pauseOfLunch ){

        int indexOfLunchPause = getIndexOfPauseOfLunch();

        //  Don't exists the pause of lunch
        if( indexOfLunchPause == -1 ){
            pauses.add(pauseOfLunch);
            return;
        }

        //  Pause of lunch already exists
        pauses.set(indexOfLunchPause, pauseOfLunch);
    }


    public int getIndexOfPauseOfLunch(){

        if( pauses == null ){
            pauses = new ArrayList<PauseWorking>();
            return -1;
        }

        int size = pauses.size();
        for( int i=0; i<size; i++ ) {
            if (pauses.get(i).isLunch()) {
                return i;
            }
        }
        return -1;
    }

//    public void setPause( int location, Entity_PauseWorking pause ){
//        pauses.set( location, pause );
//    }

    public void setPauses( ArrayList<PauseWorking> pauses ){
        this.pauses = pauses;
    }

    public void addPause( PauseWorking pause ){
        pauses.add(pause);
    }

    public Period getTimeWorked(){
        return new Duration( getExitDate() - getEntranceDate() ).toPeriod();
    }


    public Period getAllPausesDuration(){

        Duration duration = new Duration(0);

        for( PauseWorking pause : getPauses() ){
            duration = duration.plus( pause.getDurationInMillis() );
        }

        return duration.toPeriod();
    }

    public long calcExitTime(){

        //  Start
        long coundown = entranceDate;
        Utils.logger("entranceDate => "+ entranceDate, Utils.LOG_DEBUG );

        //  Hour to work
        coundown += BadgeHelper.getWorkTimeInMillis();

        //  Add the pause of lunch
        PauseWorking pauseLunch = getPauseOfLunch();
        coundown += pauseLunch.getDurationInMillis();

        return coundown;
    }

    /*
     *   Ritorno una stringa - HH:MM:SS oppure senza "-" se è straordinario
    */
    public String getReadableCountdownExitTime(){

        long millisOfExit = calcExitTime();

        //  Tempo di uscita è maggiore di adesso
        boolean isStraordinario = ( (new Date()).getTime() > millisOfExit );

        //  Se il tempo del CountDown
        String sign = (isStraordinario) ? "" : "-";

        long countDown = millisOfExit - (new Date()).getTime();

        return sign + (new SimpleDateFormat("HH:mm:ss")).format(new Date(countDown));
    }

    ///////////////////////////////////////////
    //  Getters and Setters

    public Long getId() {
        return id;
    }
    public void setId( Long id ){
        this.id = id;
    }

    public long getEntranceDate() {
        return entranceDate;
    }
    public void setEntranceDate(long entranceDate) {
        this.entranceDate = entranceDate;
    }

    public long getExitDate() {
        return exitDate;
    }
    public void setExitDate(long exitDate) {
        this.exitDate = exitDate;
    }

    public List<PauseWorking> getPauses() {
        if( pauses == null ){
            pauses = new ArrayList<PauseWorking>();
        }
        return pauses;
    }
    public void setPauses(List<PauseWorking> pauses) {
        this.pauses = pauses;
    }

}
