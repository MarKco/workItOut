package com.ilsecondodasinistra.workitout.classes;

import com.ilsecondodasinistra.workitout.database.DaoSession;
import com.ilsecondodasinistra.workitout.database.Entity_PauseWorking;
import com.ilsecondodasinistra.workitout.database.Entity_SessionWorkingDao;

import java.util.Date;
import java.util.List;

/**
 * Created by kevin on 01/09/2014.
 */
public class SessionWorking {

    protected int id_sessionworking;
    protected java.util.Date start_date;
    protected java.util.Date end_date;
    protected List<Entity_PauseWorking> pauses;




    ///////////////////////////////////////////
    //  Getters and Setters

    public int getId_sessionworking() {
        return id_sessionworking;
    }
    public void setId_sessionworking( int id_sessionworking ){
        this.id_sessionworking = id_sessionworking;
    }

    public Date getStartDate() {
        return start_date;
    }
    public void setStartDate(Date start_date) {
        this.start_date = start_date;
    }

    public Date getEndDate() {
        return end_date;
    }
    public void setEndDate(Date end_date) {
        this.end_date = end_date;
    }

    public List<Entity_PauseWorking> getPauses() {
        return pauses;
    }
    public void setPauses(List<Entity_PauseWorking> pauses) {
        this.pauses = pauses;
    }

}
