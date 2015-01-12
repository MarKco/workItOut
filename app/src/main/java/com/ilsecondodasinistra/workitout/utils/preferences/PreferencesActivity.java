package com.ilsecondodasinistra.workitout.utils.preferences;


import android.app.Activity;
import android.preference.Preference;

import com.ilsecondodasinistra.workitout.R;
import com.ilsecondodasinistra.workitout.WorkItOutMain;
import com.ilsecondodasinistra.workitout.utils.BadgeHelper;
import com.ilsecondodasinistra.workitout.utils.SettingsWorkitout;

import org.joda.time.Duration;

import it.lucichkevin.cip.Utils;
import it.lucichkevin.cip.preferencesmanager.activity.AbstractPreferencesListActivity;
import it.lucichkevin.cip.preferencesmanager.activity.ItemPreference;

/**
 * Created by Kevin Lucich on 15/03/14.
 */
public class PreferencesActivity extends AbstractPreferencesListActivity {

    public static Activity activity_main = null;

    @Override
    protected void populatePreferencesList() {
        super.populatePreferencesListWithDefault();

        ItemPreference ip;

        //  Duration in hours and minutes
        ip = new ItemPreference( SettingsWorkitout.WORK_TIME, R.string.prefs_choose_workday_length, R.string.empty_string, ItemPreference.TYPE_TIMEPICKER, 480 );
        ip.setOnPreferenceChangeListener(onPreferenceChangeListener);
        items.add(ip);

        ip = new ItemPreference( SettingsWorkitout.PAUSE_DURATION, R.string.change_break_time, R.string.empty_string, ItemPreference.TYPE_MINUTEPICKER, 15 );
        ip.setOnPreferenceChangeListener(onPreferenceChangeListener);
        items.add(ip);

        ip = new ItemPreference( SettingsWorkitout.NOTIFICATIONS_ENABLED, R.string.setting_notifications_enabled, R.string.change_minimum_break_time_desc, ItemPreference.TYPE_SWITCH, true );
        ip.setOnPreferenceChangeListener(onPreferenceChangeListener);
        items.add(ip);

        ip = new ItemPreference( SettingsWorkitout.MINIMUM_PAUSE_DURATION, R.string.change_minimum_break_time, R.string.change_minimum_break_time_desc, ItemPreference.TYPE_TIMEPICKER, 0 );
        ip.setOnPreferenceChangeListener(onPreferenceChangeListener);
        items.add(ip);

        ip = new ItemPreference( SettingsWorkitout.PAUSE_COUNTED_FOR_EXIT, R.string.setting_pauses_count_for_exit, R.string.setting_pauses_count_for_exit_desc, ItemPreference.TYPE_SWITCH, false );
        ip.setOnPreferenceChangeListener(onPreferenceChangeListener);
        items.add(ip);
    }


    private Preference.OnPreferenceChangeListener onPreferenceChangeListener = new Preference.OnPreferenceChangeListener(){
        @Override
        public boolean onPreferenceChange( Preference preference, Object newValue ){

            if( activity_main != null ) {
                if( preference.getKey().equals(SettingsWorkitout.WORK_TIME) ){
                    BadgeHelper.setWorkTime( new Duration( ((Integer) newValue)*60000 ) );
                }

                ((WorkItOutMain) activity_main).triggerResumeFragments();
                ((WorkItOutMain) activity_main).updateWorkDayLength();
                ((WorkItOutMain) activity_main).updateEstimatedTimeOfExit();
            }

            return true;
        }
    };

}
