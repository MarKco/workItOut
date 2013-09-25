package com.ilsecondodasinistra.workitout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * This class is used in order to perform activities which are triggered
 * on the main activity. Whatever operation calls triggerObservers
 * will be reflected in a call in WorkItOutMain IF this class has been linked to
 * WorkItOutMain class with a set like the following:
 *       TimingsObservable to = new TimingsObservable();
      	 to.addObserver((WorkItOutMain)context);
      	 to.updateWorkDayLength(context);
 * It's used for example to update all the fields in main activity
 * once workday length has been changed.
 * CustomTimeDialog -> instantiate observable -> links observer and observable
 * (see above three lines), calls method here. Method here triggers observer
 * once the procedure is over, therefore data is updates. No scope issues. 
 * @author marco
 *
 */
public class TimingsObservable extends Observable {

	private static SimpleDateFormat hhmmFormatter = new SimpleDateFormat("H:mm");
	private static SimpleDateFormat hhmmssFormatter = new SimpleDateFormat("H:mm:ss");
	private static SimpleDateFormat longDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	
	static boolean DEBUG = false;
	
	static Date workTime;
	
	/***
	 * updateWorkDayLength calculated workDay length after the
	 * date picker has been filled with data
	 */
	public void updateWorkDayLength(Context context)
	{		
		triggerObservers();
	}	
	
	
	
	
	private static void logIt(String message)
	{
		if (DEBUG)
		{
			Log.i("workitout", message);
		}
	}

    // Create a method to update the Observerable's flag to true for changes and
    // notify the observers to check for a change. These are also a part of the
    // secret sauce that makes Observers and Observables communicate
    // predictably.
    private void triggerObservers() {
        setChanged();
        notifyObservers();
    }
}
