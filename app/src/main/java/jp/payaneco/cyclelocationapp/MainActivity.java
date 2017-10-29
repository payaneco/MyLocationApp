package jp.payaneco.cyclelocationapp;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Xml;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_PICK_GPX = 1;

    public static LocationManager locationManager;
    private static int interval;
    private static int distance;

    private ServiceConnection mConnection;
    private MyLocationService locationService;
    private SharedPreferences sharedPreferences;
    private Pin currentPin;
    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initProperties();
        checkGPS();
        DBHelper dbHelper = new DBHelper(this);
        MyLocationListener.setDbHelper(dbHelper);
        loadPinList(dbHelper);

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

    private void checkGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(findViewById(R.id.nameView), "GPSを許可してアプリを再起動してください", Snackbar.LENGTH_LONG)
                    .setAction("設定", new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_SETTINGS);
                            startActivity(intent);
                        }
                    }).show();
        }
    }

    private void initProperties() {
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        interval = sharedPreferences.getInt("INTERVAL", 60);
        distance = sharedPreferences.getInt("DISTANCE", 500);
    }

    private void bindService(ServiceConnection mConnection) {
        //アプリケーションにバインドされたサービス開始
        Intent i = new Intent(MainActivity.this, MyLocationService.class);
        stopService(i);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        MyLocationListener myLocationListener = MyLocationService.myLocationListener;
    }

    //アクティビティへの文字列表示処理
    private void showLocationData() {
        MyLocationListener myLocationListener = MyLocationService.myLocationListener;
        if (myLocationListener == null) return;
        Date now = myLocationListener.getUpdate();
        if(now == null) return;
        DateFormat df = Pin.getDateFormat();
        //更新日時
        TextView updateView = (TextView) findViewById(R.id.updateView);
        updateView.setText(myLocationListener.getUpdteText());
        Pin pin = myLocationListener.getCurrentPin();
        if (pin == null) return;
        //次
        showNextDistance(pin, myLocationListener.getCurrentLatitude(), myLocationListener.getCurrentLongitude());
        if (pin.equals(currentPin)) return;
        showPinData(pin);

        //ツイート
        String url = String.format("https://twitter.com/share?text=%s", pin.getTweet());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        currentPin = pin;
    }

    private void showPinData(Pin pin) {
        DateFormat df = Pin.getDateFormat();
        //動的に変わらないもの
        TextView nextNameView = (TextView) findViewById(R.id.nextNameView);
        if (pin.getNextNameText().isEmpty()) {
            nextNameView.setText("");
        } else {
            nextNameView.setText(String.format("Next: %s", pin.getNextNameText()));
        }
        TextView nextTargetView = (TextView) findViewById(R.id.nextTargetView);
        String nextTargetText = pin.getNextTargetText();
        if (nextTargetText.isEmpty()) {
            nextTargetView.setText("");
        } else {
            nextTargetView.setText(String.format("%s目標", nextTargetText));
        }
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
    }

    private void showNextDistance(Pin pin, double lat, double lon) {
        TextView nextDistanceView = (TextView) findViewById(R.id.nextTargetView);
        float nextDistance = pin.getNextDistance(lat, lon);
        if (nextDistance < 0) {
            nextDistanceView.setText("");
        } else {
            nextDistanceView.setText(String.format("直線距離で%1$.1fkm", nextDistance));
        }
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
        } else if (id == R.id.action_gpx) {
            loadGpx();
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
                Snackbar.make(findViewById(R.id.nameView), "設定変更後はアプリを再起動してください", Snackbar.LENGTH_LONG).show();
            }
        });
        pop.showAtLocation(findViewById(R.id.nameView), Gravity.CENTER, 0, 0);
    }

    private void loadGpx() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_PICK_GPX);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == REQUEST_PICK_GPX && resultCode == RESULT_OK) {
                Uri uri = data.getData();
                LinkedList<Pin> list = parseXml(uri);
                if (list.isEmpty()) return;
                //最初の地点が相対時間の場合、スタート時間を設定する
                final Pin pin = list.pollLast();
                if (pin.isAbsTime()) {
                    setDB(pin);
                } else {
                    Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"), Locale.JAPAN);
                    c.setTime(new Date());
                    TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            pin.addTargetTime(hourOfDay, minute);
                            setDB(pin);
                        }
                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
                    timePickerDialog.show();
                }
            }
        } catch (UnsupportedEncodingException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void setDB(Pin pin) {
        DBHelper dbHelper = new DBHelper(this);
        dbHelper.beginTransaction();
        dbHelper.Clear();
        insertPins(pin, dbHelper);
        dbHelper.commit();
        loadPinList(dbHelper);
    }

    private void loadPinList(DBHelper dbHelper) {
        MyLocationListener myLocationListener = MyLocationService.myLocationListener;
        MyLocationListener.setPinList(dbHelper.selectAll());
        showLocationData();
    }

    private void insertPins(Pin pin, DBHelper dbHelper) {
        if (pin == null) return;
        dbHelper.insert(pin);
        insertPins(pin.getNextPin(), dbHelper);
    }

    private LinkedList<Pin> parseXml(Uri uri) throws IOException, XmlPullParserException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        String text = convertInputStreamToString(inputStream);
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(text));
        int eventType = parser.getEventType();
        String s = "";
        LinkedList<Pin> list = new LinkedList<>();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (parser.getName().equalsIgnoreCase("wpt")) {
                        Pin pin = new Pin(parser);
                        if (pin.isCorrect()) {
                            pin.setNextPin(list.peekLast());
                            list.add(pin);
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }
        return list;
    }

    private static String convertInputStreamToString(InputStream is) throws IOException {
        InputStreamReader reader = new InputStreamReader(is);
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[512];
        int read;
        while (0 <= (read = reader.read(buffer))) {
            builder.append(buffer, 0, read);
        }
        return rmBom(builder.toString());
    }

    private static String rmBom(String xmlString) {
        if (Integer.toHexString(xmlString.charAt(0)).equals("feff")) {
            // 先頭一文字を除く
            xmlString = xmlString.substring(1);
        }
        return xmlString;
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
