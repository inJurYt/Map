package com.example.magi.map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.OverlayManager;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button btn_location;
    private Button btn_satellite;
    private Button btn_traffic;
    private Button btn_weather;
    private Button btn_goSearch;
    private TextView tvForInfoWindow;
    private TextView tv_weather;
    private InfoWindow infoWindow;

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

        tv_weather = (TextView)findViewById(R.id.tv_weather);
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
            GetWeather();
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
        if(getIntent().getParcelableExtra("mPoiResult") != null){
            mBaiduMap.clear();

            MyPoiOverlay overlay = new MyPoiOverlay(mBaiduMap, (BitmapDrawable)getResources().getDrawable(R.drawable.icon_map_marker));
            PoiResult poiResult = getIntent().getParcelableExtra("mPoiResult");
            overlay.setData(poiResult);
            overlay.addToMap();

            LatLng lng = getIntent().getParcelableExtra("latLng");
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(lng, 20.0f);
            mBaiduMap.animateMapStatus(mapStatusUpdate);

            mBaiduMap.setOnMarkerClickListener(mOnMarkerClickListener);
            tvForInfoWindow = new TextView(getApplicationContext());
            int index = getIntent().getIntExtra("index", 0);
            tvForInfoWindow.setText(poiResult.getAllPoi().get(index).name);
            tvForInfoWindow.setTextColor(Color.BLACK);
            tvForInfoWindow.setBackgroundColor(Color.TRANSPARENT);
            tvForInfoWindow.setPadding(5, 10, 5, 10);
            tvForInfoWindow.setTextSize(15);
            infoWindow = new InfoWindow(tvForInfoWindow, poiResult.getAllPoi().get(index).location, 0);
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
            tv_weather.setText(Weather.getWeather(location.getCity()));
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
        try{
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
        public boolean onMarkerClick(Marker marker) {
            tvForInfoWindow.setText(marker.getExtraInfo().getString("name"));
            infoWindow = new InfoWindow(tvForInfoWindow, marker.getPosition(), 0);
            mBaiduMap.showInfoWindow(infoWindow);
            return false;
        }
    };

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