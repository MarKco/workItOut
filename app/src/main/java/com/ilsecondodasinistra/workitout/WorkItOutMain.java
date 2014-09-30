package com.ilsecondodasinistra.workitout;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.ilsecondodasinistra.workitout.utils.BadgeHelper;
import com.ilsecondodasinistra.workitout.utils.SettingsWorkitout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import it.lucichkevin.cip.Utils;
import it.lucichkevin.cip.navigationdrawermenu.DrawerLayoutHelper;
import it.lucichkevin.cip.navigationdrawermenu.ItemDrawerMenu;
import it.lucichkevin.cip.dialogs.pickers.MinutesPickerDialog;
import it.lucichkevin.cip.dialogs.pickers.TimePickerDialog;
//import com.google.analytics.tracking.android.EasyTracker;

public class WorkItOutMain extends SherlockFragmentActivity implements Observer {

    boolean DEBUG = false;

    //  Fragment managing the behaviors, interactions and presentation of the navigation drawer.
    private DrawerLayoutHelper drawerLayoutHelper;
    private ItemDrawerMenu[] ARRAY_ITEMS = new ItemDrawerMenu[]{

            //  Duration in hours and minutes
            new ItemDrawerMenu( R.string.prefs_choose_workday_length, new ItemDrawerMenu.OnClickListener() {
                @Override
                public void onClick() {
                    final TimePickerDialog timePickerDialog = new TimePickerDialog(WorkItOutMain.this);
                    timePickerDialog.getDialog().setTitle(R.string.workday_duration_prefs_title);
                    timePickerDialog.setHour(SettingsWorkitout.getWorkTime().getHours());
                    timePickerDialog.setMinute(SettingsWorkitout.getWorkTime().getMinutes());
                    timePickerDialog.setCallbacks(new TimePickerDialog.Callbacks() {
                        @Override
                        public void onButtonPositiveClicked( Dialog dialog, int hour, int minute ){
                            Calendar c = Calendar.getInstance();
                            c.set(Calendar.HOUR_OF_DAY, hour);
                            c.set(Calendar.MINUTE, minute);
                            SettingsWorkitout.setWorkTime(c.getTime());
                            updateWorkDayLength();
                            updateEstimatedTimeOfExit();
                            dialog.dismiss();
                        }
                        @Override
                        public void onButtonCancelClicked( Dialog dialog, int hour, int minute ){
                            dialog.dismiss();
                        }
                    });
                    timePickerDialog.show();
                }
            }),

            //
            new ItemDrawerMenu( R.string.prefs_delete_timings, new ItemDrawerMenu.OnClickListener() {
                @Override
                public void onClick() {
                    WorkItOutMain.this.clearAllInput();
                }
            }),

            //
            new ItemDrawerMenu( R.string.change_break_time, new ItemDrawerMenu.OnClickListener() {
                @Override
                public void onClick() {
                    final MinutesPickerDialog minutesPickerDialog = new MinutesPickerDialog( WorkItOutMain.this );
                    minutesPickerDialog.getDialog().setTitle("Seleziona durata pausa");
                    minutesPickerDialog.setMinute(SettingsWorkitout.getPauseDuration());
                    minutesPickerDialog.setCallbacks(new MinutesPickerDialog.Callbacks(){
                        @Override
                        public void onButtonPositiveClicked( Dialog dialog, int minute ){
                            SettingsWorkitout.setPauseDuration(minute);
                            dialog.dismiss();
                        }
                        @Override
                        public void onButtonCancelClicked( Dialog dialog, int i) {
                            dialog.dismiss();
                        }
                    });
//                minutesPickerDialog.show(getSupportFragmentManager(), "timepicker-break-time");
                    minutesPickerDialog.show();
                }
            }),

            //
            new ItemDrawerMenu( R.string.send_email, new ItemDrawerMenu.OnClickListener() {
                @Override
                public void onClick() {
                    WorkItOutMain.this.sendMail();
                }
            }),

            //  About WorkItOut
            new ItemDrawerMenu( R.string.prefs_about, AboutActivity.class ),

    };


	/*
	 * Elements for app Drawer
	 */

    private TextView timeToLeave;

    private EditText entranceText;
    private EditText lunchInText;
    private EditText lunchOutText;
    private EditText exitText;
    private TextView extraTimeText;
    private TextView workdayLength;
    private LinearLayout extraTimeLayout;

