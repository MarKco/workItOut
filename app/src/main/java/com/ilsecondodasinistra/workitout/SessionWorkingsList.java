package com.ilsecondodasinistra.workitout;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.ilsecondodasinistra.workitout.database.PauseWorking;
import com.ilsecondodasinistra.workitout.database.SessionWorking;
import com.ilsecondodasinistra.workitout.utils.BadgeHelperFormat;
import com.ilsecondodasinistra.workitout.utils.DatabaseHelper;

import java.util.ArrayList;

import it.lucichkevin.cip.ObjectAdapter;

/**
 * Created by kevin on 10/09/14.
 */
public class SessionWorkingsList extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<SessionWorking> _sessions = (ArrayList<SessionWorking>) DatabaseHelper.getDaoSession().getSessionWorkingDao().queryBuilder().list();
        SessionWorking[] sessions = new SessionWorking[_sessions.size()];
        sessions = _sessions.toArray(sessions);

        ObjectAdapter<SessionWorking> adapter = new ObjectAdapter<SessionWorking>( getBaseContext(), R.layout.sessionworkingsitem, sessions ){
            @Override
            protected void attachItemToLayout( SessionWorking session, int position ){

                String total_work_time = "Stai ancora lavorando....";
                if( session.getExitDate() != 0 ){
                    total_work_time = session.getTimeWorked().toString(BadgeHelperFormat.HHmm);
                }

                ((TextView) findViewById(R.id.sessionworking_date)).setText( BadgeHelperFormat.formatDateTime(session.getEntranceDate()) );
                ((TextView) findViewById(R.id.total_work_time)).setText( total_work_time );
                ((TextView) findViewById(R.id.total_pauses_time)).setText( BadgeHelperFormat.formatPeriod(session.getAllPausesDuration()) );

                ((TextView) findViewById(R.id.entrance_time)).setText( BadgeHelperFormat.formatTime(session.getEntranceDate()) );
                ((TextView) findViewById(R.id.exit_time)).setText( BadgeHelperFormat.formatTime(session.getExitDate()) );

                PauseWorking pauseLunch = session.getPauseOfLunch();
                ((TextView) findViewById(R.id.lunchout_time)).setText( BadgeHelperFormat.formatTime(pauseLunch.getStartDate()) );
                ((TextView) findViewById(R.id.lunchin_time)).setText( BadgeHelperFormat.formatTime(pauseLunch.getEndDate()) );

            }
        };

        setListAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ){
        switch( item.getItemId() ){
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
