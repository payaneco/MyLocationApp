package jp.payaneco.cyclelocationapp;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

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
        long span = 1000 * 60 * 3; //3分ごとにチェック
        //span = 1000 * 5; //[テスト]5秒ごとにチェック
        int distance = 10;   //最小距離間隔[m]
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return mBinder;
        }

        if(myLocationListener == null) {
            myLocationListener = new MyLocationListener();
        }
        MainActivity.locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, span, distance, myLocationListener);
        return mBinder;
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
