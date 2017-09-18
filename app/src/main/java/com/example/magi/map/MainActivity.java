package com.example.magi.map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
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
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private TextView et_click;
    private Button btn_location;
    private Button btn_satellite;
    private Button btn_traffic;
    private TextView tvForInfoWindow;
    private MapView bmapView;
    private BaiduMap mBaiduMap;
    private LocationClient mLocationClient;
    private boolean isSatelliteOpen;
    private boolean isTrafficMapOpen;
    private PoiSearch mPoiSearch;
    private InfoWindow infoWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        et_click = (TextView)findViewById(R.id.et_click);
        btn_location = (Button)findViewById(R.id.btn_location);
        btn_satellite = (Button)findViewById(R.id.btn_satellite);
        btn_traffic = (Button)findViewById(R.id.btn_traffic);

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
        mPoiSearch = PoiSearch.newInstance();
        et_click.setOnClickListener(et_clickListener);
        btn_location.setOnClickListener(btn_locationListener);
        btn_traffic.setOnClickListener(btn_trafficListener);
        btn_satellite.setOnClickListener(btn_satelliteListener);
        mBaiduMap.setOnMapClickListener(mOnMapClickListener);
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
            MyPoiOverlay overlay = new MyPoiOverlay(mBaiduMap);
            PoiResult poiResult = getIntent().getParcelableExtra("mPoiResult");
            overlay.setData(poiResult);
            overlay.addToMap();
            LatLng lng = getIntent().getParcelableExtra("latLng");
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(lng, 20.0f);
            mBaiduMap.animateMapStatus(mapStatusUpdate);
            et_click.setText(getIntent().getStringExtra("searchText"));
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
        option.setWifiCacheTimeOut(30000);
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
        }
    }

    View.OnClickListener et_clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            SharedPreferences sharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
            LatLng latLng = new LatLng(Double.parseDouble(sharedPreferences.getString("Latitude", "")), Double.parseDouble(sharedPreferences.getString("Longitude", "")));
            intent.putExtra("myLatLng", latLng).putExtra("city", sharedPreferences.getString("city", "")).putExtra("searchText", et_click.getText().toString());
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

    private class MyPoiOverlay extends OverlayManager {
        private PoiResult poiResult = null;

        private MyPoiOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        public void setData(PoiResult poiResult) {
            this.poiResult = poiResult;
        }

        @Override
        public boolean onMarkerClick(Marker marker) {
            return false;
        }

        @Override
        public boolean onPolylineClick(Polyline polyline){
            return false;
        }

        @Override
        public List<OverlayOptions> getOverlayOptions() {
            if ((this.poiResult == null)
                    || (this.poiResult.getAllPoi() == null))
                return null;
            ArrayList<OverlayOptions> arrayList = new ArrayList<>();
            for (int i = 0; i < poiResult.getAllPoi().size(); i++) {
                if (this.poiResult.getAllPoi().get(i).location == null)
                    continue;
                Bundle bundle = new Bundle();
                bundle.putInt("index", i);
                bundle.putString("uid", poiResult.getAllPoi().get(i).uid);
                bundle.putString("name", poiResult.getAllPoi().get(i).name);

                arrayList.add(new MarkerOptions()
                        .icon(BitmapDescriptorFactory
                                .fromBitmap(setNumToIcon(i + 1))).extraInfo(bundle)
                        .position(poiResult.getAllPoi().get(i).location).extraInfo(bundle));
            }
            return arrayList;
        }

        private Bitmap setNumToIcon(int num) {
            BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(R.drawable.icon_map_marker);
            Bitmap bitmap = bd.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setAntiAlias(true);
            int widthX;
            int heightY = 0;
            if (num < 10) {
                paint.setTextSize(30);
                widthX = 8;
                heightY = 6;
            } else {
                paint.setTextSize(20);
                widthX = 11;
            }
            canvas.drawText(String.valueOf(num),
                    ((bitmap.getWidth() / 2) - widthX),
                    ((bitmap.getHeight() / 2) + heightY), paint);
            return bitmap;
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
}