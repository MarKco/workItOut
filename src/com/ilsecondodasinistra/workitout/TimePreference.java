package com.ilsecondodasinistra.workitout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

/**
 * Custom preference for time selection. Hour and minute are persistent and
 * stored separately as ints in the underlying shared preferences under keys
 * KEY.hour and KEY.minute, where KEY is the preference's key.
 */
public class TimePreference extends DialogPreference {
  
  /** The widget for picking a time */
  private TimePicker timePicker;

  /** Default hour */
  private static final int DEFAULT_HOUR = 8;

  /** Default minute */
  private static final int DEFAULT_MINUTE = 0;

  /**
   * Creates a preference for choosing a time based on its XML declaration.
   * 
   * @param context
   * @param attributes
   */
  public TimePreference(Context context,
                        AttributeSet attributes) {
    super(context, attributes);
    setPersistent(false);
  }

  /**
   * Initialize time picker to currently stored time preferences.
   * 
   * @param view
   * The dialog preference's host view
   */
  @Override
  public void onBindDialogView(View view) {
    super.onBindDialogView(view);
    timePicker = (TimePicker) view.findViewById(R.id.prefTimePicker);
    timePicker.setCurrentHour(getSharedPreferences().getInt(getKey() + ".hour", DEFAULT_HOUR));
    timePicker.setCurrentMinute(getSharedPreferences().getInt(getKey() + ".minute", DEFAULT_MINUTE));
    timePicker.setIs24HourView(true);
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
  protected void onDialogClosed(boolean okToSave) {
    super.onDialogClosed(okToSave);
    if (okToSave) {
      timePicker.clearFocus();
      SharedPreferences.Editor editor = getEditor();
      editor.putInt(getKey() + ".hour", timePicker.getCurrentHour());
      editor.putInt(getKey() + ".minute", timePicker.getCurrentMinute());
      editor.commit();
    }
  }
}