package com.ilsecondodasinistra.workitout;

import android.app.Application;

import com.ilsecondodasinistra.workitout.database.Entity_PauseWorking;
import com.ilsecondodasinistra.workitout.database.Entity_SessionWorking;
import com.ilsecondodasinistra.workitout.utils.DatabaseHelper;

import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import it.lucichkevin.cip.Utils;
import it.lucichkevin.cip.preferencesmanager.PreferencesManager;


@ReportsCrashes(
    formKey = "",   // will not be used
    mailTo = "ilsecondodasinistra@gmail.com",
    customReportContent = {
        ReportField.APP_VERSION_CODE, ReportField.PACKAGE_NAME, ReportField.ANDROID_VERSION,
        ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT
    },
    mode = ReportingInteractionMode.TOAST,
    resToastText = R.string.ACRACrash
)

public class MyApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

        Utils.init(getBaseContext());
        PreferencesManager.setDebugLog(true);

//        insertFakeData();

		// The following line triggers the initialization of ACRA
		ACRA.init(this);
	}


    private void insertFakeData(){

//        ArrayList<Entity_SessionWorking> ssss = (ArrayList<Entity_SessionWorking>) DatabaseHelper.getDaoSession().getEntity_SessionWorkingDao().queryBuilder().list();
//        ArrayList<Entity_PauseWorking> pppp = (ArrayList<Entity_PauseWorking>) DatabaseHelper.getDaoSession().getEntity_PauseWorkingDao().queryBuilder().list();
//
//        int i = 0;
//        String idsPause = "";
//        for( Entity_PauseWorking p : pppp ){
//            idsPause += ","+ p.getId();
//            i++;
//        }
//        i = 0;
//        String idsSessiosn = "";
//        for( Entity_SessionWorking s : ssss ){
//            idsSessiosn += ","+ s.getId();
//            i++;
//        }
//
//        Utils.logger("IDS SESSION = "+ idsSessiosn, Utils.LOG_DEBUG );
//        Utils.logger("IDS PAUSES = "+ idsPause, Utils.LOG_DEBUG );

        Random random = new Random();

        Calendar calendar = Calendar.getInstance();
        long today = calendar.getTimeInMillis();

        Calendar sessionCalendar = Calendar.getInstance();
        sessionCalendar.set(Calendar.DAY_OF_MONTH, 1);

        ArrayList<Entity_SessionWorking> sessions = new ArrayList<Entity_SessionWorking>();
        ArrayList<Entity_PauseWorking> pauses = new ArrayList<Entity_PauseWorking>();

        while(true){

            if( sessionCalendar.getTimeInMillis() >= today ){
                break;
            }
            sessionCalendar.set( Calendar.MINUTE, 0 );
            sessionCalendar.set( Calendar.SECOND, 0 );

            Entity_SessionWorking session = Entity_SessionWorking.newInstance();
            DatabaseHelper.getDaoSession().getEntity_SessionWorkingDao().insert( session );

            Entity_PauseWorking pause = Entity_PauseWorking.newInstance( session.getId(), true );

            //  9:00
            sessionCalendar.set( Calendar.HOUR_OF_DAY, 9 );
            session.setEntranceDate( sessionCalendar.getTime() );

            //  13:00
            sessionCalendar.set( Calendar.HOUR_OF_DAY, 13 );
            pause.setEndDate( sessionCalendar.getTime() );

            //  12:XX
            sessionCalendar.set( Calendar.MINUTE, (random.nextInt(29)+30) );
            sessionCalendar.set( Calendar.HOUR_OF_DAY, 12 );
            pause.setStartDate( sessionCalendar.getTime() );

            //  17:XX
            sessionCalendar.set( Calendar.HOUR_OF_DAY, 17 );
            session.setExitDate( sessionCalendar.getTime() );

            sessions.add(session);
            pauses.add(pause);

            sessionCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Utils.logger("sessions.size() = "+ sessions.size(), Utils.LOG_DEBUG );
        Utils.logger("pauses.size() = "+ pauses.size(), Utils.LOG_DEBUG );

        DatabaseHelper.getDaoSession().getEntity_PauseWorkingDao().insertOrReplaceInTx( pauses );
        DatabaseHelper.getDaoSession().getEntity_SessionWorkingDao().updateInTx( sessions );

    }
}
