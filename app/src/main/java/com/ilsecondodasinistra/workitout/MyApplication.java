package com.ilsecondodasinistra.workitout;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

import it.lucichkevin.cip.Utils;
import it.lucichkevin.cip.preferencesmanager.PreferencesManager;


@ReportsCrashes(formKey = "", // will not be used
mailTo = "ilsecondodasinistra@gmail.com", //
customReportContent = { ReportField.APP_VERSION_CODE, ReportField.PACKAGE_NAME,
		ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL,
		ReportField.CUSTOM_DATA, ReportField.STACK_TRACE /*
														 * , ReportField.LOGCAT
														 */}, //
mode = ReportingInteractionMode.TOAST, //
resToastText = R.string.ACRACrash)
public class MyApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();

        Utils.init(getBaseContext());
        PreferencesManager.init(getBaseContext());
        PreferencesManager.setDebugLog(true);

		// The following line triggers the initialization of ACRA
//		ACRA.init(this);
	}
}