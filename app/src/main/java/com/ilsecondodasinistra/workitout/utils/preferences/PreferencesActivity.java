package com.ilsecondodasinistra.workitout.utils.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import com.ilsecondodasinistra.workitout.R;


/**
 * Created by Kevin Lucich on 15/03/14.
 */
public class PreferencesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace( android.R.id.content, new SettingsFragment() )
                .commit();

        // Enabling Up navigation
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
//        getActionBar().setLogo(R.drawable.ic_launcher);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        /*
            When the back button on the ActionBar is pressed... well, go back!
        */
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }

    }
}
