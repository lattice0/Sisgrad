package com.lucaszanella.sisgrad;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by lucaszanella on 7/29/16.
 */
public class TimeAgo {
    public static Integer ONE_MINUTE = 60; //1 minute in seconds
    public static Integer ONE_HOUR = ONE_MINUTE*60;//1 hour in seconds
    public static Integer ONE_DAY = ONE_HOUR*24;//1 day in seconds
    public static Integer ONE_WEEK = ONE_DAY*7;//1 week in seconds
    public static Integer ONE_MONTH_AVERAGE = ONE_DAY*30;//1 average month in seconds
    public static Integer ONE_YEAR = ONE_DAY*365;//1 average year in seconds

    private static Integer ApproximateQuantity (Long difference) {
        return Double.valueOf(Math.floor(difference)).intValue();
    }

    private static String returnDay(Long unixNow) {
        Date date = new Date(unixNow*1000L); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM"); // the format of your date
        sdf.setTimeZone(TimeZone.getTimeZone("GMT-3")); // give a timezone reference for formatting
        return sdf.format(date);
    }
    public static String TimeAgo (Long unixNow, Long unixBefore) {
        Long differenceInSeconds = unixNow - unixBefore;
        String timeText = "";
        if (differenceInSeconds<ONE_HOUR) {//less than 1 hour
            timeText = ApproximateQuantity(differenceInSeconds/ONE_HOUR)+"min";
        } else if (differenceInSeconds<ONE_DAY) {//less than 1 day
            timeText = ApproximateQuantity(differenceInSeconds/ONE_DAY)+"h";
        } else if (differenceInSeconds<ONE_WEEK){
            timeText = returnDay(unixBefore);
        } else {
            timeText = returnDay(unixBefore);
        }
        return timeText;
    }
}
