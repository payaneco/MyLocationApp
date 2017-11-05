package jp.payaneco.cyclelocationapp;

import android.database.Cursor;
import android.location.Location;

import org.xmlpull.v1.XmlPullParser;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by payaneco on 2017/10/14.
 */

public class Pin {
    public static final String DB_DATE_TEMPLATE = "yyyy/MM/dd HH:mm:ss";

    private static final Pattern TIME_PATTERN = Pattern.compile("(^|\\s):([0-9:]+)(!?)(\\s|$)");
    private static final Pattern DISTANCE_PATTERN = Pattern.compile("(^|\\s)@([0-9.]+)(!?)(\\s|$)");
    private static final Pattern QUIET_PATTERN = Pattern.compile("(^|\\s)/q(\\s|$)");

    private int id;
    private boolean arrived;
    private Date arrivalTime;
    private double latitude; // 緯度
    private double longitude; // 経度
    private double distance;
    private boolean absDistance;
    private String name;
    private int hour;
    private int minute;
    private boolean absTime;
    private boolean tweet;
    private String comment;
    private Pin nextPin;
    private String errMsg;

    public Pin() {
        name = "";
        errMsg = "";
        distance = Double.NaN;
    }

    public Pin(XmlPullParser parser) {
        this();
        try {
            latitude = Double.parseDouble(parser.getAttributeValue(null, "lat"));
            longitude = Double.parseDouble(parser.getAttributeValue(null, "lon"));
            int eventType = parser.next();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        String tag = parser.getName();
                        parser.next();
                        if (tag.equalsIgnoreCase("name")) {
                            name = parser.getText();
                        } else if (tag.equalsIgnoreCase("cmt")) {
                            setComment(parser.getText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (parser.getName().equalsIgnoreCase("wpt")) return;
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            errMsg = e.getMessage();
        }
    }

    public Pin(Cursor c) {
        this();
        try {
            id = c.getInt(c.getColumnIndex("lc_id"));
            latitude = c.getDouble(c.getColumnIndex("lc_lat"));
            longitude = c.getDouble(c.getColumnIndex("lc_lon"));
            name = c.getString(c.getColumnIndex("lc_name"));
            comment = c.getString(c.getColumnIndex("lc_cmt"));
            setTargetTime(c.getString(c.getColumnIndex("lc_target_time")));
            distance = c.getDouble(c.getColumnIndex("lc_distance"));
            absDistance = true;
            tweet = (c.getInt(c.getColumnIndex("lc_tweet")) == 1);
            setArrivalTime(c.getString(c.getColumnIndex("lc_arrival_time")));
            arrived = (c.getInt(c.getColumnIndex("lc_passed")) == 1);
        } catch (Exception e) {
            errMsg = e.getMessage();
        }
    }

    public static DateFormat getDateFormat() {
        return getDateFormat("HH:mm:ss");
    }

    public static DateFormat getDateFormat(String template) {
        DateFormat df = new SimpleDateFormat(template, Locale.JAPAN);
        df.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        return df;
    }

    public static Pin getSample() {
        return getSample(true);
    }

    private static Pin getSample(boolean isStart) {
        Pin pin = new Pin();
        pin.setSampleValues(isStart);
        return pin;
    }

    public boolean isArrived() {
        return arrived;
    }

    public void arrive(boolean isSkip) {
        if (isArrived()) return;
        arrived = true;
        if (!isSkip) {
            arrivalTime = new Date();
        }
    }

    public boolean isInPlace(double pLatitude, double pLongitude, int pDistance) {
        float[] results = new float[3];
        Location.distanceBetween(pLatitude, pLongitude, latitude, longitude, results);
        return (results[0] < pDistance);
    }

    public double getLatitude() {
        return latitude;
    }

    public String getLatText() {
        return String.format("%1$.5f", getLatitude());
    }

    public double getLongitude() {
        return longitude;
    }

    public String getLonText() {
        return String.format("%1$.5f", getLongitude());
    }

    public String getURL() {
        return String.format("http://maps.google.com/maps?q=%s,%s", getLatText(), getLonText());
    }

    public double getDistance() {
        return distance;
    }

    public String getDistanceText() {
        return getDistanceText(getDistance());
    }

    private static String getDistanceText(double distance) {
        return String.format("%1$.1fkm", distance);
    }

    public String getName() {
        return name;
    }

    public String getTargetText() {
        if (!isCorrect()) return "";
        return String.format("%02d:%02d", hour, minute);
    }

    public String getSavingText() {
        if (arrivalTime == null) return "";
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"), Locale.JAPAN);
        c.setTime(arrivalTime);
        int saving = (hour - c.get(Calendar.HOUR_OF_DAY)) * 60;
        saving += minute - c.get(Calendar.MINUTE);
        return String.format("%+d分", saving);
    }

    public Date getArrivalTime() {
        return arrivalTime;
    }

    private void setArrivalTime(String inputTime) {
        if (inputTime == null || inputTime.isEmpty()) {
            arrivalTime = null;
            return;
        }
        DateFormat df = getDateFormat(DB_DATE_TEMPLATE);
        try {
            this.arrivalTime = df.parse(inputTime);
        } catch (ParseException e) {
            arrivalTime = null;
            e.printStackTrace();
        }
    }

    public String getArrivalText() {
        if (arrivalTime == null) return "";
        DateFormat df = getDateFormat(DB_DATE_TEMPLATE);
        return df.format(arrivalTime);
    }

    public String getTweet(String fmtMessage) {
        String s = fmtMessage;
        s = s.replace("[地名]", getName());
        s = s.replace("[総距離]", getDistanceText());
        s = s.replace("[目標時刻]", getTargetText());
        s = s.replace("[緯度]", getLatText());
        s = s.replace("[経度]", getLonText());
        s = s.replace("[URL]", getURL());
        String sArrive = (arrivalTime == null) ? "取得失敗" : getDateFormat().format(arrivalTime);
        s = s.replace("[到着時刻]", sArrive);
        s = s.replace("[貯金]", getSavingText());
        boolean isCorrect = (nextPin != null && nextPin.isCorrect());
        s = s.replace("[次地名]", isCorrect ? nextPin.getName() : "");
        s = s.replace("[次総距離]", isCorrect ? nextPin.getDistanceText() : "");
        s = s.replace("[次目標時刻]", isCorrect ? nextPin.getTargetText() : "");
        s = s.replace("[次緯度]", isCorrect ? nextPin.getLatText() : "");
        s = s.replace("[次経度]", isCorrect ? nextPin.getLonText() : "");
        s = s.replace("[次URL]", isCorrect ? nextPin.getURL() : "");
        String diff = isCorrect ? getDistanceText(nextPin.getDistance() - getDistance()) : "";
        s = s.replace("[次区間距離]", diff);
        return s;
    }

    public Pin getNextPin() {
        if (nextPin == null) return null;
        nextPin.addTargetTime(hour, minute);
        nextPin.addDistance(distance);
        return nextPin;
    }

    public void setNextPin(Pin nextPin) {
        this.nextPin = nextPin;
    }

    private void addDistance(double distance) {
        if (absDistance) return;
        this.distance += distance;
        absDistance = true;
    }

    public float getDistance(double pLatitude, double pLongitude) {
        if (!isCorrect()) return Float.NaN;
        float[] results = new float[3];
        Location.distanceBetween(pLatitude, pLongitude, latitude, longitude, results);
        return results[0] / 1000f;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
        if (!parseTime(comment)) return;
        if (!parseDistance(comment)) return;
        tweet = !QUIET_PATTERN.matcher(comment).find();
    }

    public boolean isTweet() {
        return tweet;
    }

    public boolean isCorrect() {
        if (!getErrMsg().isEmpty()) return false;
        //必須入力チェック
        if (getName().isEmpty()) return false;
        if (Double.isNaN(distance)) return false;
        //正常データ
        return true;
    }

    public String getErrMsg() {
        return errMsg;
    }

    private boolean parseTime(String comment) {
        Matcher m = TIME_PATTERN.matcher(comment);
        if (!m.find()) {
            return false;
        }
        try {
            String[] ss = m.group(2).split(":");
            if (ss.length > 1) {
                hour = Integer.parseInt(ss[0]);
                minute = Integer.parseInt(ss[1]);
            } else {
                minute = Integer.parseInt(ss[0]);
            }
            calcTargetTime();
            absTime = m.group(3).equals("!");
            return true;
        } catch (Exception e) {
            errMsg = e.getMessage();
            return false;
        }
    }

    private boolean parseDistance(String comment) {
        Matcher m = DISTANCE_PATTERN.matcher(comment);
        if (!m.find()) {
            return false;
        }
        try {
            distance = Double.parseDouble(m.group(2));
            absDistance = m.group(3).equals("!");
            return true;
        } catch (Exception e) {
            errMsg = e.getMessage();
            return false;
        }
    }

    public boolean isAbsDistance() {
        return absDistance;
    }

    public boolean isAbsTime() {
        return absTime;
    }

    public void addTargetTime(int hourOfDay, int minute) {
        if (absTime) return;
        this.hour += hourOfDay;
        this.minute += minute;
        calcTargetTime();
        absTime = true;
    }

    private void calcTargetTime() {
        while (this.minute >= 60) {
            this.hour++;
            this.minute -= 60;
        }
        this.hour %= 24;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTargetTime(String targetTime) {
        String[] ss = targetTime.split(":");
        if (ss.length == 1) {
            hour = 0;
            minute = Integer.parseInt(ss[0]);
        } else {
            hour = Integer.parseInt(ss[0]);
            minute = Integer.parseInt(ss[1]);
        }
        calcTargetTime();
        absTime = true;
    }

    private void setSampleValues(boolean isStart) {
        if (isStart) {
            id = 1;
            arrived = true;
            arrivalTime = new Date();
            latitude = 35.68439;
            longitude = 139.77453;
            distance = 0;
            absDistance = true;
            name = "日本橋";
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"), Locale.JAPAN);
            c.setTime(arrivalTime);
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
            nextPin = getSample(false);
        } else {
            id = 2;
            latitude = 34.69838;
            longitude = 135.50033;
            distance = 530.1;
            name = "梅田新道";
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"), Locale.JAPAN);
            c.setTime(new Date());
            c.add(Calendar.MINUTE, -30);
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
        }
        absTime = true;
        tweet = false;
        comment = "";
        errMsg = "";
    }
}
