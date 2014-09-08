package com.ilsecondodasinistra.workitout.classes;

import java.util.Date;

/**
 * Created by kevin on 01/09/2014.
 */
public class PauseWorking {

    protected int id_pauseworking;
    protected java.util.Date start_date;
    protected java.util.Date end_date;
    protected int id_sessionworking;


    ///////////////////////////////////////////
    //  Getters and Setters


    public int getId_pauseworking() {
        return id_pauseworking;
    }
    public void setId_pauseworking(int id_pauseworking) {
        this.id_pauseworking = id_pauseworking;
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

    public int getId_sessionworking() {
        return id_sessionworking;
    }
    public void setId_sessionworking(int id_sessionworking) {
        this.id_sessionworking = id_sessionworking;
    }

}
