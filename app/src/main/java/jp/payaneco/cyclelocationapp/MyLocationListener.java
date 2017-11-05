package jp.payaneco.cyclelocationapp;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;

/**
 * Created by payaneco on 2017/10/15.
 */

public class MyLocationListener implements LocationListener {
    private Date update;            //GPSの最終更新日時
    private double currentLatitude;
    private double currentLongitude;
    private static LinkedList<Pin> pinList;
    private static Pin currentPin;         //最後に表示したピン
    private static DBHelper dbHelper;

    @Override
    public void onLocationChanged(Location location) {
        setLocation(location);
        checkArrived();
    }

    public void setLocation(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        update = new Date();
    }

    public void checkArrived() {
        int distance = MainActivity.getDistance();
        Pin pin = getNextPin();
        if (pin == null) return;
        if (pin.isInPlace(currentLatitude, currentLongitude, distance)) {
            setArrived(pin, false);
            MainActivity.playSe();
        }
    }

    public void setArrived(Pin pin) {
        setArrived(pin, true);
    }

    private void setArrived(Pin pin, boolean isSkip) {
        if (pin == null) return;
        pin.arrive(isSkip);
        if (dbHelper == null) return;
        dbHelper.beginTransaction();
        dbHelper.setArrived(pin);
        dbHelper.commit();
        setCurrentPin(pin);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public Date getUpdate() {
        return update;
    }

    public Pin getCurrentPin() {
        return currentPin;
    }

    public Pin getNextPin() {
        if (currentPin == null) return getPinList().peekFirst();
        return currentPin.getNextPin();
    }

    public double getCurrentLatitude() {
        return currentLatitude;
    }

    public double getCurrentLongitude() {
        return currentLongitude;
    }

    public LinkedList<Pin> getPinList() {
        if (pinList == null) {
            pinList = new LinkedList<>();
        }
        return pinList;
    }

    public static void setPinList(LinkedList<Pin> list) {
        pinList = list;
        //currentPinを設定
        setCurrentPin(null);
        for (Pin pin : pinList) {
            //最後に見つかった既着のピンを選択
            if (pin.isArrived()) {
                setCurrentPin(pin);
            }
        }
    }

    public static void setCurrentPin(Pin pin) {
        currentPin = pin;
    }

    public String getUpdteText() {
        DateFormat df = Pin.getDateFormat();
        if (pinList == null) {
            return "";
        }
        return String.format("%s updated. %d/%d passed.", df.format(getUpdate()),
                getPassedCount(), pinList.size());
    }

    private int getPassedCount() {
        int c = 0;
        for (Pin pin : pinList) {
            if (pin.isArrived()) {
                c++;
            }
        }
        return c;
    }

    public static void setDbHelper(DBHelper helper) {
        dbHelper = helper;
    }
}
