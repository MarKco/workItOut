package com.ilsecondodasinistra.workitout.classes;

import com.ilsecondodasinistra.workitout.database.PauseWorking;
import com.ilsecondodasinistra.workitout.utils.BadgeHelper;
import com.ilsecondodasinistra.workitout.utils.DatabaseHelper;
import com.ilsecondodasinistra.workitout.utils.SettingsWorkitout;

import org.joda.time.DateTime;

/**
 *  @author     Kevin Lucich (01/09/2014)
 */
public class SuperPauseWorking {

//    protected transient long id_pauseworking;
    protected transient Long id;
    protected transient long startDate;
    protected transient long endDate;
    protected transient boolean isLunch;
    protected transient Long id_sessionworking;

    //  Create new Pause, setting the session_id to current session
    public static PauseWorking newInstance(){
        return PauseWorking.newInstance(BadgeHelper.getCurrentSessionWorking().getId());
    }

    //  Create new Pause, setting a session_id
    public static PauseWorking newInstance( Long id_sessionworking ){
        return PauseWorking.newInstance(null, false);
    }

    /**
     *  Create new instance of PauseWorking
     *  @param  id_sessionworking   The id to assign the pause
     *  @param  isForLunch          This pause is for lunch
     *  @return Entity_PauseWorking
     *  @see com.ilsecondodasinistra.workitout.database.PauseWorking
     */
    public static PauseWorking newInstance( Long id_sessionworking, boolean isForLunch ){

        PauseWorking pause = new PauseWorking();
        pause.setStartDate(0);
        pause.setEndDate(0);
        pause.setLunch(isForLunch);
        pause.setId_sessionworking(id_sessionworking);

        //  Insert the new Pause Object into the DB :)
        DatabaseHelper.getDaoSession().getPauseWorkingDao().insert(pause);

        return pause;
    }

    /**
     *  Return the duration of pause, in milliseconds
     *  @see    #getDuration()
     */
    public long getDurationInMillis(){
        long minimum_duration = SettingsWorkitout.getMinimumLunchPause().getMillis();
        long duration = getEndDate() - getStartDate();
        if( duration < minimum_duration ){
            return minimum_duration;
        }
        return duration;
    }

    /**
     *  Return a Date Object with the duration of pause
     *  @see    #getDuration()
     */
    public DateTime getDuration(){
        return new DateTime(getDurationInMillis());
    }


    ///////////////////////////////////////////
    //  Getters and Setters

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public long getStartDate() {
        return startDate;
    }
    public void setStartDate( long startDateInMillis ) {
        this.startDate = startDateInMillis;
    }
    public void setStartDate( DateTime startDate ) {
        setStartDate( startDate.getMillis() );
    }

    public long getEndDate() {
        return endDate;
    }
    public void setEndDate(DateTime endDate) {
        setEndDate( endDate.getMillis() );
    }
    public void setEndDate( long endDateInMillis) {
        this.endDate = endDateInMillis;
    }

    public boolean isLunch() {
        return isLunch;
    }
    public void setLunch(boolean isLunch) {
        this.isLunch = isLunch;
    }
    /**
     * @deprecated
     */
    public void setIsLunch(boolean isLunch) {
        setLunch(isLunch);
    }

    public Long getId_sessionworking() {
        return id_sessionworking;
    }
    public void setId_sessionworking(Long id_sessionworking) {
        this.id_sessionworking = id_sessionworking;
    }
}
