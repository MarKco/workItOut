package com.ilsecondodasinistra.workitout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;

public class WorkItOutMain extends SherlockFragmentActivity {

	boolean DEBUG = true;
	
	/*
	 * Elements for app Drawer
	 */
	private String[] mPlanetTitles;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
	
	private TextView timeToLeave;
	private Button entranceButton;
	private Button lunchInButton;
	private Button lunchOutButton;
	private Button exitButton;
	private EditText entranceText;
	private EditText lunchInText;
	private EditText lunchOutText;
	private EditText exitText;
	private TextView extraTimeText;
	private TextView workdayLength;
	private LinearLayout extraTimeLayout;
	
	private Date entranceTime = new Date();				//Where we'll be saving entrance time
	private Date lunchInTime = new Date();				//Where we'll be saving time we come back from lunch
	private Date lunchOutTime = new Date();				//Where we'll be saving time we go out for lunch
	private Date exitTime = new Date();					//Time you declare you're leaving
	private Date estimatedExitTime = new Date();		//Hour of the day you should leave the office
	private Date extraTime = new Date();				//Extra time elapsed, or to pass before end of the day
	private Calendar dateTimeForPicker = Calendar.getInstance();
	
	private SimpleDateFormat hhmmFormatter = new SimpleDateFormat("H:mm");
	private SimpleDateFormat hhmmssFormatter = new SimpleDateFormat("H:mm:ss");
	private SimpleDateFormat longDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	
	private boolean isTimerMarching = true;
	
	private int optionSelected = 0;
	
	static final int TIME_DIALOG_ID = 999;
	
	final Handler handler = new Handler();
	
