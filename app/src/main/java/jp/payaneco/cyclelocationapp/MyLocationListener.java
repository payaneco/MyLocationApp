package jp.payaneco.cyclelocationapp;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by payaneco on 2017/10/15.
 */

public class MyLocationListener implements LocationListener {
    private Date update;            //GPSの最終更新日時
    private double currentLatitude;
    private double currentLongitude;
    private static ArrayList<Pin> pinList;
    private static Pin currentPin;         //最後に表示したピン
    private static DBHelper dbHelper;

    public MyLocationListener() {
        setPinList(new ArrayList<Pin>());
    }

    @Override
    public void onLocationChanged(Location location) {
        int distance = MainActivity.getDistance();
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        update = new Date();
        if (currentPin == null) return;
        if (currentPin.isInPlace(location.getLatitude(), location.getLongitude(), distance)) {
            setArrived();
        }
    }

    private void setArrived() {
        currentPin.arrive();
        if (dbHelper == null) return;
        dbHelper.beginTransaction();
        dbHelper.setArrived(currentPin);
        dbHelper.commit();
        setCurrentPin(currentPin.getNextPin());
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

    public double getCurrentLatitude() {
        return currentLatitude;
    }

    public double getCurrentLongitude() {
        return currentLongitude;
    }

    public ArrayList<Pin> getPinList() {
        return pinList;
    }

    public static void setPinList(ArrayList<Pin> list) {
        pinList = list;
        //currentPinを設定
        setCurrentPin(null);
        for (Pin pin : pinList) {
            //最初に見つかった未着のピンを選択
            if (!pin.isArrived()) {
                setCurrentPin(pin);
                break;
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
