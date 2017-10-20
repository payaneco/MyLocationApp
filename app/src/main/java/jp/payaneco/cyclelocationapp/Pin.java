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

    public boolean isInPlace(double pLatitude, double pLongitude) {
        float[] results = new float[3];
        Location.distanceBetween(pLatitude, pLongitude, latitude, longitude, results);
        return (results[0] < 2 * 1000); //半径2キロ以内
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

    //動かないのでボツ
    public String getDiff(double pLatitude, double pLongitude) {
        float[] results = new float[3];
        Location.distanceBetween(pLatitude, pLongitude, latitude, longitude, results);
        double degree = Math.toDegrees(results[2]) - 11.75 + 90d;
        int dp = (int)(degree / 22.5);
        String direction;
        switch (dp) {
            case 1: direction = "北北東"; break;
            case 2: direction = "北東"; break;
            case 3: direction = "東北東"; break;
            case 4: direction = "東"; break;
            case 5: direction = "東南東"; break;
            case 6: direction = "南東"; break;
            case 7: direction = "南南東"; break;
            case 8: direction = "南"; break;
            case 9: direction = "南南西"; break;
            case 10: direction = "南西"; break;
            case 11: direction = "西南西"; break;
            case 12: direction = "西"; break;
            case 13: direction = "西北西"; break;
            case 14: direction = "北西"; break;
            case 15: direction = "北北西"; break;
            default: direction = "北"; break;
        }
        return String.format("%s%2$.1fkm地点", direction, results[0] / 1000d);
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
