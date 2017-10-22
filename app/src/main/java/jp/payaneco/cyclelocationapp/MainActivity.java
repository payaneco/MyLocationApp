package jp.payaneco.cyclelocationapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    public static LocationManager locationManager;
    private static int interval;
    private static int distance;

    private ServiceConnection mConnection;
    private MyLocationService locationService;
    private SharedPreferences sharedPreferences;
    private boolean mBound = false;
    private Pin currentPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        interval = sharedPreferences.getInt("INTERVAL", 60);
        distance = sharedPreferences.getInt("DISTANCE", 500);

        final ServiceConnection mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                MyLocationService.LocalBinder binder = (MyLocationService.LocalBinder) service;
                locationService = binder.getService();
                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
        bindService(mConnection);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLocationData();
            }
        });
    }

    private void bindService(ServiceConnection mConnection) {
        //アプリケーションにバインドされたサービス開始
        Intent i = new Intent(MainActivity.this, MyLocationService.class);
        stopService(i);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    //アクティビティへの文字列表示処理
    private void showLocationData() {
        MyLocationListener myLocationListener = MyLocationService.myLocationListener;
        Date now = myLocationListener.getUpdate();
        if(now == null) return;
        DateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.JAPAN);
        df.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        //更新日時
        TextView updateView = (TextView) findViewById(R.id.updateView);
        updateView.setText(String.format("%sGPS稼働", df.format(now)));
        if(myLocationListener.getCurrentPin() == null) return;
        if(myLocationListener.getCurrentPin().equals(currentPin)) return;
        Pin pin = myLocationListener.getCurrentPin();
        //地名
        TextView nameView = (TextView) findViewById(R.id.nameView);
        nameView.setText(pin.getName());
        //総距離
        TextView distanceView = (TextView) findViewById(R.id.distanceView);
        distanceView.setText(String.format("%1$.1fkm走破", pin.getDistance()));
        //時点
        TextView currentView = (TextView) findViewById(R.id.currentView);
        currentView.setText(String.format("%s到着", df.format(pin.getArrivalTime())));
        //目標
        TextView targetView = (TextView) findViewById(R.id.targetView);
        targetView.setText(String.format("%s目標", pin.getTargetText()));
        //貯金
        TextView savingView = (TextView) findViewById(R.id.savingView);
        savingView.setText(pin.getSavingText());
        //ツイート
        String url = String.format("https://twitter.com/share?text=%s", pin.getTweet());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        currentPin = pin;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            showPopupSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPopupSettings() {
        PopupWindow pop = new PopupWindow(this);
        //レイアウト設定
        final View popupView = getLayoutInflater().inflate(R.layout.popup_settings, null);
        final EditText editInterval = (EditText)popupView.findViewById(R.id.editInterval);
        editInterval.setText(String.valueOf(interval));
        final EditText editDistance = (EditText)popupView.findViewById(R.id.editDistance);
        editDistance.setText(String.valueOf(distance));
        pop.setContentView(popupView);
        //タップ時に他のViewでキャッチされないための設定
        pop.setOutsideTouchable(true);
        pop.setFocusable(true);
        //画面閉じたら設定保存
        pop.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                int interval = getSettingsValue(popupView.findViewById(R.id.editInterval));
                setInterval(interval);
                int distance = getSettingsValue(popupView.findViewById(R.id.editDistance));
                setDistance(distance);
                //bindService(mConnection); //エラーになるのでいったん頑張らない
                Snackbar.make(findViewById(R.id.nameView), "設定変更後はアプリを再起動してください", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
        pop.showAtLocation(findViewById(R.id.nameView), Gravity.CENTER, 0, 0);
    }

    //EditTextから数値を取得する。無効な値の場合はMIN_VALUEを返す
    private int getSettingsValue(View view) {
        EditText editText = (EditText)view;
        String value = editText.getText().toString();
        if(!value.matches("\\d+")) {
            return Integer.MIN_VALUE;
        }
        return Integer.parseInt(value);
    }

    public static int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        if(interval <= 0) return;
        MainActivity.interval = interval;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("INTERVAL", interval);
        editor.apply();
    }

    public static int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        if(distance <= 0) return;
        MainActivity.distance = distance;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("DISTANCE", distance);
        editor.apply();
    }
}
