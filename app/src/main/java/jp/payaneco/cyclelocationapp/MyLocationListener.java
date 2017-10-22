package jp.payaneco.cyclelocationapp;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by payaneco on 2017/10/15.
 */

public class MyLocationListener implements LocationListener {
    private Date update;            //GPSの最終更新日時
    private ArrayList<Pin> pinList;
    private Pin currentPin;         //最後に表示したピン
    //private Twitter mTwitter;

    public MyLocationListener() {
        pinList = new ArrayList<Pin>();
        //テスト
        pinList.add(new Pin(36.3262, 139.1982, 15, "伊勢崎駅", 11, 45));
        pinList.add(new Pin(36.2945, 139.1968, 20, "伊勢崎市民の森公園", 12, 0));
        pinList.add(new Pin(36.2633, 139.1926, 25, "坂東大橋", 12, 10));
        pinList.add(new Pin(36.2360, 139.1888, 30, "本庄駅", 12, 30));

        //本番
        pinList.add(new Pin(35.6824, 139.7738, 0, "日本橋", 23, 59));
        pinList.add(new Pin(35.5971, 139.7376, 11.3, "大井競馬場", 0, 29));
        pinList.add(new Pin(35.5295, 139.7047, 19.5, "川崎市役所", 0, 49));
        pinList.add(new Pin(35.4655, 139.6242, 30, "横浜駅", 1, 15));
        pinList.add(new Pin(35.4009, 139.5363, 42.5, "戸塚駅", 1, 47));
        pinList.add(new Pin(35.3506, 139.4786, 51, "藤沢本町駅", 2, 10));
        pinList.add(new Pin(35.3327, 139.3502, 63.5, "平塚駅", 2, 40));
        pinList.add(new Pin(35.2978, 139.2587, 73.5, "二宮駅", 3, 4));
        pinList.add(new Pin(35.2495, 139.1595, 84.5, "小田原城", 3, 30));
        pinList.add(new Pin(35.2333, 139.1046, 90, "箱根湯本", 3, 45));
        //1000mずっと上り
        pinList.add(new Pin(35.2174, 139.0385, 103.4, "国道1号線最高地点", 4, 45));
        pinList.add(new Pin(35.1823, 139.0140, 110.4, "箱根峠", 5, 5));
        pinList.add(new Pin(35.1123, 138.9228, 126, "三島駅", 5, 35));
        pinList.add(new Pin(35.0994, 138.8560, 133, "沼津駅", 5, 52));
        pinList.add(new Pin(35.1431, 138.7045, 148.2, "吉原駅", 6, 28));
        pinList.add(new Pin(35.1149, 138.5846, 160.5, "蒲原駅", 6, 56));
        pinList.add(new Pin(35.0507, 138.5217, 170.5, "興津駅", 7, 20));
        pinList.add(new Pin(35.0120, 138.4516, 178.9, "セブンイレブン清水七ツ新屋店", 7, 45));
        pinList.add(new Pin(34.9546, 138.3683, 190, "安倍川", 8, 25));
        pinList.add(new Pin(34.8732, 138.3239, 200.8, "焼津駅", 8, 52));
        pinList.add(new Pin(34.8565, 138.2489, 209.1, "藤枝駅", 9, 15));
        pinList.add(new Pin(34.8335, 138.1491, 219, "大井川", 9, 45));
        //250mくらい上り
        pinList.add(new Pin(34.7947, 138.0733, 229.5, "道の駅掛川", 10, 15));
        pinList.add(new Pin(34.7510, 137.9247, 245, "袋井市役所", 10, 51));
        pinList.add(new Pin(34.7085, 137.7586, 260, "天竜川駅", 11, 27));
        pinList.add(new Pin(34.6734, 137.6967, 271, "米津の浜", 11, 55));
        pinList.add(new Pin(34.6892, 137.6041, 280, "弁天島駅", 12, 20));
        pinList.add(new Pin(34.6795, 137.5072, 290.5, "潮見坂", 12, 45));
        pinList.add(new Pin(34.7227, 137.4381, 299, "のんほいパーク", 13, 10));
        pinList.add(new Pin(34.7685, 137.3906, 306.3, "豊橋市役所", 13, 30));
        pinList.add(new Pin(34.8583, 137.3097, 319, "名電赤坂駅", 14, 0));
        pinList.add(new Pin(34.9155, 137.2155, 330, "藤川駅", 14, 25));
        pinList.add(new Pin(34.9616, 137.1430, 339, "矢作橋駅", 14, 50));
        pinList.add(new Pin(35.0125, 137.0431, 350, "知立(ちりゅー)駅", 15, 15));
        pinList.add(new Pin(35.0606, 136.9808, 358, "中京競馬場前", 15, 36));
        pinList.add(new Pin(35.1217, 136.9075, 368, "熱田神宮", 16, 5));
        pinList.add(new Pin(35.1261, 136.8212, 376.2, "ファミリーマート中川江松店", 16, 30));
        pinList.add(new Pin(35.1071, 136.7167, 386, "尾張大橋", 17, 7));
        pinList.add(new Pin(35.0078, 136.6544, 399.4, "富田駅", 17, 42));
        pinList.add(new Pin(34.9426, 136.6030, 408, "南四日市駅", 18, 6));
        pinList.add(new Pin(34.8900, 136.5345, 418, "加佐登駅", 18, 31));
        pinList.add(new Pin(34.8519, 136.4503, 428, "亀山駅", 18, 56));
        pinList.add(new Pin(34.8558, 136.3812, 435.5, "大和街道分岐", 19, 16));
        pinList.add(new Pin(34.8465, 136.2859, 446, "伊賀手前の峠", 19, 51));
        pinList.add(new Pin(34.7756, 136.1297, 463, "伊賀上野城", 20, 28));
        pinList.add(new Pin(34.7630, 136.0249, 474, "月ケ瀬口駅", 20, 57));
        pinList.add(new Pin(34.7612, 135.9375, 484, "笠置駅", 21, 22));
        pinList.add(new Pin(34.7367, 135.8227, 497, "木津駅", 21, 53));
        pinList.add(new Pin(34.7297, 135.7307, 506, "奈良先端科学技術大学院大学", 22, 16));
        pinList.add(new Pin(34.7397, 135.6391, 515, "四條畷(なわて)市役所", 22, 41));
        pinList.add(new Pin(34.7128, 135.5464, 525, "関目駅", 23, 13));
        pinList.add(new Pin(34.6980, 135.5007, 530.7, "梅田新道", 23, 30));
    }

    @Override
    public void onLocationChanged(Location location) {
        int distance = MainActivity.getDistance();
        update = new Date();
        for(Pin pin: pinList) {
            if(pin.isArrived()) continue;
            if(!pin.isInPlace(location.getLatitude(), location.getLongitude(), distance)) continue;
            //現在地変更
            currentPin = pin;
            //フラグ変更
            pin.arrive();
        }
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
}
