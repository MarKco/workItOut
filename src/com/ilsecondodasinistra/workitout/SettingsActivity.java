package com.ilsecondodasinistra.workitout;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.text.InputType;
public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.prefs);
        
//        EditTextPreference pref = (EditTextPreference)findPreference("workday_hours");
//        pref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    }
    
//    @Override
//    protected void onDestroy() {
//    	// TODO Auto-generated method stub
//    	super.onDestroy();
//    	
//        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putString("workday_hours", value);
//        editor.commit();  
//    }
}