	//How long a work day lasts
	Date workTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_work_it_out_main);

		//Preferences
		final SharedPreferences settings = getPreferences(0);
		
		/*
		 * Initializations
		 */
		timeToLeave = (TextView)findViewById(R.id.time_to_leave);
		entranceButton = (Button)findViewById(R.id.entrance_button);
		lunchInButton = (Button)findViewById(R.id.lunch_in_button);
		lunchOutButton = (Button)findViewById(R.id.lunch_out_button);
		exitButton = (Button)findViewById(R.id.exit_button);
		
		entranceText = (EditText)findViewById(R.id.entrance_text);
		lunchInText = (EditText)findViewById(R.id.lunch_in_text);
		lunchOutText = (EditText)findViewById(R.id.lunch_out_text);
		exitText = (EditText)findViewById(R.id.exit_text);

		timeToLeave = (TextView)findViewById(R.id.time_to_leave);
		extraTimeText = (TextView)findViewById(R.id.extra_time);
		workdayLength = (TextView)findViewById(R.id.workday_length);
		
		extraTimeLayout = (LinearLayout)findViewById(R.id.extraTimeLayout);
		
		updateWorkDayLength();
		
		final Calendar c = Calendar.getInstance();
		final int calendarHour = c.get(Calendar.HOUR_OF_DAY);
		final int calendarMinute = c.get(Calendar.MINUTE);
		
		try {
			//Initialization
			if(settings.getLong("entranceTime", 0) == 0)
			{
				entranceTime = hhmmFormatter.parse("0:00");
			}
			else
			{
				entranceTime = new Date(settings.getLong("entranceTime", 0));
				entranceText.setText(hhmmFormatter.format(entranceTime));
				setTextColor(entranceText,entranceTime);
			}
			
			if(settings.getLong("lunchInTime", 0) == 0)
			{
				lunchInTime = hhmmFormatter.parse("0:00");
			}
			else
			{
				lunchInTime = new Date(settings.getLong("lunchInTime", 0));
				lunchInText.setText(hhmmFormatter.format(lunchInTime));
				setTextColor(entranceText,entranceTime);
				extraTimeLayout.setVisibility(View.VISIBLE);
			}
	
	
			if(settings.getLong("lunchOutTime", 0) == 0)
			{
				lunchOutTime = hhmmFormatter.parse("0:00");
			}
			else
			{
				lunchOutTime = new Date(settings.getLong("lunchOutTime", 0));
				lunchOutText.setText(hhmmFormatter.format(lunchOutTime));
				setTextColor(entranceText,entranceTime);
			}
			
			if(!(settings.getLong("exitTime", 0) == 0))
			{
				exitTime = new Date(settings.getLong("exitTime", 0));
				exitText.setText(hhmmFormatter.format(exitTime));
				setTextColor(exitText,exitTime);
			}			
		}
		catch(ParseException e)
		{
			logIt(e.getStackTrace().toString());
		}
		
		updateEstimatedTimeOfExit();
		
		entranceText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				optionSelected = 1;
				if (isYesterday(entranceTime))
					chooseTime(calendarHour, calendarMinute);
				else
					chooseTime(entranceTime.getHours(), entranceTime.getMinutes());
			}
		});
		
		lunchOutText.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				optionSelected = 2;
				if (isYesterday(lunchOutTime))
					chooseTime(calendarHour, calendarMinute);
				else
					chooseTime(lunchOutTime.getHours(), lunchOutTime.getMinutes());
			}
		});
		
		lunchInText.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				optionSelected = 3;
				if (isYesterday(lunchInTime))
					chooseTime(calendarHour, calendarMinute);
				else
					chooseTime(lunchInTime.getHours(), lunchInTime.getMinutes());
			}
		});
		
		exitText.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				optionSelected = 4;
				if (isYesterday(exitTime))
					chooseTime(calendarHour, calendarMinute);
				else
					chooseTime(exitTime.getHours(), exitTime.getMinutes());
			}
		});
		
		entranceButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				entranceTime = setActualTime(entranceText, entranceTime);
				entranceActions();
			}
		});
				
		lunchInButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				lunchInTime = setActualTime(lunchInText, lunchInTime);
				lunchInActions();
			}
		});
		
		lunchOutButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				lunchOutTime = setActualTime(lunchOutText, lunchOutTime);
				lunchOutActions();
			}
		});
		
		exitButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				extraTimeLayout.setVisibility(View.VISIBLE);
				exitTime = setActualTime(exitText, exitTime);

				toggleCountForExtraTime();
				removeAlarm();
			}
		});
		
		Button clearButton = (Button)findViewById(R.id.clear);
		clearButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				clearAllInput();
			}
		});        
	}
	
	private void toggleCountForExtraTime() {
		
		//Preferences
		final SharedPreferences settings = getPreferences(0);
		
		if(isTimerMarching)
		{
			handler.removeCallbacks(updateExtraTime);
			isTimerMarching = false;
			Toast.makeText(getBaseContext(), "Conteggio straordinario fermato", 500).show();
			extraTimeLayout.setVisibility(View.VISIBLE);
			
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancelAll(); //When application is open all its notifications must be deleted

		}
		else
		{
			startCountForExtraTime();
			Toast.makeText(getBaseContext(), "Conteggio straordinario ripreso", 500).show();			
			extraTimeLayout.setVisibility(View.VISIBLE);
		}
	}
	
	private void startCountForExtraTime() {
		
		extraTimeLayout.setVisibility(View.VISIBLE);
		
		handler.removeCallbacks(updateExtraTime);
		handler.postDelayed(updateExtraTime, 1000);
		isTimerMarching = true;		
	}
	
	private void entranceActions() {
		//Ricalcola l'orario di uscita
		updateEstimatedTimeOfExit();
		entranceText.setTextColor(Color.BLACK);
		
		/*
		 * Blanks out all previous
		 * entrance and exit timings
		 * if timings are yesterday ones
		 */
		if (isYesterday(lunchInTime))
		{	lunchInTime.setTime(0);
		lunchInText.setText("");
		}
		
		if (isYesterday(lunchOutTime))
		{	lunchOutTime.setTime(0);
		lunchOutText.setText("");
		}
		
		if (isYesterday(exitTime))
		{	exitTime.setTime(0);
		exitText.setText("");
		}
		
		/*
		 * Deletes all notifications
		 */
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll(); //When application is open all its notifications must be deleted
		
		extraTimeLayout.setVisibility(View.GONE);
		removeAlarm();
	}
	
	private void lunchOutActions() {
		lunchOutText.setTextColor(Color.BLACK);
		
		//Ricalcola l'orario di uscita
		updateEstimatedTimeOfExit();
		
		extraTimeLayout.setVisibility(View.GONE);
	}
	
	private void lunchInActions() {
		//Ricalcola l'orario di uscita
		updateEstimatedTimeOfExit();
		
		timeToLeave.setVisibility(View.VISIBLE);
		extraTimeLayout.setVisibility(View.VISIBLE);

		lunchInText.setTextColor(Color.BLACK);
		
		//Set up notification for proper time
		if(estimatedExitTime.after(new Date()))
		{
			Intent i = new Intent(getBaseContext(), NotificationService.class);
			PendingIntent pi = PendingIntent.getService(getBaseContext(), 0, i, 0);
			AlarmManager mAlarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
		Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.setTimeInMillis(estimatedExitTime.getTime());

        /*
         * Debug: the line below allows to set notification to 5 seconds in future.
         */
//		mAlarm.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()+(10*1000)), pi);
		mAlarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
		
		Toast.makeText(getBaseContext(), "Allarme attivato per le ore " + hhmmFormatter.format(estimatedExitTime), 3000).show();
		}

		startCountForExtraTime();
	}

	boolean mIgnoreTimeSet = false;
	
	public void chooseTime(int hours, int minutes) {
		
		//Per quando rifarai il comportamento del time picker
//		DialogFragment timeFragment = new TimePickerFragment();
//		timeFragment.show(getSupportFragmentManager(), "timePicker");

		final TimePickerDialog dialogToShow = new TimePickerDialog(WorkItOutMain.this, timePickerListener,
				hours,
				minutes, true);
		
		// Make the Set button
		dialogToShow.setButton(DialogInterface.BUTTON_POSITIVE, "Set", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
		        mIgnoreTimeSet = false;
		        // only manually invoke OnTimeSetListener (through the dialog) on pre-ICS devices
		        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) dialogToShow.onClick(dialog, which);
		    }
		});
		
		// Set the Cancel button
		dialogToShow.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
		        mIgnoreTimeSet = true;
		        dialog.cancel();
		    }
		});
		
		dialogToShow.show();
	}
	
	TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			if (!mIgnoreTimeSet)
			{
				logIt("Perché passo di qui?");
				dateTimeForPicker.set(Calendar.HOUR_OF_DAY, hourOfDay);
				dateTimeForPicker.set(Calendar.MINUTE, minute);
				
	//			try {
					Date utilityDate = new Date();
					utilityDate.setHours(hourOfDay);
					utilityDate.setMinutes(minute);
					switch(optionSelected) {
					case 1:
						entranceTime = utilityDate;
						entranceText.setText(hhmmFormatter.format(entranceTime));
						entranceActions();
						break;
					case 2:
						lunchOutTime = utilityDate;
						lunchOutText.setText(hhmmFormatter.format(lunchOutTime));
						lunchOutActions();
						break;
					case 3:
						lunchInTime = utilityDate;
						lunchInText.setText(hhmmFormatter.format(lunchInTime));
						lunchInActions();
						break;
					case 4:
						exitTime = utilityDate;
						exitText.setText(hhmmFormatter.format(exitTime));
						updateExtraTimeFields();
						handler.removeCallbacks(updateExtraTime);
						break;
					default:
						break;
					}
			}	
