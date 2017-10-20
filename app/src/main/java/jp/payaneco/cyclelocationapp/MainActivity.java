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
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    public static LocationManager locationManager;

    private ServiceConnection mConnection;
    private MyLocationService locationService;
    private SharedPreferences sharedPreferences;
    private boolean mBound = false;
    private Pin currentPin;

    /*
    //ツイートしようとして失敗した残骸その1
    private final String API_KEY = "XXX";
    private final String API_SECRET = "XXX";

    private String accessKey;
    private String accessSecret;

    private AsyncTwitter mTwitter;
    private RequestToken mReqToken;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

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

        /*
        //Twitter4jでツイートしようとして失敗した残骸その2
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        getAccessToken();
        if(accessKey != null) {
            mTwitter = new AsyncTwitterFactory().getInstance();
            mTwitter.addListener(mListener);
            mTwitter.setOAuthConsumer(API_KEY, API_SECRET);
            mTwitter.setOAuthAccessToken(new AccessToken(accessKey, accessSecret));
            mTwitter.updateStatus("ねむねむ");
        }
        */

        //アプリケーションにバインドされたサービス開始
        Intent i = new Intent(MainActivity.this, MyLocationService.class);
        stopService(i);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                showLocationData();
            }
        });
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
        updateView.setText(df.format(now) + "GPS稼働");
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
        currentView.setText(df.format(pin.getArrivalTime()) + "到着");
        //目標
        TextView targetView = (TextView) findViewById(R.id.targetView);
        targetView.setText(pin.getTargetText() + "目標");
        //貯金
        TextView savingView = (TextView) findViewById(R.id.savingView);
        savingView.setText(pin.getSavingText());
        //ツイート
        String url = String.format("https://twitter.com/share?text=%s", pin.getTweet());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        currentPin = pin;
    }

    /*
    //ツイートしようとして失敗した残骸その3
    private void getAccessToken() {
        //アクセストークンが永続記憶域にあれば読み出す
        accessKey = sharedPreferences.getString("ACCESS_KEY", "none");
        accessSecret = sharedPreferences.getString("ACCESS_SECRET", "none");

        if(!accessKey.equals("none")) return;

        mTwitter = new AsyncTwitterFactory().getInstance();
        mTwitter.addListener(mListener);
        mTwitter.setOAuthConsumer(API_KEY, API_SECRET);
        mTwitter.getOAuthRequestTokenAsync("twittercallback://callback");
    }

    private final TwitterListener mListener = new TwitterAdapter() {
        @Override
        public void updatedStatus(Status status) {
            Log.d("updatedStatus", "Status ID:" + status.getId());
        }
        @Override
        public void gotOAuthRequestToken(RequestToken token) {
            mReqToken = token;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mReqToken.getAuthorizationURL()));
            startActivity(intent);
        }

        @Override
        public void gotOAuthAccessToken(AccessToken token) {
            //token.getToken()とtoken.getTokenSecret()を保存する
        }

    };

    @Override
    protected void onNewIntent(Intent intent) {
        //ブラウザからのコールバックで呼ばれる
        final Uri uri = intent.getData();
        final String verifier = uri.getQueryParameter("oauth_verifier");
        if (verifier != null) {
            mTwitter.getOAuthAccessTokenAsync(mReqToken, verifier);
            accessKey = mReqToken.getToken();
            accessSecret = mReqToken.getTokenSecret();
            //アクセストークンを永続記憶域に保存
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("ACCESS_KEY", accessKey);
            editor.putString("ACCESS_SECRET", accessSecret);
            editor.commit();
        }
    }
    */

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
