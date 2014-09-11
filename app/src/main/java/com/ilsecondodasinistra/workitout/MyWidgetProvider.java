package com.ilsecondodasinistra.workitout;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.ilsecondodasinistra.workitout.utils.BadgeHelper;

import java.util.Calendar;
import java.util.Date;

import it.lucichkevin.cip.Utils;

public class MyWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_BADGE = "action_badge";

	@Override
	public void onUpdate( Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds ){

	    //  Get all ids
	    ComponentName thisWidget = new ComponentName( context, MyWidgetProvider.class );
	    int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for( int widgetId : allWidgetIds ){

	        RemoteViews remoteViews = new RemoteViews( context.getPackageName(), R.layout.widget_layout );

	        //    Register an onClickListener
	        Intent intent = new Intent( context, MyWidgetProvider.class );
            intent.setAction(MyWidgetProvider.ACTION_BADGE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast( context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
	        remoteViews.setOnClickPendingIntent( R.id.btn_badge, pendingIntent );
	        appWidgetManager.updateAppWidget( widgetId, remoteViews );
	    }
	}

	@Override
	public void onReceive( Context context, Intent intent ){
		super.onReceive(context, intent);

        Utils.logger("onReceive() :D  intent.getAction() = "+ intent.getAction(), Utils.LOG_DEBUG );

		if( intent.getAction().equals(MyWidgetProvider.ACTION_BADGE) ){
            Utils.logger("ho premuto il widget! :D", Utils.LOG_DEBUG );

            Calendar now = Calendar.getInstance();

            Date date = BadgeHelper.getEntranceTime();
            Utils.logger( "EntranceTime = "+ date.getHours() +":"+ date.getMinutes(), Utils.LOG_DEBUG );

            date = now.getTime();
            Utils.logger( "setEntrance = "+ date.getHours() +":"+ date.getMinutes(), Utils.LOG_DEBUG );

//            Utils.Toaster( context, "EntranceTime = "+ date.getHours() +":"+ date.getMinutes() );
        }

	}

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        //  Quando viene eliminato...
        Utils.Toaster(context, "Perchè mi abbandoni?!", Utils.Toaster.LENGTH_SHORT);
    }
}
