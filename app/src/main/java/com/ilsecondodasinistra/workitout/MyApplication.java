package com.ilsecondodasinistra.workitout;

import android.app.Application;

import com.ilsecondodasinistra.workitout.database.PauseWorking;
import com.ilsecondodasinistra.workitout.database.SessionWorking;
import com.ilsecondodasinistra.workitout.utils.DatabaseHelper;

import org.acra.ACRA;
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

        Random random = new Random();

        Calendar calendar = Calendar.getInstance();
        long today = calendar.getTimeInMillis();

        Calendar sessionCalendar = Calendar.getInstance();
        sessionCalendar.set(Calendar.DAY_OF_MONTH, 1);

        ArrayList<SessionWorking> sessions = new ArrayList<SessionWorking>();
        ArrayList<PauseWorking> pauses = new ArrayList<PauseWorking>();

        while(true){

            if( sessionCalendar.getTimeInMillis() >= today ){
                break;
            }

            sessionCalendar.set( Calendar.MINUTE, 0 );
            sessionCalendar.set( Calendar.SECOND, 0 );

            SessionWorking session = SessionWorking.newInstance();
            DatabaseHelper.getDaoSession().getSessionWorkingDao().insert( session );

            PauseWorking pause = PauseWorking.newInstance(session.getId(), true);

            //  9:00
            sessionCalendar.set( Calendar.HOUR_OF_DAY, 9 );
            session.setEntranceDate( sessionCalendar.getTimeInMillis() );

            //  13:00
            sessionCalendar.set( Calendar.HOUR_OF_DAY, 13 );
            pause.setEndDate( sessionCalendar.getTimeInMillis() );

            //  12:XX
            sessionCalendar.set( Calendar.MINUTE, (random.nextInt(29)+30) );
            sessionCalendar.set( Calendar.HOUR_OF_DAY, 12 );
            pause.setStartDate( sessionCalendar.getTimeInMillis() );

            //  17:XX
            sessionCalendar.set( Calendar.HOUR_OF_DAY, 17 );
            session.setExitDate( sessionCalendar.getTimeInMillis() );

            sessions.add(session);
            pauses.add(pause);

            sessionCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Utils.logger("sessions.size() = "+ sessions.size(), Utils.LOG_DEBUG );
        Utils.logger("pauses.size() = "+ pauses.size(), Utils.LOG_DEBUG );

        DatabaseHelper.getDaoSession().getPauseWorkingDao().insertOrReplaceInTx( pauses );
        DatabaseHelper.getDaoSession().getSessionWorkingDao().updateInTx( sessions );

    }
}
