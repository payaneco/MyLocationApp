package jp.payaneco.cyclelocationapp;

import android.location.Location;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by payaneco on 2017/10/14.
 */

public class Pin {
    private boolean arrived;
    private Date arrivalTime;
    private double latitude; // 緯度
    private double longitude; // 経度
    private double distance;
    private String name;
    private int hour;
    private int minute;

    public Pin(double latitude, double longitude, double distance, String name, int hour, int minute)
    {
        arrived = false;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.name = name;
        this.hour = hour;
        this.minute = minute;
    }

    public boolean isArrived() {
        return arrived;
    }

    public void arrive() {
        arrived = true;
        arrivalTime = new Date();
    }

    public boolean isInPlace(double pLatitude, double pLongitude, int pDistance) {
        float[] results = new float[3];
        Location.distanceBetween(pLatitude, pLongitude, latitude, longitude, results);
        return (results[0] < pDistance);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistance() {
        return distance;
    }

    public String getName() {
        return name;
    }

    public String getTargetText() {
        return String.format("%02d:%02d", hour, minute);
    }

    public String getSavingText() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"), Locale.JAPAN);
        c.setTime(arrivalTime);
        int saving = (hour - c.get(Calendar.HOUR_OF_DAY)) * 60;
        saving += minute - c.get(Calendar.MINUTE);
        return String.format("貯金%+d分", saving);
    }

    public Date getArrivalTime() {
        return arrivalTime;
    }

    public String getTweet() {
        StringBuilder sb = new StringBuilder();
        DateFormat df = new SimpleDateFormat("HH:mm", Locale.JAPAN);
        df.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        sb.append(String.format("%sに%2$.1fkm地点の", df.format(arrivalTime), distance)).append(name).append("周辺に到着しました。\r\n");
        sb.append(getTargetText()).append("が到達目標時刻です。").append("\r\n");
        sb.append(getSavingText()).append("！").append("\r\n");
        sb.append(String.format("http://maps.google.com/maps?q=%1$.4f,%2$.4f", latitude, longitude));
        return sb.toString();
    }
}