    private SimpleDateFormat hhmmFormatter = new SimpleDateFormat("H:mm");
    private SimpleDateFormat hhmmssFormatter = new SimpleDateFormat("H:mm:ss");
    private SimpleDateFormat longDateFormatter = new SimpleDateFormat("yyyy-MM-dd");    //  Used in sendMail

    private int optionSelected = 0;

    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_it_out_main);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getString(R.string.app_name));

        //  Initializations
        Button entranceButton = (Button)findViewById(R.id.entrance_button);
        Button lunchInButton = (Button)findViewById(R.id.lunch_in_button);
        Button lunchOutButton = (Button)findViewById(R.id.lunch_out_button);
        Button exitButton = (Button)findViewById(R.id.exit_button);
        Button pauseButton = (Button)findViewById(R.id.pause_button);

        entranceText = (EditText)findViewById(R.id.entrance_text);
        lunchInText = (EditText)findViewById(R.id.lunch_in_text);
        lunchOutText = (EditText)findViewById(R.id.lunch_out_text);
        exitText = (EditText)findViewById(R.id.exit_text);

        timeToLeave = (TextView)findViewById(R.id.time_to_leave);
        extraTimeText = (TextView)findViewById(R.id.extra_time);
        workdayLength = (TextView)findViewById(R.id.workday_length);

        extraTimeLayout = (LinearLayout)findViewById(R.id.extraTimeLayout);

        updateWorkDayLength();

        try {
            //  Initialization

            Date entranceTime = new Date(SettingsWorkitout.getEntranceTime());
            Utils.logger("entranceTime = "+ entranceTime, Utils.LOG_DEBUG );
            if( entranceTime.getTime() == 0 ) {
                BadgeHelper.setEntranceTime(hhmmFormatter.parse("0:00"));
            }else{
                BadgeHelper.setEntranceTime( entranceTime );
                entranceText.setText( hhmmFormatter.format(entranceTime) );
                setTextColor(entranceText, entranceTime );
            }

            Date lunchInTime = new Date(SettingsWorkitout.getLunchInTime());
            Utils.logger("lunchInTime = "+ lunchInTime, Utils.LOG_DEBUG );
            if( lunchInTime.getTime() == 0 ){
                BadgeHelper.setLunchInTime(hhmmFormatter.parse("0:00"));
            }else{
                BadgeHelper.setLunchInTime(lunchInTime);
                lunchInText.setText( hhmmFormatter.format(lunchInTime) );
                setTextColor(entranceText,lunchInTime);

                if( !isYesterday(lunchInTime) ){
                    extraTimeLayout.setVisibility(View.VISIBLE);
                }else{
                    extraTimeLayout.setVisibility(View.GONE);
                }
            }

            Date lunchOutTime = new Date(SettingsWorkitout.getLunchOutTime());
            Utils.logger("lunchOutTime = "+ lunchOutTime, Utils.LOG_DEBUG );
            if( lunchOutTime.getTime() == 0 ){
                BadgeHelper.setLunchOutTime(hhmmFormatter.parse("0:00"));
            }else{
                BadgeHelper.setLunchOutTime(lunchOutTime);
                lunchOutText.setText( hhmmFormatter.format(lunchOutTime) );
                setTextColor(entranceText,lunchOutTime);
            }

            Date exitTime = new Date(SettingsWorkitout.getExitTime());
            Utils.logger("exitTime = "+ exitTime, Utils.LOG_DEBUG );
            if( exitTime.getTime() != 0 ){
                BadgeHelper.setExitTime( exitTime );
                exitText.setText(hhmmFormatter.format(exitTime));
                setTextColor(exitText,exitTime);
            }

            BadgeHelper.setExtraTimeSign( SettingsWorkitout.isExtraTimeSign() );

        }catch( ParseException e ){
            e.printStackTrace();
        }

        updateEstimatedTimeOfExit();

        entranceText.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                entranceText.setText("");
                BadgeHelper.setEntranceTime(new Date(0));
                SettingsWorkitout.setEntranceTime(0);
                updateEstimatedTimeOfExit();
                return true;
            }
        });

        entranceText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                optionSelected = entranceText.getId();
                if (isYesterday(BadgeHelper.getEntranceTime()) ){
                    chooseTime();
                }else
                    chooseTime( BadgeHelper.getEntranceTime() );
            }
        });

        lunchOutText.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                lunchOutText.setText("");
                BadgeHelper.setLunchOutTime(new Date(0));
                SettingsWorkitout.setLunchOutTime(0);
                return true;
            }
        });

        lunchOutText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                optionSelected = lunchOutText.getId();
                if( isYesterday(BadgeHelper.getLunchOutTime()) )
                    chooseTime();
                else
                    chooseTime( BadgeHelper.getLunchOutTime() );
            }
        });

        lunchInText.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                lunchInText.setText("");
                BadgeHelper.setLunchInTime(new Date(0));
                SettingsWorkitout.setLunchInTime(0);
                return true;
            }
        });

        lunchInText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                optionSelected = lunchInText.getId();
                if( isYesterday(BadgeHelper.getLunchInTime()) )
                    chooseTime();
                else
                    chooseTime( BadgeHelper.getLunchInTime() );
            }
        });

        exitText.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                exitText.setText("");
                BadgeHelper.setExitTime( new Date(0) );
                startCountForExtraTime();
                return true;
            }
        });

        exitText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                optionSelected = exitText.getId();
                if( isYesterday(BadgeHelper.getExitTime()) ){
                    chooseTime();
                }else{
                    chooseTime( BadgeHelper.getExitTime() );
                }
            }
        });

        entranceButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                BadgeHelper.setEntranceTime( setActualTime(entranceText, BadgeHelper.getEntranceTime()) );
                entranceActions();
            }
        });

        lunchInButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                BadgeHelper.setLunchInTime( setActualTime(lunchInText, BadgeHelper.getLunchInTime() ) );
                lunchInActions();
            }
        });

        lunchOutButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                BadgeHelper.setLunchOutTime( setActualTime(lunchOutText, BadgeHelper.getLunchOutTime()) );
                lunchOutActions();
            }
        });

        exitButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                extraTimeLayout.setVisibility(View.VISIBLE);
                BadgeHelper.setExitTime( setActualTime(exitText, BadgeHelper.getExitTime()) );
                toggleCountForExtraTime();

                SettingsWorkitout.setExtraTimeHours( BadgeHelper.getExtraTime().getHours() );
                SettingsWorkitout.setExtraTimeMinutes( BadgeHelper.getExtraTime().getMinutes() );
                SettingsWorkitout.setExtraTimeSeconds( BadgeHelper.getExtraTime().getSeconds() );
                SettingsWorkitout.setExtraTimeSign(BadgeHelper.isExtraTimeSign());

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

        //  Toggles handler for application drawer
        drawerLayoutHelper = new DrawerLayoutHelper( WorkItOutMain.this, R.id.main_layout, R.id.left_drawer, ARRAY_ITEMS );

        //  If application drawer was never opened manually, automatically open it at first application run
        if( SettingsWorkitout.isFirstRun() ){
            drawerLayoutHelper.open();
        }

        //  This button enables a notification before the break ends
        pauseButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {

//					long pauseInMillis = 3 * 60 * 1000; //15 is break duration - in minutes
                long pauseInMillis = SettingsWorkitout.getPauseDuration() * 60 * 1000; //   15 is break duration - in minutes
                long intervallForNotification = pauseInMillis - (2000 * 60); //Notification is 3 minutes before the end of the break

                Date now = new Date();
                now.setTime(now.getTime() + intervallForNotification);

                Intent i = new Intent(getBaseContext(), PauseNotificationService.class);
                PendingIntent pi = PendingIntent.getService(getBaseContext(), 2, i, 0);
                AlarmManager mAlarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);

                mAlarm.set(AlarmManager.RTC_WAKEUP, now.getTime(), pi);

                Utils.Toaster(getBaseContext(), getString(R.string.alarm_activated) + hhmmFormatter.format(BadgeHelper.getEstimatedExitTime()), 3000);
