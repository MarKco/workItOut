package com.ilsecondodasinistra.workitout.classes;

import com.ilsecondodasinistra.workitout.database.Entity_PauseWorking;
import com.ilsecondodasinistra.workitout.database.Entity_SessionWorking;
import com.ilsecondodasinistra.workitout.utils.SettingsWorkitout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    protected Date entranceDate;
    protected Date exitDate;
    protected List<Entity_PauseWorking> pauses;



    public static Entity_SessionWorking newInstance(){
        Date tmp = new Date(0);

        Entity_SessionWorking session = new Entity_SessionWorking();
        session.setEntranceDate(tmp);
        session.setExitDate(tmp);
        session.setPauses(new ArrayList<Entity_PauseWorking>());

        return session;
    }

    public Entity_PauseWorking getPauseOfLunch(){

        int indexOfLunchPause = getIndexOfPauseOfLunch();

        if( indexOfLunchPause == -1 ){
            Entity_PauseWorking pause = Entity_PauseWorking.newInstance( this.getId(), true);
            pauses.add(pause);
            return pause;
        }

        return getPauses().get(indexOfLunchPause);
    }

    public void setPauseOfLunch( Entity_PauseWorking pauseOfLunch ){

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

    public void setPauses( ArrayList<Entity_PauseWorking> pauses ){
        this.pauses = pauses;
    }

    public void addPause( Entity_PauseWorking pause ){
        pauses.add(pause);
    }

    public Date getTimeWorked(){
        return new Date( getExitDate().getTime() - getEntranceDate().getTime() );
    }

    public long calcExitTime(){

        Calendar coundown = Calendar.getInstance();
        coundown.setTime(entranceDate);
        coundown.add(Calendar.MINUTE, ((SettingsWorkitout.getWorkTime().getHours()*60) + SettingsWorkitout.getWorkTime().getMinutes()) );  //  Aggiungo l'ora di uscita

        Entity_PauseWorking pauseLunch = getPauseOfLunch();

        int pauseEnd = pauseLunch.getEndDate().getHours()*60 + pauseLunch.getEndDate().getMinutes();
        int pauseStart = pauseLunch.getStartDate().getHours()*60 + pauseLunch.getStartDate().getMinutes();

        coundown.add(Calendar.MINUTE, (pauseEnd-pauseStart) );

        return coundown.getTimeInMillis();
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

    public Date getEntranceDate() {
        return entranceDate;
    }
    public void setEntranceDate(Date entranceDate) {
        this.entranceDate = entranceDate;
    }

    public Date getExitDate() {
        return exitDate;
    }
    public void setExitDate(Date exitDate) {
        this.exitDate = exitDate;
    }

    public List<Entity_PauseWorking> getPauses() {
        return pauses;
    }
    public void setPauses(List<Entity_PauseWorking> pauses) {
        this.pauses = pauses;
    }

}
