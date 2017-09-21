package com.example.magi.map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button btn_location;
    private Button btn_satellite;
    private Button btn_traffic;
    private Button btn_weather;
    private Button btn_goSearch;
    private Button tv_weather;

    private InfoWindow infoWindow;
    private TextView tvForInfoWindow;

    private MapView bmapView;
    private BaiduMap mBaiduMap;
    private LocationClient mLocationClient;
    private PoiSearch mPoiSearch;

    private boolean isSatelliteOpen;
    private boolean isTrafficMapOpen;
    private long firstTime = 0;
    private String weather;
    private boolean weatherFirst = true;

    final android.os.Handler weatherHandler = new android.os.Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 1){
                tv_weather.setText((String)msg.obj);
                if(!weatherFirst){
                    Toast.makeText(MainActivity.this, "天气更新成功", Toast.LENGTH_SHORT).show();
                }
                weatherFirst = false;
            }
            else {
                tv_weather.setText("加载失败");
                Toast.makeText(MainActivity.this, "天气更新失败", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        tv_weather = (Button)findViewById(R.id.tv_weather);
        btn_weather = (Button)findViewById(R.id.btn_weather);
        btn_location = (Button)findViewById(R.id.btn_location);
        btn_satellite = (Button)findViewById(R.id.btn_satellite);
        btn_traffic = (Button)findViewById(R.id.btn_traffic);
        btn_goSearch = (Button)findViewById(R.id.btn_goSearch);

        initMapView(isSatelliteOpen, isTrafficMapOpen);

        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener( new MyLocationListener() );
        mLocationClient.setLocOption(getLocationOption());
        if(getIntent().getParcelableExtra("latLng") == null){
            mLocationClient.start();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mBaiduMap.setOnMapClickListener(mOnMapClickListener);
        mPoiSearch = PoiSearch.newInstance();
        btn_weather.setOnClickListener(btn_weatherListener);
        btn_location.setOnClickListener(btn_locationListener);
        btn_traffic.setOnClickListener(btn_trafficListener);
        btn_satellite.setOnClickListener(btn_satelliteListener);
        btn_goSearch.setOnClickListener(btn_goSearchListener);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mLocationClient.stop();
        mPoiSearch.destroy();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bmapView.onDestroy();
        mPoiSearch.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bmapView.onResume();
        if(getIntent().getParcelableExtra("mPoiList") != null){
            mBaiduMap.clear();

            MyPoiOverlay overlay = new MyPoiOverlay(mBaiduMap, (BitmapDrawable)getResources().getDrawable(R.drawable.icon_map_marker));
            PoiList mPoiList = getIntent().getParcelableExtra("mPoiList");
            List<Storage> lst = mPoiList.getStorageList();
            overlay.setData(mPoiList);
            overlay.addToMap();

            LatLng lng = getIntent().getParcelableExtra("latLng");
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(lng, 20.0f);
            mBaiduMap.animateMapStatus(mapStatusUpdate);

            mBaiduMap.setOnMarkerClickListener(mOnMarkerClickListener);
            tvForInfoWindow = new TextView(getApplicationContext());
            int index = getIntent().getIntExtra("index", 0);
            tvForInfoWindow.setTextColor(Color.BLACK);
            tvForInfoWindow.setBackgroundColor(Color.TRANSPARENT);
            tvForInfoWindow.setPadding(5, 10, 5, 10);
            tvForInfoWindow.setTextSize(15);
            tvForInfoWindow.setText(lst.get(index).getName());
            final Storage info = lst.get(index);
            infoWindow = new InfoWindow(tvForInfoWindow, info.getLocation(), 0);
            mBaiduMap.showInfoWindow(infoWindow);

            SharedPreferences sharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
            tv_weather.setText(sharedPreferences.getString("weather", ""));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        bmapView.onPause();
        mPoiSearch.destroy();
    }

    private void initMapView(boolean isSatelliteOpen, boolean isTrafficMapOpen) {
        bmapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = bmapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
        if(!isSatelliteOpen){
            mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            mBaiduMap.setTrafficEnabled(isTrafficMapOpen);
        }
        else {
            mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
        }
    }

    private LocationClientOption getLocationOption(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        option.setCoorType("bd09ll");
        option.setWifiCacheTimeOut(1000);
        return option;
    }

    private class MyLocationListener extends BDAbstractLocationListener{
        @Override
        public void onReceiveLocation(BDLocation location){
            MyLocationData locData = new MyLocationData.Builder().accuracy(location.getRadius()).latitude(location.getLatitude()).longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(myLatLng, 18.0f);
            SharedPreferences sharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor= sharedPreferences.edit();
            editor.putString("Latitude", Double.toString(location.getLatitude()));
            editor.putString("Longitude", Double.toString(location.getLongitude()));
            editor.putString("city", location.getCity());
            editor.apply();
            mBaiduMap.animateMapStatus(mapStatusUpdate);
            Toast.makeText(MainActivity.this, "定位成功", Toast.LENGTH_SHORT).show();
            mLocationClient.stop();
            if(weatherFirst){
                GetWeather();
            }
        }
    }

    View.OnClickListener btn_goSearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            SharedPreferences sharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("weather", tv_weather.getText().toString());
            editor.apply();
            LatLng latLng = new LatLng(Double.parseDouble(sharedPreferences.getString("Latitude", "")), Double.parseDouble(sharedPreferences.getString("Longitude", "")));
            intent.putExtra("myLatLng", latLng).putExtra("city", sharedPreferences.getString("city", ""));
            startActivity(intent);
            finish();
        }
    };

    View.OnClickListener btn_satelliteListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(!isSatelliteOpen){
                initMapView(true, false);
                isSatelliteOpen = !isSatelliteOpen;
                isTrafficMapOpen = false;
                btn_traffic.setEnabled(false);
                Toast.makeText(MainActivity.this, "卫星图开启", Toast.LENGTH_SHORT).show();

            }else {
                initMapView(false, false);
                isSatelliteOpen = !isSatelliteOpen;
                isTrafficMapOpen = false;
                btn_traffic.setEnabled(true);
                Toast.makeText(MainActivity.this, "卫星图关闭", Toast.LENGTH_SHORT).show();
            }
        }
    };

    View.OnClickListener btn_trafficListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(!isTrafficMapOpen){
                initMapView(false, true);
                isTrafficMapOpen = true;
                btn_satellite.setEnabled(false);
                Toast.makeText(MainActivity.this, "交通图开启", Toast.LENGTH_SHORT).show();

            }else {
                initMapView(false, false);
                isTrafficMapOpen = false;
                btn_satellite.setEnabled(true);
                Toast.makeText(MainActivity.this, "交通图关闭", Toast.LENGTH_SHORT).show();
            }
        }
    };

    View.OnClickListener btn_locationListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mLocationClient.start();
            mLocationClient.requestLocation();
        }
    };

    View.OnClickListener btn_weatherListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            GetWeather();
        }
    };

    private void GetWeather(){
        SharedPreferences sharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
        final String city = sharedPreferences.getString("city", "");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                weather = Weather.getWeather(city);
                if(weather != null && weather == ""){
                    Message message = new Message();
                    message.what = 0;
                    weatherHandler.sendMessage(message);
                }
                else {
                    Message message = new Message();
                    message.what = 1;
                    message.obj = weather;
                    weatherHandler.sendMessage(message);
                }
            }
        });
        try{
            thread.start();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    BaiduMap.OnMapClickListener mOnMapClickListener = new BaiduMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            mBaiduMap.hideInfoWindow();
        }

        @Override
        public boolean onMapPoiClick(MapPoi mapPoi) {
            return false;
        }
    };

    BaiduMap.OnMarkerClickListener mOnMarkerClickListener = new BaiduMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {//TODO 待完善
            final Storage info = (Storage)marker.getExtraInfo().getParcelable("info");
            tvForInfoWindow.setText(info.getName());
            android.graphics.Point p = mBaiduMap.getProjection().toScreenLocation(info.getLocation());
            p.y -= 47;
            LatLng latLng = mBaiduMap.getProjection().fromScreenLocation(p);
            infoWindow = new InfoWindow(tvForInfoWindow, latLng, 0);
            mBaiduMap.showInfoWindow(infoWindow);

            final View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.popwindow_marker, null, false);
            Button btn_info = (Button)view.findViewById(R.id.btn_info);
            if(info.getUid().equals("1")){
                btn_info.setText("  名称:" + toShortString(info.getName(), 15) + "\r\n  地址:" + toShortString(info.getAddress(), 15) + "\r\n  类型:" + info.getStyle() + "\r\n  总车位:" + info.getTotal() + "个      空车位：" + info.getEmpty() + "个\r\n  价格(时/元):" + info.getTime1() +"(0-8点)," + info.getTime2() + "(8-16)," + info.getTime3() + "(16-24)" );
            }
            else {
                btn_info.setText("  名称:" + toShortString(info.getName(), 15) + "\r\n  地址:" + toShortString(info.getAddress(), 15));
            }

            final Button btn_go = (Button)view.findViewById(R.id.btn_go);
            btn_go.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "准备前往" + info.getName(), Toast.LENGTH_SHORT).show();
                }
            });
            final PopupWindow popWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            popWindow.setTouchable(true);
            popWindow.setTouchInterceptor(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
            popWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
            popWindow.showAsDropDown((View)tvForInfoWindow, 0, 500);

            return false;
        }
    };

    public static String toShortString(String str, int length){
        if(str.length() > length){
            return str.substring(0, length - 1) + "\r\n              " + str.substring(length - 1, str.length());
        }
        else {
            return str;
        }
    }

    InfoWindow.OnInfoWindowClickListener mOnInfoWindowClickListener = new InfoWindow.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick() {
            mBaiduMap.hideInfoWindow();
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                long secondTime = System.currentTimeMillis();
                if(secondTime - firstTime > 2000){
                    Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                    firstTime = secondTime;
                    return true;
                }else{
                    System.exit(0);
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }
}