package com.ilsecondodasinistra.workitout;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.ilsecondodasinistra.workitout.database.PauseWorking;
import com.ilsecondodasinistra.workitout.utils.BadgeHelper;
import com.ilsecondodasinistra.workitout.utils.BadgeHelperFormat;
import com.ilsecondodasinistra.workitout.utils.CounterWorkingTime;
import com.ilsecondodasinistra.workitout.utils.SettingsWorkitout;
import com.ilsecondodasinistra.workitout.utils.preferences.PreferencesActivity;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;

import it.lucichkevin.cip.Utils;
import it.lucichkevin.cip.dialogs.pickers.TimePickerDialog;
import it.lucichkevin.cip.navigationdrawermenu.DrawerLayoutHelper;
import it.lucichkevin.cip.navigationdrawermenu.ItemDrawerMenu;
import it.lucichkevin.cip.preferencesmanager.PreferencesManager;
//import com.google.analytics.tracking.android.EasyTracker;

public class WorkItOutMain extends SherlockFragmentActivity {

    //  Fragment managing the behaviors, interactions and presentation of the navigation drawer.
    private DrawerLayoutHelper drawerLayoutHelper;
    private ItemDrawerMenu[] ARRAY_ITEMS = new ItemDrawerMenu[]{

        //
        new ItemDrawerMenu( R.string.prefs_delete_timings, new ItemDrawerMenu.OnClickListener() {
            @Override
            public void onClick() {
                WorkItOutMain.this.clearAllInput();
            }
        }),

        //
        new ItemDrawerMenu( R.string.send_email, new ItemDrawerMenu.OnClickListener() {
            @Override
            public void onClick() {
                WorkItOutMain.this.sendMail();
            }
        }),

        //  Vecchie "badgate"
        new ItemDrawerMenu( R.string.past_sessions_working, SessionWorkingsList.class ),

        //  Settings
        new ItemDrawerMenu( R.string.action_settings, PreferencesActivity.class, new ItemDrawerMenu.OnClickListener() {
            @Override
            public void onClick() {
                PreferencesActivity.activity_main = WorkItOutMain.this;
            }
        }),

        //  About WorkItOut
        new ItemDrawerMenu( R.string.about_workitout, AboutActivity.class ),

    };


	private TextView timeToLeave;
    private View rowTimeToLeave;

	private EditText entranceText;
	private EditText lunchInText;
	private EditText lunchOutText;
	private EditText exitText;
	private TextView workdayLength;
	private LinearLayout extraTimeLayout;

    private CounterWorkingTime chronoWorkingTime = null;

	private int optionSelected = 0;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_work_it_out_main);

        getActionBar().setTitle(getString(R.string.app_name));

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
        rowTimeToLeave = findViewById(R.id.row_time_to_leave);
        workdayLength = (TextView)findViewById(R.id.workday_length);
        extraTimeLayout = (LinearLayout)findViewById(R.id.extraTimeLayout);

        chronoWorkingTime = new CounterWorkingTime( (TextView) findViewById(R.id.chrono_working_time) );

		updateWorkDayLength();

        DateTime entranceTime = BadgeHelper.getEntranceTime();
        if( entranceTime.getMillis() != 0 ){
            entranceText.setText( BadgeHelperFormat.formatTime(entranceTime) );
            setTextColor(entranceText, entranceTime );
        }



        DateTime lunchInTime = BadgeHelper.getLunchInTime();
        if( lunchInTime.getMillis() != 0 ){
            lunchInText.setText( BadgeHelperFormat.formatTime(lunchInTime) );
            setTextColor(entranceText,lunchInTime);
            if( !BadgeHelper.isYesterday(lunchInTime) ){
                extraTimeLayout.setVisibility(View.VISIBLE);
            }else{
                extraTimeLayout.setVisibility(View.GONE);
            }
        }

        DateTime lunchOutTime = BadgeHelper.getLunchOutTime();
        if( lunchOutTime.getMillis() != 0 ){
            lunchOutText.setText( BadgeHelperFormat.formatTime(lunchOutTime) );
            setTextColor(entranceText,lunchOutTime);
        }

        DateTime exitTime = BadgeHelper.getExitTime();
        if( exitTime.getMillis() != 0 ){
            exitText.setText( BadgeHelperFormat.formatTime(exitTime) );
            setTextColor(exitText,exitTime);
        }

		updateEstimatedTimeOfExit();
		
		entranceText.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {
				entranceText.setText("");
                BadgeHelper.setEntranceTime(0);
                updateEstimatedTimeOfExit();
				return true;
			}
		});
		
		entranceText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
                optionSelected = entranceText.getId();
                chooseTime( BadgeHelper.getEntranceTime() );
			}
		});
		
		lunchOutText.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				lunchOutText.setText("");
                BadgeHelper.setLunchOutTime(0);
				return true;
			}
		});
		
		lunchOutText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
                optionSelected = lunchOutText.getId();
                chooseTime( BadgeHelper.getLunchOutTime() );
			}
		});

		lunchInText.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				lunchInText.setText("");
                BadgeHelper.setLunchInTime(0);
				return true;
			}
		});
		
		lunchInText.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
                optionSelected = lunchInText.getId();
                chooseTime( BadgeHelper.getLunchInTime() );
			}
		});

		exitText.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				exitText.setText("");
                BadgeHelper.setExitTime(0);
				startCountForExtraTime();
				return true;
			}
		});

		exitText.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
                optionSelected = exitText.getId();
                chooseTime( BadgeHelper.getExitTime() );
			}
		});
		
		entranceButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                BadgeHelper.setEntranceTime( setActualTime(entranceText) );
				entranceActions();
			}
		});
				
		lunchInButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				BadgeHelper.setLunchInTime( setActualTime(lunchInText) );
				lunchInActions();
			}
		});
		
		lunchOutButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                BadgeHelper.setLunchOutTime( setActualTime(lunchOutText) );
				lunchOutActions();
			}
		});

		exitButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				extraTimeLayout.setVisibility(View.VISIBLE);
                BadgeHelper.setExitTime( setActualTime(exitText) );
				toggleCountForExtraTime();
				removeAlarm();
			}
		});

