package com.ilsecondodasinistra.workitout;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.ilsecondodasinistra.workitout.utils.BadgeHelper;

import org.joda.time.DateTime;

import it.lucichkevin.cip.Utils;

public class MyWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_BADGE = "ACTION_BADGE";

    private static final int ACTION_ENTRANCE = 1;
    private static final int ACTION_LUNCH_OUT = 2;
    private static final int ACTION_LUNCH_IN = 3;
    private static final int ACTION_EXIT = 4;

    private static int CURRENT_ACTION = 1;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        setCurrentAction();
        String textButton = getButtonText(context);

        //  Get all ids
        ComponentName thisWidget = new ComponentName(context, MyWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int widgetId : allWidgetIds) {

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            //    Register an onClickListener
            Intent intent = new Intent(context, MyWidgetProvider.class);
            intent.setAction(MyWidgetProvider.ACTION_BADGE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.btn_badge, pendingIntent);
            remoteViews.setTextViewText(R.id.btn_badge, textButton);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction().equals(MyWidgetProvider.ACTION_BADGE)) {

            String toastText = "";
            DateTime now = DateTime.now();

            switch (CURRENT_ACTION) {
                case 1:
                    BadgeHelper.setEntranceTime(now);
                    toastText = "Bentornato a lavoro!";
                    break;
                case 2:
                    BadgeHelper.setLunchOutTime(now);
                    toastText = "Buon pranzo!";
                    break;
                case 3:
                    BadgeHelper.setLunchInTime(now);
                    toastText = "Si ricomincia... tieni duro!";
                    break;
                case 4:
                    BadgeHelper.setExitTime(now);
                    toastText = "È passato velocemente, no?";
                    break;
            }

            Utils.Toaster(context, toastText);

            //  call onUpdate()
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), MyWidgetProvider.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }

    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        //  Quando viene eliminato...
        Utils.Toaster(context, "Perchè mi abbandoni?!", Utils.Toaster.LENGTH_SHORT);
    }

    private String getButtonText(Context context) {

        switch (CURRENT_ACTION) {
            case 1:
                return context.getString(R.string.entranceButtonLabel);
            case 2:
                return context.getString(R.string.lunchOutButtonLabel);
            case 3:
                return context.getString(R.string.lunchInButtonLabel);
            case 4:
                return context.getString(R.string.exitButtonLabel);
        }

        return "";
    }

    private void setCurrentAction() {
        if (BadgeHelper.getEntranceTime().getMillis() == 0 || BadgeHelper.isYesterday(BadgeHelper.getEntranceTime())) {
            CURRENT_ACTION = ACTION_ENTRANCE;
        } else if (BadgeHelper.getLunchOutTime().getMillis() == 0 || BadgeHelper.isYesterday(BadgeHelper.getLunchOutTime())) {
            CURRENT_ACTION = ACTION_LUNCH_OUT;
        } else if (BadgeHelper.getLunchInTime().getMillis() == 0 || BadgeHelper.isYesterday(BadgeHelper.getLunchInTime())) {
            CURRENT_ACTION = ACTION_LUNCH_IN;
        } else {
            CURRENT_ACTION = ACTION_EXIT;
        }
    }

}
