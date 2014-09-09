package com.ilsecondodasinistra.workitout.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.ilsecondodasinistra.workitout.database.DaoMaster;
import com.ilsecondodasinistra.workitout.database.DaoSession;

import de.greenrobot.dao.AbstractDaoMaster;

/**
 * Created by kevin on 01/09/2014.
 */
public class DatabaseHelper{

    private static SQLiteDatabase db = null;
    private static DaoSession daoSession = null;

    public static SQLiteDatabase getDatabase( Context context ){
        if( db == null ){
            DaoMaster.OpenHelper helper = new DaoMaster.OpenHelper( context, "workitout", null) {
                @Override
                public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ){
                    //  Do nothing...
                }
            };
            // Access the database using the helper
            db = helper.getWritableDatabase();
        }
        return db;
    }

    public static SQLiteDatabase getDatabase(){
        return db;
    }

    public static DaoSession getDaoSession(){
        return getDaoSession(false);
    }

    public static DaoSession getDaoSession( boolean reliable_session ){

        if( daoSession == null || reliable_session ){
            // Construct the DaoMaster which brokers DAOs for the Domain Objects
            DaoMaster daoMaster = new DaoMaster( getDatabase() );
            daoSession = daoMaster.newSession();
        }

        return daoSession;
    }

}
