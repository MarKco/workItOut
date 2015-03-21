package com.ilsecondodasinistra.workitout.utils;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.SimpleDateFormat;


/**
 * @author Kevin Lucich (11/09/14)
 */
public class CounterWorkingTime {

    private static final SimpleDateFormat hhmmssFormatter = new SimpleDateFormat("H:mm:ss");
    private static final int MSG = 1;
    private final static long interval = 1000;

    private boolean STARTED = false;
    //  Mi dice se sto facendo uno straordinario o no
    private TextView textView;
    private Long currentMillis;

    public CounterWorkingTime(TextView textView) {
        this.textView = textView;
    }

    public void calculateMillis() {
//        stop();
        Long time = BadgeHelper.getCurrentSessionWorking().calcExitTime();
        if (time == null) {
            currentMillis = null;
            return;
        }
        currentMillis = DateTime.now(DateTimeZone.UTC).getMillis() - time;
    }

    public void restart() {
        stop();
        start();
    }

    public synchronized final CounterWorkingTime start() {

        if (STARTED) {
            return this;
        }

        calculateMillis();

        if (currentMillis == null) {
            return this;
        }

        STARTED = true;

        mHandler.sendMessage(mHandler.obtainMessage(MSG));
        return this;
    }

    public CounterWorkingTime stop() {
        if (!STARTED) {
            return this;
        }
        STARTED = false;
        mHandler.removeMessages(MSG);
        return this;
    }

    public void onTick(long millis) {
        textView.setText(format(millis));
    }

    private String format(long millis) {

        int seconds = (int) millis / 1000;

        if (seconds < 0) {
            seconds *= -1;
        }

        int hours = seconds / 3600;
        int minutes = (seconds - hours * 3600) / 60;
        seconds = seconds - (hours * 3600) - (minutes * 60);

        return ((millis < 0) ? "-" : "") + pad(hours) + ":" + pad(minutes) + ":" + pad(seconds);
    }

    private String pad(int n) {
        if (n < 10) {
            return "0" + n;
        }
        return "" + n;
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            long lastTickStart = SystemClock.elapsedRealtime();

            // take into account user's onTick taking time to execute
            long delay = lastTickStart + interval - SystemClock.elapsedRealtime();

            currentMillis += delay;

            CounterWorkingTime.this.onTick(currentMillis);

            // special case: user's onTick took more than interval to
            // complete, skip to next interval
            while (delay < 0) {
                delay += interval;
            }

            sendMessageDelayed(obtainMessage(MSG), delay);

        }
    };


    ///////////////////////////
    //  Getters and Setter

    public TextView getTextView() {
        return textView;
    }

    public boolean isRunning() {
        return STARTED;
    }

}
