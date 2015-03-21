package com.ilsecondodasinistra.workitout.database;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.DaoException;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table SessionsWorking.
 */
public class SessionWorking extends com.ilsecondodasinistra.workitout.classes.SuperSessionWorking  implements java.io.Serializable {

//    private Long id;
//    private long entranceDate;
//    private long exitDate;

    /** Used to resolve relations */
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    private transient SessionWorkingDao myDao;

//    private List<PauseWorking> pauses;

    public SessionWorking() {

    }

    public SessionWorking(Long id) {
        this.id = id;
    }

    public SessionWorking(Long id, long entranceDate, long exitDate) {
        this.id = id;
        this.entranceDate = entranceDate;
        this.exitDate = exitDate;
    }

    /** called by internal mechanisms, do not call yourself. */
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getSessionWorkingDao() : null;
    }

    /** To-many relationship, resolved on first access (and after reset). Changes to to-many relations are not persisted, make changes to the target entity. */
    public List<PauseWorking> getPauses() {
        if (pauses == null) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            PauseWorkingDao targetDao = daoSession.getPauseWorkingDao();
            List<PauseWorking> pausesNew = targetDao._querySessionWorking_Pauses(id);
            synchronized (this) {
                if(pauses == null) {
                    pauses = pausesNew;
                }
            }
        }
        return pauses;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    public synchronized void resetPauses() {
        pauses = null;
    }

    /** Convenient call for {@link AbstractDao#delete(Object)}. Entity must attached to an entity context. */
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.delete(this);
    }

    /** Convenient call for {@link AbstractDao#update(Object)}. Entity must attached to an entity context. */
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.update(this);
    }

    /** Convenient call for {@link AbstractDao#refresh(Object)}. Entity must attached to an entity context. */
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.refresh(this);
    }

}