//			} catch (ParseException e) {
//				// TODO Auto-generated catch block
//				Log.e("parakeet",e.getStackTrace().toString());
//			}
		}
	};
	
//	/*
//	 * Finestra di dialogo per gli orari
//	 */
//	@Override
//	protected Dialog onCreateDialog(int id) {
//		switch(id) {
//		case TIME_DIALOG_ID:
//			return new TimePickerDialog(this, timePickerListener, hour, minute, false);
//		}
//		return null;
//	}
	
	private static String pad(int c) {
		if (c >= 10)
		   return String.valueOf(c);
		else
		   return "0" + String.valueOf(c);
	}
	
	private void updateWorkDayLength()
	{
		//Preferences
		final SharedPreferences sharedSettings = PreferenceManager.getDefaultSharedPreferences(WorkItOutMain.this);
		
		int workHours = sharedSettings.getInt(getString(R.string.workday_hours)+".hour", 8);
		int workMinutes = sharedSettings.getInt(getString(R.string.workday_hours)+".minute", 0);

//		Toast.makeText(getBaseContext(), "Ore: "+workHours+" e minuti "+workMinutes, 3000).show();

		try
		{
			workTime = hhmmFormatter.parse(String.valueOf(workHours)+":"+String.valueOf(workMinutes));
			logIt("Okay, la giornata dura " + workHours + " ore e "+ workMinutes + " minuti");
		}
		catch(NumberFormatException e)
		{
			workTime = new Date();
			logIt("Azz, C'è stato un problema!");
		} catch (ParseException e) {
			workTime = new Date();
			logIt("Azz, C'è stato un problema!");
		}
		
		workdayLength.setText(hhmmFormatter.format(workTime));
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		//Preferences
		final SharedPreferences settings = getPreferences(0);
		
		SharedPreferences.Editor editor = settings.edit();

		updateWorkDayLength();
		updateEstimatedTimeOfExit();
		updateExtraTimeFields();
		
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll(); //When application is open all its notifications must be deleted
		
	    EasyTracker.getInstance().activityStart(this); // Add this method.
	    
		if(settings.getBoolean("isTimerMarching", false))
		{
			//Se avevamo chiuso l'app con il timer acceso
			extraTimeLayout.setVisibility(View.VISIBLE);
			isTimerMarching = true;
			startCountForExtraTime();
		}
		else
		{
			/*
			 * Se avevamo chiuso l'app con il timer NON acceso
			 * allora deve mostrare l'ultimo valore risalente
			 * a quando ho chiuso l'app (ed era stato salvato)
			 */
			isTimerMarching = false;
			
			if(settings.getLong("extraTime", 0) != 0)
			{	
				extraTime = new Date(settings.getLong("extraTime", 0));
//				Toast.makeText(getBaseContext(), "Extratime vale " + extraTime.getTime(), 100).show();
				extraTimeLayout.setVisibility(View.VISIBLE);
				
				if(extraTime.getTime() < 0)
				{
					try {
						extraTime = hhmmssFormatter.parse(Long.toString(settings.getLong("extraTime", 0)));
					} catch (ParseException e) {
						// TODO Auto-generated catch block
					}
					extraTimeText.setText(hhmmssFormatter.format(extraTime));
				}
				else
				{
					extraTimeText.setText("-" + hhmmssFormatter.format(extraTime));
				}
			}
			else
			{
//				Toast.makeText(getBaseContext(), "Il tempo vale zero!", 100).show();
				extraTimeLayout.setVisibility(View.GONE);
			}
		}
	    
	    if(timeToLeave.getTag() != "")
	    {
			timeToLeave.setVisibility(View.VISIBLE);
	    }
	    else
	    {
			timeToLeave.setVisibility(View.GONE);
	    }
	}
	
	  @Override
	protected void onPause() {
		super.onPause();
		
		//Preferences
		final SharedPreferences settings = getPreferences(0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putBoolean("isTimerMarching", isTimerMarching);
		editor.putLong("extraTime", extraTime.getTime());
		editor.putLong("entranceTime", entranceTime.getTime());
		editor.putLong("lunchOutTime", lunchOutTime.getTime());
		editor.putLong("exitTime", exitTime.getTime());
		editor.putLong("lunchInTime", lunchInTime.getTime());

		editor.commit();

	}
	
	  @Override
	  public void onStop() {
	    super.onStop();
	    EasyTracker.getInstance().activityStop(this); // Add this method.
	  }

	/*
	 * Questa funzione pulisce tutte le caselle di testo
	 * e resetta le date a mezzanotte. Annulla anche tutte
	 * le notifiche
	 */
	private void clearAllInput()
	{
		//Preferences
		final SharedPreferences settings = getPreferences(0);
		
			estimatedExitTime = new Date(0);
			entranceTime = new Date(0);
			lunchInTime = new Date(0);
			lunchOutTime = new Date(0);
			exitTime = new Date(0);
			
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong("entranceTime", 0);
		editor.putLong("lunchInTime", 0);
		editor.putLong("lunchOutTime", 0);
		editor.putLong("exitTime",0);
		editor.putLong("extraTime",0);
		editor.commit();
		
		entranceText.setText("");
		lunchInText.setText("");
		lunchOutText.setText("");
		timeToLeave.setText("");
		extraTimeText.setText("");
		exitText.setText("");
		
		timeToLeave.setVisibility(View.GONE);
		extraTimeLayout.setVisibility(View.GONE);
		
		removeAlarm();
	}
	
	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.work_it_out_main, menu);
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.actionbarsherlock.app.SherlockActivity#onOptionsItemSelected(android.view.MenuItem)
	 * Cosa succede se l'utente seleziona una voce di menu o dell'action bar?
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId()) {
			case R.id.action_clear:
				clearAllInput();
				return true;
			case R.id.action_settings:
				Intent openSettings = new Intent(WorkItOutMain.this, SettingsActivity.class);
				startActivity(openSettings);
				return true;
			case R.id.send_email:
				
				long oneHour = 1000*60*60;
				
				/* Quanto tempo lavorato quest'oggi? */
				Date dailyWorkedTime = new Date(
						(exitTime.getTime() - lunchInTime.getTime())
						+ (lunchOutTime.getTime() - entranceTime.getTime()) - oneHour);

				
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("message/rfc822");
				i.putExtra(Intent.EXTRA_SUBJECT, "WorkItOut: orari del giorno " + longDateFormatter.format(new Date()));
				i.putExtra(Intent.EXTRA_TEXT   , "Orario di entrata: " + hhmmFormatter.format(entranceTime)
							+ "\nOrario di pranzo: " + hhmmFormatter.format(lunchOutTime)
							+ "\nOrario di rientro: " + hhmmFormatter.format(lunchInTime)
							+ "\nOrario di uscita: " + hhmmFormatter.format(exitTime)
							+ "\nTempo totale lavorato: " + hhmmFormatter.format(dailyWorkedTime)
							+ "\nStraordinario: " + extraTimeText.getText());
				try {
				    startActivity(Intent.createChooser(i, "Invia mail..."));
				} catch (android.content.ActivityNotFoundException ex) {
				    Toast.makeText(WorkItOutMain.this, "Nessun client email installato.", Toast.LENGTH_SHORT).show();
				}
			default:
				return false;
		}
	}
	
	protected void onResume() {
		super.onResume();
		setTextColor(entranceText, entranceTime);
		setTextColor(lunchInText, lunchInTime);
		setTextColor(lunchOutText, lunchOutTime);
	}
	
	private Date setActualTime(TextView textToChange, Date dateToChange)
	{
		//Prepara il timestamp
		Date dt = new Date();
		
		//Scrive il timestamp nella casella di testo
		textToChange.setText(hhmmFormatter.format(dt));
		
		//Modifica il timestamp passato come parametro
		dateToChange = dt;
		
		return dateToChange;
	}	
	
	private void updateEstimatedTimeOfExit()
	{	
		estimatedExitTime = new Date(entranceTime.getTime() 
							+ workTime.getTime() 
							+ (lunchInTime.getTime() 
							- lunchOutTime.getTime()));
//		logIt("Pausa pranzo: " + dateFormatter.format((lunchInTime.getTime() - lunchOutTime.getTime())));
//		logIt("Worktime: " + dateFormatter.format(workTime));
//		logIt("Entrance: " + dateFormatter.format(entranceTime));
//		logIt("Lunch out: " + dateFormatter.format(lunchOutTime));
//		logIt("Lunch in: " + dateFormatter.format(lunchInTime));
//		logIt(dateFormatter.format(estimatedExitTime));

		timeToLeave.setText(hhmmFormatter.format(estimatedExitTime));
	}
	
	private Runnable updateExtraTime = new Runnable() {
		public void run() {
			try {
				
				updateExtraTimeFields();
				
				handler.removeCallbacks(updateExtraTime);
				handler.postDelayed(updateExtraTime, 1000);
			}
			catch(Exception e)
			{
				logIt(e.getStackTrace().toString());
			}
		}
	};
	
	private void updateExtraTimeFields() {
		Date now = new Date();
		
		if(estimatedExitTime.after(now))
		{
			//Se sono ancora nelle ore regolamentari
			extraTime = new Date(estimatedExitTime.getTime() - now.getTime());
			extraTimeText.setText("-" + hhmmssFormatter.format(extraTime));
		}
		else
		{
			//Se sarei già dovuto uscire
			extraTime = new Date(now.getTime() - estimatedExitTime.getTime());
			extraTimeText.setText(hhmmssFormatter.format(extraTime));
		}
	}

	private void setTextColor(EditText inputText, Date inputDate)
	{
		Date now = new Date();
		
//		logIt("Nel campo " + dateFormatter.format(inputDate) + " la differenza tra i due tempi è di " + (now.getTime() - inputDate.getTime())/1000 + " secondi");
		
//		if(now.getTime() - inputDate.getTime() > 86400000)	//Un giorno intero
//		if(now.getTime() - inputDate.getTime() > 60000)			//Un minuto
//		long workDayInMillis = (long)Math.ceil(workDayHours*60*60*1000);
		long workDayInMillis = workTime.getTime();
		if(now.getTime() - inputDate.getTime() > workDayInMillis)	//Giornata lavorativa di 8 ore
		{
			inputText.setTextColor(Color.GRAY);
			exitText.setTextColor(Color.GRAY);
		}
		else
		{
			inputText.setTextColor(Color.BLACK);
			exitText.setTextColor(Color.BLACK);
		}
	}
	
	/*
	 * Check if date provided is yesterday
	 * @returns true if it's yesterday (or before)
	 * @returns false otherwise
	 */
	private boolean isYesterday(Date dateToCheck)
	{
		Calendar c1 = Calendar.getInstance(); // today
		c1.add(Calendar.DAY_OF_YEAR, -1); // yesterday
		
		Calendar c2 = Calendar.getInstance();
		c2.setTime(dateToCheck); // your date

		if (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
		  && c1.get(Calendar.DAY_OF_YEAR) < c2.get(Calendar.DAY_OF_YEAR)) {
			return false;
		}
		else
		{
			return true;
		}
	}
	
	private void removeAlarm()
	{
		Intent i = new Intent(getBaseContext(), NotificationService.class);
		PendingIntent pi = PendingIntent.getService(getBaseContext(), 0, i, 0);
		AlarmManager mAlarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);

	    mAlarm.cancel(pi);
	    pi.cancel();
	}

	private void logIt(String message)
	{
		if (DEBUG)
		{
			Log.i("workitout", message);
		}
	}
	
}