//                    Toast.makeText(getBaseContext(), getString(R.string.alarm_activated) + hhmmFormatter.format(estimatedExitTime), 3000).show();
            }
        });
    }

    private void toggleCountForExtraTime() {

        if( BadgeHelper.isTimerMarching() )
        {
            handler.removeCallbacks(updateExtraTime);
            BadgeHelper.setTimerMarching(false);
            extraTimeLayout.setVisibility(View.VISIBLE);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll(); //When application is open all its notifications must be deleted
        }
        else
        {
            startCountForExtraTime();
            extraTimeLayout.setVisibility(View.VISIBLE);
        }
    }

    private void startCountForExtraTime() {

        extraTimeLayout.setVisibility(View.VISIBLE);

        handler.removeCallbacks(updateExtraTime);
        handler.postDelayed(updateExtraTime, 1000);
        BadgeHelper.setTimerMarching(true);
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
        if( isYesterday(BadgeHelper.getLunchInTime()) ){
//            lunchInTime.setTime(0);
            Date tmp = BadgeHelper.getLunchInTime();
            tmp.setTime(0);
            BadgeHelper.setLunchInTime(tmp);
            lunchInText.setText("");
        }

        if( isYesterday(BadgeHelper.getLunchOutTime()) ){
            Date tmp = BadgeHelper.getLunchOutTime();
            tmp.setTime(0);
            BadgeHelper.setLunchOutTime(tmp);
            lunchOutText.setText("");
        }

        if( isYesterday(BadgeHelper.getExitTime()) ){
            Date tmp = BadgeHelper.getExitTime();
            tmp.setTime(0);
            BadgeHelper.setExitTime(tmp);
            exitText.setText("");
        }

        //  Deletes all notifications
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll(); //When application is open all its notifications must be deleted

        extraTimeLayout.setVisibility(View.GONE);
        timeToLeave.setVisibility(View.VISIBLE);
        removeAlarm();
    }

    private void lunchOutActions() {
        lunchOutText.setTextColor(Color.BLACK);

        //  Ricalcola l'orario di uscita
        updateEstimatedTimeOfExit();

        extraTimeLayout.setVisibility(View.GONE);
        timeToLeave.setVisibility(View.GONE);
    }

    private void lunchInActions() {
        //  Ricalcola l'orario di uscita
        updateEstimatedTimeOfExit();

        timeToLeave.setVisibility(View.VISIBLE);
        extraTimeLayout.setVisibility(View.VISIBLE);

        lunchInText.setTextColor(Color.BLACK);

        //Set up notification for proper time
        if( BadgeHelper.getEstimatedExitTime().after(new Date()) ){
            Intent i = new Intent(getBaseContext(), NotificationService.class);
            PendingIntent pi = PendingIntent.getService(getBaseContext(), 0, i, 0);
            AlarmManager mAlarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);

            /*
             * Debug: the line below allows to set notification to 5 seconds in future.
             */
//    		mAlarm.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()+(10*1000)), pi);
            mAlarm.set(AlarmManager.RTC_WAKEUP, BadgeHelper.getEstimatedExitTime().getTime(), pi);

            Utils.Toaster( getBaseContext(), getString(R.string.alarm_activated) + " " + hhmmFormatter.format(BadgeHelper.getEstimatedExitTime()), 3000 );
        }

        startCountForExtraTime();
    }

    public void chooseTime(){
        Calendar sharedCal = Calendar.getInstance();
        chooseTime( sharedCal.get(Calendar.HOUR_OF_DAY), sharedCal.get(Calendar.MINUTE) );
    }

    public void chooseTime( Date date ){
        chooseTime( date.getHours(), date.getMinutes() );
    }

    public void chooseTime( Integer hours, Integer minutes ){
        final TimePickerDialog timeFragment = new TimePickerDialog( WorkItOutMain.this );

        switch( optionSelected ){
            case R.id.entrance_text:
                timeFragment.setTitle(R.string.entranceButtonLabel);
                break;
            case R.id.lunch_out_text:
                timeFragment.setTitle(R.string.lunchOutButtonLabel);
                break;
            case R.id.lunch_in_text:
                timeFragment.setTitle(R.string.lunchInButtonLabel);
                break;
            case R.id.exit_text:
                timeFragment.setTitle(R.string.exitButtonLabel);
                break;
        }

        timeFragment.setCallbacks(new TimePickerDialog.Callbacks() {

            @Override
            public void onButtonPositiveClicked( Dialog dialog, int hour, int minute) {

                Date utilityDate = new Date();
                utilityDate.setHours(hour);
                utilityDate.setMinutes(minute);

                switch( optionSelected ){
                    case R.id.entrance_text:
                        BadgeHelper.setEntranceTime(utilityDate);
                        entranceText.setText(hhmmFormatter.format(utilityDate));
                        entranceActions();
                        break;
                    case R.id.lunch_out_text:
                        BadgeHelper.setLunchOutTime(utilityDate);
                        lunchOutText.setText(hhmmFormatter.format(utilityDate));
                        lunchOutActions();
                        break;
                    case R.id.lunch_in_text:
                        BadgeHelper.setLunchInTime(utilityDate);
                        lunchInText.setText(hhmmFormatter.format(utilityDate));
                        lunchInActions();
                        break;
                    case R.id.exit_text:
                        BadgeHelper.setExitTime(utilityDate);
                        exitText.setText(hhmmFormatter.format(utilityDate));
                        //  It must be stopped in any case, so we put it true. Next method will make it false and stop timer.
                        BadgeHelper.setTimerMarching(true);

                        toggleCountForExtraTime();
                        updateExtraTimeFields();
                        handler.removeCallbacks(updateExtraTime);
                        break;
                    default:
                        break;
                }

                timeFragment.dismiss();
            }

            @Override
            public void onButtonCancelClicked( Dialog dialog, int hour, int minute ) {
                timeFragment.dismiss();
            }
        });

        if( hours != null ){
            timeFragment.setHour(hours);
        }
        if( minutes != null ){
            timeFragment.setMinute(minutes);
        }

        timeFragment.show();
    }

    /*
     * Updates workday length by calling a helper function
     */
    private void updateWorkDayLength(){
        try{
            Utils.logger("WORK TIME = "+ SettingsWorkitout.getWorkTime().getHours() +":"+ SettingsWorkitout.getWorkTime().getMinutes(), Utils.LOG_DEBUG );
            workdayLength.setText(hhmmFormatter.format(SettingsWorkitout.getWorkTime()));
        }catch( NumberFormatException e ){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll(); //    When application is open all its notifications must be deleted

//		EasyTracker.getInstance().activityStart(this); // Add this method.

        if( SettingsWorkitout.isTimerMarching() )
        {
            //  Se avevamo chiuso l'app con il timer acceso
            extraTimeLayout.setVisibility(View.VISIBLE);
            BadgeHelper.setTimerMarching(true);
            startCountForExtraTime();
        }
        else
        {
			/*
			 * Se avevamo chiuso l'app con il timer NON acceso
			 * allora deve mostrare l'ultimo valore risalente
			 * a quando ho chiuso l'app (ed era stato salvato)
			 */
            BadgeHelper.setTimerMarching(false);

            if( SettingsWorkitout.getExtraTimeSeconds() != -1){

                Date extraTime = new Date();
                extraTime.setHours( (int) SettingsWorkitout.getExtraTimeHours() );
                extraTime.setMinutes( (int) SettingsWorkitout.getExtraTimeMinutes() );
                extraTime.setSeconds( (int) SettingsWorkitout.getExtraTimeSeconds() );
                BadgeHelper.setExtraTime(extraTime);
                extraTimeLayout.setVisibility(View.VISIBLE);

                if( BadgeHelper.isExtraTimeSign() ){
                    extraTimeText.setText( hhmmssFormatter.format(BadgeHelper.getExtraTime()) );
                }else{
                    extraTimeText.setText("-" + hhmmssFormatter.format(BadgeHelper.getExtraTime()));
                }
            }
            else
            {
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

        setTextColor(entranceText, BadgeHelper.getEntranceTime() );
        setTextColor(lunchInText, BadgeHelper.getLunchInTime() );
        setTextColor(lunchOutText, BadgeHelper.getLunchOutTime() );

    }

    @Override
    protected void onPause() {
        super.onPause();

        SettingsWorkitout.setTimerMarching( BadgeHelper.isTimerMarching() );

        if( BadgeHelper.isTimerMarching() ){
            SettingsWorkitout.setExtraTimeHours( BadgeHelper.getExitTime().getHours() );
            SettingsWorkitout.setExtraTimeMinutes(BadgeHelper.getExitTime().getMinutes());
            SettingsWorkitout.setExtraTimeSeconds(BadgeHelper.getExitTime().getSeconds());
            SettingsWorkitout.setExtraTimeSign(BadgeHelper.isExtraTimeSign());
        }

        SettingsWorkitout.setEntranceTime(BadgeHelper.getEntranceTime().getTime());
        SettingsWorkitout.setLunchOutTime(BadgeHelper.getLunchOutTime().getTime());
        SettingsWorkitout.setLunchInTime(BadgeHelper.getLunchInTime().getTime());
        SettingsWorkitout.setExitTime(BadgeHelper.getExitTime().getTime());

    }

    @Override
    public void onStop() {
        super.onStop();
//		EasyTracker.getInstance().activityStop(this); // Add this method.
    }

    /*
     * Questa funzione pulisce tutte le caselle di testo
     * e resetta le date a mezzanotte. Annulla anche tutte
     * le notifiche
     */
    public void clearAllInput(){

        BadgeHelper.clearAllInput();

        entranceText.setText("");
        lunchInText.setText("");
        lunchOutText.setText("");
        timeToLeave.setText("");
        extraTimeText.setText("");
        exitText.setText("");

        SettingsWorkitout.setEntranceTime(0);
        SettingsWorkitout.setLunchInTime(0);
        SettingsWorkitout.setLunchOutTime(0);
        SettingsWorkitout.setExitTime(0);
        SettingsWorkitout.setExtraTimeHours(-1);
        SettingsWorkitout.setExtraTimeMinutes(-1);
        SettingsWorkitout.setExtraTimeSeconds(-1);

        timeToLeave.setVisibility(View.GONE);
        extraTimeLayout.setVisibility(View.GONE);

        removeAlarm();
    }

//	@Override
//	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		MenuInflater inflater = getSupportMenuInflater();
//		inflater.inflate(R.menu.work_it_out_main, menu);
//		return true;
//	}

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockActivity#onOptionsItemSelected(android.view.MenuItem)
     * Cosa succede se l'utente seleziona una voce di menu o dell'action bar?
     */
    @Override
    public boolean onOptionsItemSelected( MenuItem item ){

        if( item.getItemId() == android.R.id.home ){
            drawerLayoutHelper.toggle();
            return true;
        }

        switch( item.getItemId() ){
            case R.id.send_email:
                sendMail();
                return true;
            default:
                return false;
        }
    }

    private Date setActualTime( TextView textToChange, Date dateToChange ){
        //  Prepara il timestamp
        Date dt = new Date();

        //  Scrive il timestamp nella casella di testo
        textToChange.setText(hhmmFormatter.format(dt));

        return dt;
    }

    private void updateEstimatedTimeOfExit(){
        BadgeHelper.updateEstimatedTimeOfExit();
        timeToLeave.setText(hhmmFormatter.format( BadgeHelper.getEstimatedExitTime() ));
    }

    private Runnable updateExtraTime = new Runnable() {
        public void run() {
            try {

                updateExtraTimeFields();

                handler.removeCallbacks(updateExtraTime);
                handler.postDelayed(updateExtraTime, 1000);
            }
            catch( Exception e ){
                e.printStackTrace();
            }
        }
    };

    private void updateExtraTimeFields() {
        Date exitDateTime = new Date();

//		if(exitTime != null && !(isYesterday(exitTime)))
        if( !BadgeHelper.isTimerMarching() ){
            exitDateTime = BadgeHelper.getExitTime();
        }

        if( BadgeHelper.getEstimatedExitTime().after(exitDateTime) ){
            //  Se sono ancora nelle ore regolamentari
            Calendar cal = Calendar.getInstance(); // creates calendar
            cal.setTime(BadgeHelper.getEstimatedExitTime()); // sets calendar time/date
            cal.add(Calendar.HOUR_OF_DAY, -exitDateTime.getHours());
            cal.add(Calendar.MINUTE, -exitDateTime.getMinutes());
            cal.add(Calendar.SECOND, -exitDateTime.getSeconds());
            BadgeHelper.setExitTime(cal.getTime());

//			Toast.makeText(getBaseContext(), "ExtraTime vale -"+hhmmssFormatter.format(estimatedExitTime), 200).show();

            BadgeHelper.setExtraTimeSign(false);	//  Deve avere il segno - davanti
            extraTimeText.setText("-" + hhmmssFormatter.format(BadgeHelper.getExitTime()));
        }
        else
        {
            Date eet = BadgeHelper.getEstimatedExitTime();
            //  Se sto facendo dello straordinario
            Calendar cal = Calendar.getInstance(); // creates calendar
            cal.setTime(exitDateTime); // sets calendar time/date
            cal.add(Calendar.HOUR_OF_DAY, -eet.getHours());
            cal.add(Calendar.MINUTE, -eet.getMinutes());
            cal.add(Calendar.SECOND, -eet.getSeconds());
            BadgeHelper.setExitTime(cal.getTime());

//			Toast.makeText(getBaseContext(), hhmmssFormatter.format(exitDateTime) + " " 
//					+ hhmmssFormatter.format(estimatedExitTime) + " " 
//					+ hhmmssFormatter.format(extraTime), 2).show();

            BadgeHelper.setExtraTimeSign(true);	//Dev'essere stampato come orario positivo
            extraTimeText.setText( hhmmssFormatter.format(BadgeHelper.getExitTime()) );
        }
    }

    private void setTextColor( EditText inputText, Date inputDate ){
        setTextColor( inputText, inputDate.getTime() );
    }
    private void setTextColor( EditText inputText, long inputDate ){
        Date now = new Date();

//		Utils.logger("Nel campo " + dateFormatter.format(inputDate) + " la differenza tra i due tempi è di " + (now.getTime() - inputDate )/1000 + " secondi", Utils.LOG_DEBUG );

//		if(now.getTime() - inputDate.getTime() > 86400000)  //  Un giorno intero
//		if(now.getTime() - inputDate.getTime() > 60000)		//  Un minuto
//		long workDayInMillis = (long) Math.ceil(workDayHours*60*60*1000);

        if( (now.getTime() - inputDate) > SettingsWorkitout.getWorkTimeInMillis() )	//  Giornata lavorativa di X ore
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == 1 ){
            if(resultCode == 0){
                // We successfully get back from settings
                updateWorkDayLength();
                updateEstimatedTimeOfExit();
                updateExtraTimeFields();
            }
        }
    }


    @Override
    protected void onPostCreate( Bundle savedInstanceState ){
        super.onPostCreate(savedInstanceState);
        drawerLayoutHelper.getDrawerToggle().syncState();
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ){
        super.onConfigurationChanged(newConfig);
        drawerLayoutHelper.getDrawerToggle().onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onKeyDown( final int keyCode, final KeyEvent event ){
        if( keyCode == KeyEvent.KEYCODE_MENU ){
            drawerLayoutHelper.toggle();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void update( Observable observable, Object data ){
        onResumeFragments();
        updateWorkDayLength();
        updateEstimatedTimeOfExit();
        updateExtraTimeFields();
    }

    /**
     TODO: getHours() is deprecated :(
     */
    public void sendMail() {
        //  Quanto tempo lavorato quest'oggi?

        Calendar dailyWorkedTime = Calendar.getInstance();
        dailyWorkedTime.setTime( BadgeHelper.getExitTime() );
        dailyWorkedTime.add(Calendar.HOUR, -BadgeHelper.getLunchInTime().getHours());
        dailyWorkedTime.add(Calendar.MINUTE, -BadgeHelper.getLunchInTime().getMinutes());
        dailyWorkedTime.add(Calendar.HOUR, BadgeHelper.getLunchOutTime().getHours());
        dailyWorkedTime.add(Calendar.MINUTE, BadgeHelper.getLunchOutTime().getMinutes());
        dailyWorkedTime.add(Calendar.HOUR, -BadgeHelper.getEntranceTime().getHours());
        dailyWorkedTime.add(Calendar.MINUTE, -BadgeHelper.getEntranceTime().getMinutes());

//		Date dailyWorkedTime = new Date(
//				(exitTime.getTime() - lunchInTime.getTime())
//				+ (lunchOutTime.getTime() - entranceTime.getTime()) - oneHour);


        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject) + longDateFormatter.format(new Date()));
        i.putExtra(Intent.EXTRA_TEXT   , getString(R.string.email_in_time) + hhmmFormatter.format(BadgeHelper.getEntranceTime())
                + "\n" + getString(R.string.email_lunch_time) + hhmmFormatter.format(BadgeHelper.getLunchOutTime())
                + "\n" + getString(R.string.email_back_from_lunch) + hhmmFormatter.format(BadgeHelper.getLunchInTime())
                + "\n" + getString(R.string.email_exit_time) + hhmmFormatter.format(BadgeHelper.getExitTime())
                + "\n" + getString(R.string.email_total_time) + hhmmFormatter.format(dailyWorkedTime.getTime())
                + "\n" + getString(R.string.email_extra_time) + extraTimeText.getText());
        try {
            startActivity(Intent.createChooser(i, getString(R.string.send_email)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(WorkItOutMain.this, getString(R.string.no_email_client), Toast.LENGTH_SHORT).show();
        }
    }

}