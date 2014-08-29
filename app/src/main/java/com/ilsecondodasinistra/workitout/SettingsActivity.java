package com.ilsecondodasinistra.workitout;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.prefs);
        
//        EditTextPreference pref = (EditTextPreference)findPreference("workday_hours");
//        pref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    }
    
    @Override
    protected void onDestroy() {
    	Intent returnIntent = new Intent();
    	 returnIntent.putExtra("result","backFromSettings");
    	 setResult(RESULT_OK,returnIntent);     
    	 finish();
    	super.onDestroy();
    	
//        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putString("workday_hours", value);
//        editor.commit();  
    }
}