//		Button clearButton = (Button) findViewById(R.id.clear);
//		clearButton.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				clearAllInput();
//			}
//		});

        drawerLayoutHelper = new DrawerLayoutHelper( WorkItOutMain.this, R.id.main_layout, R.id.left_drawer, ARRAY_ITEMS );

		//  If application drawer was never opened manually, automatically open it at first application run
		if( SettingsWorkitout.isFirstRun() ){
            drawerLayoutHelper.open();
		}

		//  This button enables a notification before the break ends
		pauseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {

                    long now = DateTime.now().getMillis();
					long pauseInMillis = SettingsWorkitout.getPauseDuration() * 60000; //   break duration - in millis

                    if( SettingsWorkitout.isPausesCounted() ) {
                        PauseWorking pause = PauseWorking.newInstance();
                        pause.setStartDate(now);
                        pause.setEndDate(now + pauseInMillis);
                        BadgeHelper.addPause(pause);
                    }

                    if( SettingsWorkitout.isNotificationsEnabled() ) {
                        long intervalForNotification = pauseInMillis - 120000;     // Notification is 2 minutes before the end of the break

                        Intent i = new Intent(getBaseContext(), PauseNotificationService.class);
                        PendingIntent pi = PendingIntent.getService(getBaseContext(), 2, i, 0);
                        AlarmManager mAlarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
                        mAlarm.set(AlarmManager.RTC_WAKEUP, (now + intervalForNotification), pi);
                    }
			}
		});
	}

	private void toggleCountForExtraTime() {

        if( BadgeHelper.getCurrentSessionWorking().getExitDate() != 0 ){
            chronoWorkingTime.stop();
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll(); //    When application is open all its notifications must be deleted
            extraTimeLayout.setVisibility(View.GONE);
        }else{
            extraTimeLayout.setVisibility(View.VISIBLE);
        }

	}

	private void startCountForExtraTime() {
		extraTimeLayout.setVisibility(View.VISIBLE);
        chronoWorkingTime.restart();
        BadgeHelper.setTimerMarching(true);
	}

	private void entranceActions() {

		entranceText.setTextColor(Color.BLACK);
		
		//  Blanks out all previous entrance and exit timings, if timings are yesterday ones
		if( BadgeHelper.isYesterday(BadgeHelper.getLunchInTime()) ){
//            lunchInTime.setTime(0);
            BadgeHelper.setLunchInTime(0);
		    lunchInText.setText("");
		}

		if( BadgeHelper.isYesterday(BadgeHelper.getLunchOutTime()) ){
            BadgeHelper.setLunchOutTime(0);
		    lunchOutText.setText("");
		}

		if( BadgeHelper.isYesterday(BadgeHelper.getExitTime()) ){
            BadgeHelper.setExitTime(0);
		    exitText.setText("");
		}

		//  Deletes all notifications
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();   //    When application is open all its notifications must be deleted

		extraTimeLayout.setVisibility(View.VISIBLE);

        if( BadgeHelper.getLunchInTime().getMillis() != 0 ){
            rowTimeToLeave.setVisibility(View.VISIBLE);
        }

        //  Ricalcola l'orario di uscita
        updateEstimatedTimeOfExit();
		removeAlarm();
	}

	private void lunchOutActions() {
		lunchOutText.setTextColor(Color.BLACK);

		//  Ricalcola l'orario di uscita
		updateEstimatedTimeOfExit();

		extraTimeLayout.setVisibility(View.GONE);
        rowTimeToLeave.setVisibility(View.INVISIBLE);
	}
	
	private void lunchInActions() {
		//  Ricalcola l'orario di uscita
		updateEstimatedTimeOfExit();

        rowTimeToLeave.setVisibility(View.VISIBLE);
		extraTimeLayout.setVisibility(View.VISIBLE);

		lunchInText.setTextColor(Color.BLACK);

        DateTime calcExitTime = new DateTime(BadgeHelper.getCurrentSessionWorking().calcExitTime());

		//  Set up notification for proper time
		if( calcExitTime.isAfterNow() ){

            if( SettingsWorkitout.isNotificationsEnabled() ) {
                Intent i = new Intent(getBaseContext(), NotificationService.class);
                PendingIntent pi = PendingIntent.getService(getBaseContext(), 0, i, 0);
                AlarmManager mAlarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
                //  Debug: the line below allows to set notification to 5 seconds in future.
//        		mAlarm.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()+(10*1000)), pi);
                mAlarm.set(AlarmManager.RTC_WAKEUP, calcExitTime.getMillis(), pi);
            }

            Utils.Toaster(getBaseContext(), getString(R.string.alarm_activated) + " " + BadgeHelperFormat.formatTime(calcExitTime), 2000);
		}

		startCountForExtraTime();
	}

    public void chooseTime( DateTime date ){
        if( BadgeHelper.isYesterday(date) ){
            chooseTime( null, null );
        }else{
            chooseTime( date.getHourOfDay(), date.getMinuteOfHour() );
        }
    }

    public void chooseTime( Integer hours, Integer minutes ){
        final TimePickerDialog timeFragment = new TimePickerDialog( WorkItOutMain.this );

        switch( optionSelected ){
            case R.id.entrance_text:
                timeFragment.getDialog().setTitle(R.string.entranceButtonLabel);
                break;
            case R.id.lunch_out_text:
                timeFragment.getDialog().setTitle(R.string.lunchOutButtonLabel);
                break;
            case R.id.lunch_in_text:
                timeFragment.getDialog().setTitle(R.string.lunchInButtonLabel);
                break;
            case R.id.exit_text:
                timeFragment.getDialog().setTitle(R.string.exitButtonLabel);
                break;
        }

        timeFragment.setCallbacks(new TimePickerDialog.Callbacks() {

            @Override
            public void onButtonPositiveClicked( Dialog dialog, int hour, int minute ){

                MutableDateTime date = MutableDateTime.now();
                date.setHourOfDay(hour);
                date.setMinuteOfHour(minute);
                date.setSecondOfMinute(0);

                String formatted = BadgeHelperFormat.formatTime(date);

                switch( optionSelected ){
                    case R.id.entrance_text:
                        BadgeHelper.setEntranceTime(date.toDateTime());
                        entranceText.setText(formatted);
                        entranceActions();
                        break;
                    case R.id.lunch_out_text:
                        BadgeHelper.setLunchOutTime( date.toDateTime() );
                        lunchOutText.setText(formatted);
                        lunchOutActions();
                        break;
                    case R.id.lunch_in_text:
                        BadgeHelper.setLunchInTime( date.toDateTime());
                        lunchInText.setText(formatted);
                        lunchInActions();
                        break;
                    case R.id.exit_text:
                        BadgeHelper.setExitTime(date.toDateTime());
                        exitText.setText(formatted);
                        //  It must be stopped in any case, so we put it true. Next method will make it false and stop timer.
                        BadgeHelper.setTimerMarching(true);

                        toggleCountForExtraTime();
                        chronoWorkingTime.stop();
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

        timeFragment.show( getSupportFragmentManager(), "timeFragment" );
    }

	/*
	 * Updates workday length by calling a helper function
	 */
	public void updateWorkDayLength(){
        workdayLength.setText( BadgeHelperFormat.formatPeriod(BadgeHelper.getWorkTimeInMillis()) );
	}

    public void triggerResumeFragments(){
        onResumeFragments();
    }

	@Override
	protected void onResumeFragments() {
		super.onResumeFragments();

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll(); //    When application is open all its notifications must be deleted

//		EasyTracker.getInstance().activityStart(this); // Add this method.

        if( BadgeHelper.getCurrentSessionWorking().calcExitTime() != null ){
            startCountForExtraTime();
            (findViewById(R.id.row_time_to_leave)).setVisibility(View.VISIBLE);
            extraTimeLayout.setVisibility(View.VISIBLE);
        }else{
            (findViewById(R.id.row_time_to_leave)).setVisibility(View.INVISIBLE);
            extraTimeLayout.setVisibility(View.GONE);
        }

		setTextColor( entranceText, BadgeHelper.getEntranceTime() );
		setTextColor( lunchInText, BadgeHelper.getLunchInTime() );
		setTextColor( lunchOutText, BadgeHelper.getLunchOutTime() );
		
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
		chronoWorkingTime.getTextView().setText("");
		exitText.setText("");

        rowTimeToLeave.setVisibility(View.INVISIBLE);
		extraTimeLayout.setVisibility(View.GONE);

		removeAlarm();
	}

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
	
	private DateTime setActualTime( TextView textToChange ){
		//  Prepara il timestamp
		DateTime now = DateTime.now();

		//  Scrive il timestamp nella casella di testo
		textToChange.setText( BadgeHelperFormat.formatTime(now) );

		return now;
	}

	public void updateEstimatedTimeOfExit(){
        Long time = BadgeHelper.getCurrentSessionWorking().calcExitTime();
        if( time == null ){
            return;
        }
        timeToLeave.setText( BadgeHelperFormat.formatTime(time) );

        if( chronoWorkingTime.isRunning() ){
            chronoWorkingTime.restart();
        }
	}

    private void setTextColor( EditText inputText, DateTime inputDate ){
        //  Giornata lavorativa di X ore
        if( DateTime.now().getMillis() > (inputDate.getMillis() + SettingsWorkitout.getWorkTime().getMillis()) ){
            inputText.setTextColor(Color.GRAY);
            exitText.setTextColor(Color.GRAY);
        }else{
            inputText.setTextColor(Color.BLACK);
            exitText.setTextColor(Color.BLACK);
        }
    }

	private void removeAlarm(){

		Intent i = new Intent(getBaseContext(), NotificationService.class);
		PendingIntent pi = PendingIntent.getService(getBaseContext(), 0, i, 0);
		AlarmManager mAlarm = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);

		mAlarm.cancel(pi);
		pi.cancel();
	}

	@Override
	public boolean onKeyDown( final int keyCode, final KeyEvent event ){
		if( keyCode == KeyEvent.KEYCODE_MENU ){
			drawerLayoutHelper.toggle();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void sendMail() {

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra( Intent.EXTRA_SUBJECT, getString(R.string.email_subject) + BadgeHelperFormat.formatDateTime(DateTime.now()) );
		i.putExtra( Intent.EXTRA_TEXT,
            getString(R.string.email_in_time) + BadgeHelperFormat.getEntranceTime()
            + "\n" + getString(R.string.email_lunch_time) + BadgeHelperFormat.getLunchOutTime()
            + "\n" + getString(R.string.email_back_from_lunch) + BadgeHelperFormat.getLunchInTime()
            + "\n" + getString(R.string.email_exit_time) + BadgeHelperFormat.getExitTime()
            + "\n" + getString(R.string.email_total_time) + BadgeHelperFormat.formatTime(BadgeHelper.getCurrentSessionWorking().calcExitTime())
//            + "\n" + getString(R.string.email_extra_time) + extraTimeText.getText()
        );

		try {
			startActivity(Intent.createChooser(i, getString(R.string.send_email)));
		} catch (android.content.ActivityNotFoundException ex) {
			Utils.Toaster( WorkItOutMain.this, getString(R.string.no_email_client), Utils.Toaster.LENGTH_LONG );
		}
	}



    @Override
    protected void onPause() {
        super.onPause();

        BadgeHelper.saveCurrentSession();

        chronoWorkingTime.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //  Se l'app viene riaperta dalla memoria, non deve più essere impostato il "sono al primo avvio" :)
        //  TODO: Marco se hai idee in merito a questo marchingegno, fai un fischio oppure pusha direttamente il fix :D
        PreferencesManager.setFirstRun(false,true);
    }

    @Override
    public void onStop() {
        super.onStop();
//        EasyTracker.getInstance().activityStop(this); // Add this method.
    }

}
