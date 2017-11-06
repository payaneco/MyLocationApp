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
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
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
import android.widget.Button;
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
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_PICK_GPX = 12345;
    private final String DEFAULT_FMT_MESSAGE = "[到着時刻]に[総距離]地点の[地名]周辺に到着しました。\r\n" +
            "貯金は約[貯金]分！\r\n" +
            "次の目標は[次区間距離]先の[次地名]に[次目標時刻]です。#♨\r\n" +
            "[URL]";

    public static LocationManager locationManager;
    private static int interval;
    private static int distance;
    private static int se;
    private static int vibe;
    private static SoundPool soundPool;
    private static int soundId;
    private static boolean isSoundLoaded;
    private static Vibrator vibrator;

    private ServiceConnection mConnection;
    private MyLocationService locationService;
    private SharedPreferences sharedPreferences;
    private Pin currentPin;
    private boolean mBound = false;
    private String fmtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initProperties();
        loadSoundPool();
        checkGPS();
        DBHelper dbHelper = new DBHelper(this);
        MyLocationListener.setDbHelper(dbHelper);
        showInitView(loadPinList(dbHelper));
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        final ServiceConnection mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                MyLocationService.LocalBinder binder = (MyLocationService.LocalBinder) service;
                locationService = binder.getService();
                mBound = true;
                showLocationData(true);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
                unbindService(this);
            }
        };
        bindService(mConnection);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLocationData(false);
            }
        });
    }

    private void loadSoundPool() {
        if (soundPool == null) {
            initSoundPool();
        } else if (isSoundLoaded) {
            soundPool.unload(soundId);
        }
        isSoundLoaded = false;
        switch (se) {
            case 1:
                soundId = soundPool.load(this, R.raw.sysse, 1);
                break;
            case 2:
                soundId = soundPool.load(this, R.raw.pan, 1);
                break;
            case 3:
                soundId = soundPool.load(this, R.raw.mes, 1);
                break;
        }
    }

    private void initSoundPool() {
        AudioAttributes attr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        int seCount = getResources().obtainTypedArray(R.array.se_array).length() - 1;
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attr)
                .setMaxStreams(seCount)
                .build();
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                isSoundLoaded = (status == 0);
            }
        });
    }

    private void showInitView(int count) {
        boolean isEmpty = (count == 0);
        TextView nameView = (TextView) findViewById(R.id.nameView);
        nameView.setText(isEmpty ? "メニューからGPX選択" : "GPSの動く頃に");
        TextView distanceView = (TextView) findViewById(R.id.distanceView);
        distanceView.setText(isEmpty ? "" : "右下のボタンを押下");
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
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        interval = sharedPreferences.getInt("INTERVAL", 30);
        distance = sharedPreferences.getInt("DISTANCE", 300);
        se = sharedPreferences.getInt("SE", 1);
        vibe = sharedPreferences.getInt("VIBE", 3000);
        fmtMessage = sharedPreferences.getString("MESSAGE", DEFAULT_FMT_MESSAGE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    private void bindService(ServiceConnection mConnection) {
        //アプリケーションにバインドされたサービス開始
        Intent i = new Intent(MainActivity.this, MyLocationService.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        MyLocationListener myLocationListener = MyLocationService.myLocationListener;
    }

    //アクティビティへの文字列表示処理
    private void showLocationData(boolean isInitializing) {
        MyLocationListener myLocationListener = MyLocationService.myLocationListener;
        if (myLocationListener == null) return;
        Date now = myLocationListener.getUpdate();
        if(now == null) return;
        DateFormat df = Pin.getDateFormat();
        //更新日時
        TextView updateView = (TextView) findViewById(R.id.updateView);
        updateView.setText(myLocationListener.getUpdateText());
        Pin pin = myLocationListener.getCurrentPin();
        //次
        showNextPinData(myLocationListener.getNextPin(), myLocationListener.getCurrentLatitude(), myLocationListener.getCurrentLongitude());
        if (pin == null) {
            showStartView();
            return;
        }
        if (pin.equals(currentPin)) return;
        currentPin = pin;
        showPinData(pin);
        if (isInitializing || !pin.isTweet()) return;
        //ツイート
        tweet(pin, fmtMessage);
    }

    private void tweet(Pin pin, String fmt) {
        String tweet = pin.getTweet(fmt);
        if (tweet.isEmpty()) return;
        String enc = null;
        try {
            enc = URLEncoder.encode(tweet, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            enc = String.format("エンコードに失敗しました。%s", e.getMessage());
            e.printStackTrace();
        }
        String url = String.format("https://twitter.com/share?text=%s", enc);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void showStartView() {
        TextView nameView = (TextView) findViewById(R.id.nameView);
        nameView.setText("お気をつけて");
        TextView distanceView = (TextView) findViewById(R.id.distanceView);
        distanceView.setText("いってらっしゃい！");
        TextView currentView = (TextView) findViewById(R.id.currentView);
        currentView.setText("");
        TextView targetView = (TextView) findViewById(R.id.targetView);
        targetView.setText("");
        TextView savingView = (TextView) findViewById(R.id.savingView);
        savingView.setText("");
    }

    private void showPinData(Pin pin) {
        DateFormat df = Pin.getDateFormat();
        //地名
        TextView nameView = (TextView) findViewById(R.id.nameView);
        nameView.setText(pin.getName());
        //総距離
        TextView distanceView = (TextView) findViewById(R.id.distanceView);
        distanceView.setText(pin.getDistanceText());
        //時点
        TextView currentView = (TextView) findViewById(R.id.currentView);
        if (pin.getArrivalTime() != null) {
            currentView.setText(String.format("%s到着", df.format(pin.getArrivalTime())));
        } else {
            currentView.setText("到着時刻取得失敗");
        }
        //目標
        TextView targetView = (TextView) findViewById(R.id.targetView);
        targetView.setText(String.format("%s目標", pin.getTargetText()));
        //貯金
        TextView savingView = (TextView) findViewById(R.id.savingView);
        savingView.setText(String.format("貯金%s", pin.getSavingText()));
    }

    private void showNextPinData(Pin pin, double currentLatitude, double currentLongitude) {
        if (pin == null) {
            pin = new Pin();    //ダミーのピンを設定
        }
        boolean isGoal = (currentPin != null);
        showNextDistance(pin, currentLatitude, currentLongitude);
        TextView nextNameView = (TextView) findViewById(R.id.nextNameView);
        if (pin.getName().isEmpty()) {
            nextNameView.setText(isGoal ? "おつかれさまでした！" : "");
        } else {
            nextNameView.setText(String.format("Next: %s", pin.getName()));
        }
        TextView nextTargetView = (TextView) findViewById(R.id.nextTargetView);
        String nextTargetText = pin.getTargetText();
        if (nextTargetText.isEmpty()) {
            nextTargetView.setText("");
        } else {
            nextTargetView.setText(String.format("%s目標", nextTargetText));
        }
    }

    private void showNextDistance(Pin pin, double lat, double lon) {
        TextView nextDistanceView = (TextView) findViewById(R.id.nextDistanceView);
        float nextDistance = pin.getDistance(lat, lon);
        if (Float.isNaN(nextDistance)) {
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
            return true;
        } else if (id == R.id.action_message) {
            showPopupFmtMessage();
            return true;
        } else if (id == R.id.action_skip) {
            MyLocationListener myLocationListener = MyLocationService.myLocationListener;
            Pin pin = myLocationListener.getNextPin();
            myLocationListener.setArrived(pin);
            showLocationData(true);
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPopupFmtMessage() {
        PopupWindow pop = new PopupWindow(this);
        //レイアウト設定
        final View popupView = getLayoutInflater().inflate(R.layout.popup_message, null);
        final EditText editMsgFormat = (EditText) popupView.findViewById(R.id.editMsgFormat);
        editMsgFormat.setText(fmtMessage);
        final Button btnTest = (Button) popupView.findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Pin pin = currentPin;
                if (currentPin == null || !currentPin.isCorrect()) {
                    pin = Pin.getSample();
                    tweet(pin, editMsgFormat.getText().toString());
                } else {
                    tweet(pin, editMsgFormat.getText().toString());
                }
            }
        });
        final Button btnRevert = (Button) popupView.findViewById(R.id.btnRevert);
        btnRevert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editMsgFormat.setText(fmtMessage);
            }
        });
        pop.setContentView(popupView);
        //タップ時に他のViewでキャッチされないための設定
        pop.setOutsideTouchable(true);
        pop.setFocusable(true);
        //画面閉じたら設定保存
        pop.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setFmtMessage((EditText) popupView.findViewById(R.id.editMsgFormat));
            }
        });
        pop.showAtLocation(findViewById(R.id.nameView), Gravity.CENTER, 0, 0);
    }

    private void showPopupSettings() {
        PopupWindow pop = new PopupWindow(this);
        //レイアウト設定
        final View popupView = getLayoutInflater().inflate(R.layout.popup_settings, null);
        final EditText editInterval = (EditText)popupView.findViewById(R.id.editInterval);
        editInterval.setText(String.valueOf(interval));
        final EditText editDistance = (EditText)popupView.findViewById(R.id.editDistance);
        editDistance.setText(String.valueOf(distance));
        //final Spinner spinnerSe = (Spinner) popupView.findViewById(R.id.spinnerSe);
        //spinnerSe.setSelection(se);
        //実機だとスピナーが動かなかったので仕方なく
        final EditText editSe = (EditText) popupView.findViewById(R.id.editSe);
        editSe.setText(String.valueOf(se));
        final EditText editVibe = (EditText) popupView.findViewById(R.id.editVibe);
        editVibe.setText(String.valueOf(vibe));
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
                //Spinner spinnerSe = (Spinner) popupView.findViewById(R.id.spinnerSe);
                //setSe(spinnerSe.getSelectedItemPosition());
                int se = getSettingsValue(popupView.findViewById(R.id.editSe));
                setSe(se);
                int vibe = getSettingsValue(popupView.findViewById(R.id.editVibe));
                setVibe(vibe);
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
        showInitView(loadPinList(dbHelper));
        locationService.setLastKnownLocation();
        showLocationData(true);
    }

    private int loadPinList(DBHelper dbHelper) {
        currentPin = null;
        LinkedList<Pin> list = dbHelper.selectAll();
        MyLocationListener.setPinList(list);
        return list.size();
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

    private void setInterval(int interval) {
        if(interval <= 0) return;
        MainActivity.interval = interval;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("INTERVAL", interval);
        editor.apply();
    }

    public static int getDistance() {
        return distance;
    }

    private void setDistance(int distance) {
        if(distance <= 0) return;
        MainActivity.distance = distance;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("DISTANCE", distance);
        editor.apply();
    }

    public static void playSe() {
        if (!isSoundLoaded) return;
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
    }

    private void setSe(int se) {
        if (se < 0 || 3 < se) return;
        MainActivity.se = se;
        loadSoundPool();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("SE", se);
        editor.apply();
    }

    public void setVibe(int vibe) {
        if (vibe < 0 || 10000 < vibe) return;
        MainActivity.vibe = vibe;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("VIBE", vibe);
        editor.apply();
    }

    public String getFmtMessage() {
        return fmtMessage;
    }

    private void setFmtMessage(EditText editText) {
        String value = editText.getText().toString();
        fmtMessage = value;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("MESSAGE", fmtMessage);
        editor.apply();
    }

    public static void vibrate() {
        if (vibe < 0) return;
        if (vibrator == null) return;
        vibrator.vibrate(vibe);
    }
}
