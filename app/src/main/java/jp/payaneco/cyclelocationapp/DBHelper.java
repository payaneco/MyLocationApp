package jp.payaneco.cyclelocationapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Created by 884 on 2017/10/28.
 */

public class DBHelper extends SQLiteOpenHelper {
    static final String DB_NAME = "cycle_location_app.db";
    static final int DB_VERSION = 1;
    static final String CREATE_LOCATION = "create table location ( " +
            "lc_id INTEGER PRIMARY KEY AUTOINCREMENT, lc_lat REAL, lc_lon REAL, " +
            "lc_name TEXT, lc_cmt TEXT, lc_target_time TEXT, lc_distance INTEGER, " +
            "lc_tweet TEXT, lc_arrival_time TEXT, lc_passed TEXT);";
    static final String DROP_LOCATION = "drop table location;";
    static final String TRUNCATE_LOCATION = "delete from location;";
    static final String INIT_LOCATION_INDEX = "delete from sqlite_sequence where name='location';";
    static final String INSERT_LOCATION = "insert into location values(null, ?, ?," +
            " ?, ?, ?, ?, ?, null, null);";
    static final String UPDATE_ARRIVED = "update location set " +
            "lc_arrival_time = ?, lc_passed = ? " +
            "where lc_id = ?";

    private SQLiteDatabase db;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public void Clear() {
        getDb().execSQL(TRUNCATE_LOCATION);
        getDb().execSQL(INIT_LOCATION_INDEX);
    }

    public void insert(Pin pin) {
        getDb().execSQL(INSERT_LOCATION, new Object[]{pin.getLatitude(), pin.getLongitude(), pin.getName(),
                pin.getComment(), pin.getTargetText(), pin.getDistance(), pin.isTweet()});
    }

    public void setArrived(Pin pin) {
        getDb().execSQL(UPDATE_ARRIVED, new Object[]{pin.getArrivalTime(), pin.isArrived(), pin.getId()});
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_LOCATION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_LOCATION);
        db.execSQL(CREATE_LOCATION);
    }

    public void beginTransaction() {
        getDb().beginTransaction();
    }

    public void commit() {
        getDb().setTransactionSuccessful();
        getDb().endTransaction();
    }

    protected SQLiteDatabase getDb() {
        if (db == null) {
            db = getWritableDatabase();
        }
        return db;
    }

    public ArrayList<Pin> selectAll() {
        String[] cols = {"lc_id", "lc_lat, lc_lon, lc_name, lc_cmt, lc_target_time, lc_distance, lc_tweet, lc_arrival_time, lc_passed"};
        Cursor c = getDb().query("location", cols, null, null, null, null, null);
        ArrayList<Pin> list = new ArrayList<>();
        while (c.moveToNext()) {
            Pin pin = new Pin(c);
            if (pin.isCorrect()) {
                list.add(pin);
            }
        }
        return list;
    }
}
