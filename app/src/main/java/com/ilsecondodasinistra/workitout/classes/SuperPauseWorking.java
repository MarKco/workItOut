package com.ilsecondodasinistra.workitout.classes;

import com.ilsecondodasinistra.workitout.database.Entity_PauseWorking;
import com.ilsecondodasinistra.workitout.utils.BadgeHelper;
import com.ilsecondodasinistra.workitout.utils.DatabaseHelper;

import java.util.Date;

/**
 * Created by kevin on 01/09/2014.
 */
public class SuperPauseWorking {

//    protected transient long id_pauseworking;
    protected transient Long id;
    protected transient Date startDate;
    protected transient Date endDate;
    protected transient boolean isLunch;
    protected transient Long id_sessionworking;

    //  Create new Pause, setting the session_id to current session
    public static Entity_PauseWorking newInstance(){
        return Entity_PauseWorking.newInstance( BadgeHelper.getCurrentSessionWorking().getId() );
    }

    //  Create new Pause, setting a session_id
    public static Entity_PauseWorking newInstance( Long id_sessionworking ){
        return Entity_PauseWorking.newInstance(null,false);
    }

    /**
     *  Create new instance of PauseWorking
     *  @param  id_sessionworking   The id to assign the pause
     *  @param  isForLunch          This pause is for lunch
     *  @return Entity_PauseWorking
     *  @see Entity_PauseWorking
     */
    public static Entity_PauseWorking newInstance( Long id_sessionworking, boolean isForLunch ){
        Date tmp = new Date(0);

        Entity_PauseWorking pause = new Entity_PauseWorking();
        pause.setStartDate(tmp);
        pause.setEndDate(tmp);
        pause.setIsLunch(isForLunch);
        pause.setId_sessionworking(id_sessionworking);

        //  Insert the new Pause Object into the DB :)
        DatabaseHelper.getDaoSession().getEntity_PauseWorkingDao().insert(pause);

        return pause;
    }

    /**
     *  Return the duration of pause, in milliseconds
     *  @see    #getDuration()
     */
    public long getDurationInMillis(){
        return getEndDate().getTime() - getStartDate().getTime();
    }

    /**
     *  Return a Date Object with the duration of pause
     *  @see    #getDuration()
     */
    public Date getDuration(){
        return new Date(getDurationInMillis());
    }


    ///////////////////////////////////////////
    //  Getters and Setters

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Date getStartDate() {
        return startDate;
    }
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isLunch() {
        return isLunch;
    }
    public void setLunch(boolean isLunch) {
        this.isLunch = isLunch;
    }

    public Long getId_sessionworking() {
        return id_sessionworking;
    }
    public void setId_sessionworking(Long id_sessionworking) {
        this.id_sessionworking = id_sessionworking;
    }
}
