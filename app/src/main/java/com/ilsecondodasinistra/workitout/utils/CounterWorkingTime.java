package com.ilsecondodasinistra.workitout.utils;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;


/**
 *  @author     Kevin Lucich (11/09/14)
 */
public class CounterWorkingTime {

    private static final int MSG = 1;
    private final static long interval = 1000;

    private boolean STARTED = false;
    //  Mi dice se sto facendo uno straordinario o no
    private boolean overTime = false;
    private TextView textView;
    private long currentMillis;

    public CounterWorkingTime( TextView textView ){
        this.textView = textView;
    }

    public void calculateMillis(){
        stop();
        currentMillis = DateTime.now(DateTimeZone.UTC).getMillis() - BadgeHelper.getCurrentSessionWorking().calcExitTime();
    }

    public void restart(){
        stop();
        start();
    }

    public synchronized final CounterWorkingTime start() {
        if( STARTED ){
            return this;
        }

        calculateMillis();
        STARTED = true;

        if( currentMillis < 0 ){
            currentMillis *= -1;
            overTime = false;
        }else{
            overTime = true;
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG));
        return this;
    }

    public void stop(){
        if( !STARTED ){
            return;
        }
        STARTED = false;
        mHandler.removeMessages(MSG);
    }

    public void onTick( long millis ){
        textView.setText( ((overTime)?"":"-") + BadgeHelperFormat.formatPeriodHHmmss(millis) );
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            long lastTickStart = SystemClock.elapsedRealtime();

            // take into account user's onTick taking time to execute
            long delay = lastTickStart + interval - SystemClock.elapsedRealtime();

            if( overTime ){
                //  Se sto facendo straordinari, il counter deve auomentare
                currentMillis += delay;
            }else{
                //  Altrimenti diminuisco il tempo sperando che la giornata finisca presto :D
                currentMillis -= delay;
            }

            CounterWorkingTime.this.onTick(currentMillis);

            // special case: user's onTick took more than interval to
            // complete, skip to next interval
            while( delay < 0 ){
                delay += interval;
            }

            sendMessageDelayed(obtainMessage(MSG), delay);

        }
    };


    ///////////////////////////
    //  Getters and Setter

    public TextView getTextView(){
        return textView;
    }

}
