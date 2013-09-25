package com.ilsecondodasinistra.workitout;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;

/**
 * Custom preference for time selection. Hour and minute are persistent and
 * stored separately as ints in the underlying shared preferences under keys
 * KEY.hour and KEY.minute, where KEY is the preference's key.
 */
public class CustomTimeDialog extends Dialog implements android.view.View.OnClickListener {
  
  private Button okButton;
  private Button cancelButton;
	
  /** The widget for picking a time */
  private TimePicker timePicker;

  /** Default hour */
  private static final int DEFAULT_HOUR = 8;

  /** Default minute */
  private static final int DEFAULT_MINUTE = 0;
  
  private Context context;

  /**
   * Creates a preference for choosing a time based on its XML declaration.
   * 
   * @param context
   * @param attributes
   */
  public CustomTimeDialog(Context context) {
    super(context);
    this.context = context;
    this.setTitle(R.string.workday_duration_prefs_title);
    this.setContentView(R.layout.time_preference);
    
  }
  
  /**
   * Initialize time picker to currently stored time preferences.
   * 
   * @param view
   * The dialog preference's host view
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    timePicker = (TimePicker)findViewById(R.id.prefTimePicker);
    
    SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("WorkItOutMain", 0);
    Editor editor = prefs.edit();
    timePicker.setCurrentHour(prefs.getInt(context.getString(R.string.workday_hours) + ".hour", DEFAULT_HOUR));
    timePicker.setCurrentMinute(prefs.getInt(context.getString(R.string.workday_hours) + ".minute", DEFAULT_MINUTE));
    timePicker.setIs24HourView(true);
    
    findViewById(R.id.dialogButtonOK).setOnClickListener(this);
    findViewById(R.id.dialogButtonCancel).setOnClickListener(this);
  }
  
  /**
   * Handles closing of dialog. If user intended to save the settings, selected
   * hour and minute are stored in the preferences with keys KEY.hour and
   * KEY.minute, where KEY is the preference's KEY.
   * 
   * @param okToSave
   * True if user wanted to save settings, false otherwise
   */
  @Override
  protected void onStop() {
//    super.onDialogClosed(okToSave);
//    if (okToSave) {
	  saveData();
  }
//  }
  
  private void saveData() {
	  SharedPreferences prefs = (context.getSharedPreferences("WorkItOutMain", 0));
	  Editor editor = prefs.edit();
      timePicker.clearFocus();
      editor.putInt(context.getString(R.string.workday_hours) + ".hour", timePicker.getCurrentHour());
      editor.putInt(context.getString(R.string.workday_hours) + ".minute", timePicker.getCurrentMinute());
      editor.commit();
      
      TimingsObservable to = new TimingsObservable();
      to.addObserver((WorkItOutMain)context);
      to.updateWorkDayLength(context);
  }

@Override
public void onClick(View v) {
	// TODO Auto-generated method stub
	switch (v.getId()) {
	case R.id.dialogButtonOK:
		CustomTimeDialog.this.saveData();
		CustomTimeDialog.this.dismiss();
		break;
	default:
	   	CustomTimeDialog.this.dismiss();
		break;
	}
}
}