package com.ilsecondodasinistra.workitout;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "", // will not be used
        mailTo = "ilsecondodasinistra@gmail.com", //
        customReportContent = {ReportField.APP_VERSION_CODE, ReportField.PACKAGE_NAME,
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

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}