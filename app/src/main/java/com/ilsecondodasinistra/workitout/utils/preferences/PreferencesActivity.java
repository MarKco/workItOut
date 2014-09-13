package com.ilsecondodasinistra.workitout.utils.preferences;


import com.ilsecondodasinistra.workitout.R;
import com.ilsecondodasinistra.workitout.utils.SettingsWorkitout;

import it.lucichkevin.cip.preferencesmanager.activity.AbstractPreferencesListActivity;
import it.lucichkevin.cip.preferencesmanager.activity.ItemPreference;

/**
 * Created by Kevin Lucich on 15/03/14.
 */
public class PreferencesActivity extends AbstractPreferencesListActivity {

    @Override
    protected void populatePreferencesList() {
        super.populatePreferencesListWithDefault();

        items.add(new ItemPreference( SettingsWorkitout.PAUSE_DURATION, R.string.change_break_time, R.string.empty_string, ItemPreference.TYPE_MINUTEPICKER ));
    }

}
