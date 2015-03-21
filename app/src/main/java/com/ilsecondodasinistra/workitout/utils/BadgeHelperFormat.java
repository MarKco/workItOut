package com.ilsecondodasinistra.workitout.utils;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.base.AbstractInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * Return the info formatted
 *
 * @author Kevin Lucich (12/09/14)
 */
public class BadgeHelperFormat {

    //  Time
    public static final PeriodFormatter HHmm = new PeriodFormatterBuilder()
            .printZeroAlways()
            .appendHours().minimumPrintedDigits(2)
            .appendSeparator(":")
            .appendMinutes().minimumPrintedDigits(2)
            .toFormatter();

    public static final PeriodFormatter HHmmss = new PeriodFormatterBuilder()
            .printZeroAlways()
            .appendHours().minimumPrintedDigits(2)
            .appendSeparator(":")
            .appendMinutes().minimumPrintedDigits(2)
            .appendSeparator(":")
            .appendSeconds().minimumPrintedDigits(2)
            .toFormatter();

    public static final DateTimeFormatter HHmmDate = DateTimeFormat.forPattern("HH:mm");

    //  Date
    private static final DateTimeFormatter ddMMyyyy = DateTimeFormat.forPattern("d/MM/yyyy");


    ////////////////////////////////////////////////////
    //  Format a date

    public static String formatDateTime(long longToFormat) {
        return formatDateTime(new DateTime(longToFormat));
    }

    public static String formatDateTime(DateTime dateTimeToFormat) {
        return dateTimeToFormat.toString(ddMMyyyy);
    }


    ////////////////////////////////////////////////////
    //  Format a time

    public static String formatTime(long longToFormat) {
        return formatTime(new DateTime(longToFormat));
    }

    public static <T extends AbstractInstant> String formatTime(T dateTimeToFormat) {
        return ((T) dateTimeToFormat).toString(HHmmDate);
    }

    public static String formatPeriod(Period periodToFormat) {
        return periodToFormat.toString(HHmm);
    }

    public static String formatPeriod(long longToFormat) {
        return formatPeriod(new Period(longToFormat));
    }

    public static String formatPeriodHHmmss(Period periodToFormat) {
        return periodToFormat.toString(HHmmss);
    }

    public static String formatPeriodHHmmss(long millis) {
        return formatPeriodHHmmss(new Period(millis));
    }


    ////////////////////////////////////////////////////
    //  Format a info of current session

    public static String getEntranceTime() {
        return formatTime(BadgeHelper.getEntranceTime());
    }

    public static String getLunchInTime() {
        return formatTime(BadgeHelper.getLunchInTime());
    }

    public static String getLunchOutTime() {
        return formatTime(BadgeHelper.getLunchOutTime());
    }

    public static String getExitTime() {
        return formatTime(BadgeHelper.getExitTime());
    }

}
