package com.ilsecondodasinistra.workitout;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.ilsecondodasinistra.workitout.database.Entity_PauseWorking;
import com.ilsecondodasinistra.workitout.database.Entity_SessionWorking;
import com.ilsecondodasinistra.workitout.utils.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import it.lucichkevin.cip.ObjectAdapter;
import it.lucichkevin.cip.Utils;

/**
 * Created by kevin on 10/09/14.
 */
public class SessionWorkingsList extends ListActivity {

    private SimpleDateFormat hhmm = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat ddMMyyyy = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<Entity_SessionWorking> ssss = (ArrayList<Entity_SessionWorking>) DatabaseHelper.getDaoSession().getEntity_SessionWorkingDao().queryBuilder().list();

        Entity_SessionWorking[] sessions = new Entity_SessionWorking[ssss.size()];

        int i = 0;
        for( Entity_SessionWorking s : ssss ){
            sessions[i] = s;
            i++;
        }

        Utils.logger("sessions = "+ sessions, Utils.LOG_DEBUG );
        Utils.logger("sessions.size() = "+ ssss.size(), Utils.LOG_DEBUG );

        ObjectAdapter<Entity_SessionWorking> adapter = new ObjectAdapter<Entity_SessionWorking>( getBaseContext(), R.layout.sessionworkingsitem, sessions ){
            @Override
            protected void attachItemToLayout( Entity_SessionWorking session, int position ){

                Date entranceDate = session.getEntranceDate();

//                Utils.logger(" ===> "+ session.getExitDate(), Utils.LOG_DEBUG );

                ((TextView) findViewById(R.id.sessionworking_date)).setText( ddMMyyyy.format(entranceDate) );
                ((TextView) findViewById(R.id.total_work_time)).setText( hhmm.format(session.getTimeWorked()));

                ((TextView) findViewById(R.id.entrance_time)).setText( hhmm.format(session.getEntranceDate()) );
                ((TextView) findViewById(R.id.exit_time)).setText( hhmm.format(session.getExitDate()) );


                Entity_PauseWorking pauseLunch = session.getPauseOfLunch();
                Date lunchOut = pauseLunch.getStartDate();
                Date lunchIn = pauseLunch.getEndDate();
                ((TextView) findViewById(R.id.lunchout_time)).setText( hhmm.format(lunchOut) );
                ((TextView) findViewById(R.id.lunchin_time)).setText( hhmm.format(lunchIn) );

            }
        };

        setListAdapter(adapter);
    }

}
