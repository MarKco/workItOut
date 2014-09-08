package com.ilsecondodasinistra.workitout.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import com.ilsecondodasinistra.workitout.database.Entity_SessionWorking;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table SessionsWorking.
*/
public class Entity_SessionWorkingDao extends AbstractDao<Entity_SessionWorking, Integer> {

    public static final String TABLENAME = "SessionsWorking";

    /**
     * Properties of entity Entity_SessionWorking.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id_sessionworking = new Property(0, int.class, "id_sessionworking", true, "ID_SESSIONWORKING");
        public final static Property Start_date = new Property(1, java.util.Date.class, "start_date", false, "START_DATE");
        public final static Property End_date = new Property(2, java.util.Date.class, "end_date", false, "END_DATE");
    };

    private DaoSession daoSession;


    public Entity_SessionWorkingDao(DaoConfig config) {
        super(config);
    }
    
    public Entity_SessionWorkingDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
        this.daoSession = daoSession;
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'SessionsWorking' (" + //
                "'ID_SESSIONWORKING' INTEGER PRIMARY KEY NOT NULL ," + // 0: id_sessionworking
                "'START_DATE' INTEGER NOT NULL ," + // 1: start_date
                "'END_DATE' INTEGER NOT NULL );"); // 2: end_date
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'SessionsWorking'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, Entity_SessionWorking entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getId_sessionworking());
        stmt.bindLong(2, entity.getStart_date().getTime());
        stmt.bindLong(3, entity.getEnd_date().getTime());
    }

    @Override
    protected void attachEntity(Entity_SessionWorking entity) {
        super.attachEntity(entity);
        entity.__setDaoSession(daoSession);
    }

    /** @inheritdoc */
    @Override
    public Integer readKey(Cursor cursor, int offset) {
        return cursor.getInt(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public Entity_SessionWorking readEntity(Cursor cursor, int offset) {
        Entity_SessionWorking entity = new Entity_SessionWorking( //
            cursor.getInt(offset + 0), // id_sessionworking
            new java.util.Date(cursor.getLong(offset + 1)), // start_date
            new java.util.Date(cursor.getLong(offset + 2)) // end_date
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, Entity_SessionWorking entity, int offset) {
        entity.setId_sessionworking(cursor.getInt(offset + 0));
        entity.setStart_date(new java.util.Date(cursor.getLong(offset + 1)));
        entity.setEnd_date(new java.util.Date(cursor.getLong(offset + 2)));
     }
    
    /** @inheritdoc */
    @Override
    protected Integer updateKeyAfterInsert(Entity_SessionWorking entity, long rowId) {
        return entity.getId_sessionworking();
    }
    
    /** @inheritdoc */
    @Override
    public Integer getKey(Entity_SessionWorking entity) {
        if(entity != null) {
            return entity.getId_sessionworking();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}