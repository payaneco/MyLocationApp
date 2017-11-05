package jp.payaneco.cyclelocationapp;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.Date;

/**
 * Created by payaneco on 2017/10/15.
 */

public class MyLocationService extends Service {
    private static LocationManager locationManager;
    public static MyLocationListener myLocationListener;

    // Serviceに接続するためのBinderクラスを実装する
    public class LocalBinder extends Binder {
        //Serviceの取得
        MyLocationService getService() {
            return MyLocationService.this;
        }
    }

    // Binderの生成
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        long interval = MainActivity.getInterval();
        float distance = MainActivity.getDistance() / 3f;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return mBinder;
        }

        if(myLocationListener == null) {
            myLocationListener = new MyLocationListener();
            setLastKnownLocation();
        }
        MainActivity.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, distance, myLocationListener);
        return mBinder;
    }

    public void setLastKnownLocation() {
        //参照元 http://d.hatena.ne.jp/orangesignal/20101223/1293079002
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        final Criteria criteria = new Criteria();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true));
        //10分以内に位置情報が取得されていれば有効なデータとして扱う
        long diff = new Date().getTime() - location.getTime();
        if (location != null && (diff) <= (10 * 60 * 1000L)) {
            myLocationListener.setLocation(location);
        }
        myLocationListener.checkArrived();
    }

    @Override
    public void onRebind(Intent intent){
        // Unbind後に再接続する場合に呼ばれる
        Log.i("", "onRebind" + ": " + intent);
    }

    @Override
    public boolean onUnbind(Intent intent){
        // Service切断時に呼び出される
        //onUnbindをreturn trueでoverrideすると次回バインド時にonRebildが呼ばれる
        return true;
    }
}
