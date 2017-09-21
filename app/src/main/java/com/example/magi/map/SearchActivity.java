package com.example.magi.map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.poi.PoiSortType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {
    private EditText et_search;
    private ListView lv_result;
    private Button btn_pre;
    private Button btn_next;
    private Button btn_first;
    private Button btn_last;
    private Button btn_return;
    private Button btn_citySearch;
    private Button btn_nearbySearch;
    private TextView tv_totalPage;
    private TextView tv_currPage;
    private TextView tv_totalNum;
    private Button btn_parkSearch;

    private PoiSearch mPoiSearch;
    private int currPage;
    private int totalPage;
    private String searchText;
    private LatLng latLng;
    private int searchMode;
    private InputMethodManager inputManager;
    private PoiList mPoiList;
    private List<Storage> mStorageList;
    private long firstTime = 0;

    final android.os.Handler poiHandler = new android.os.Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 1){
                if(mPoiList.getTotalNum() % 50 == 0){
                    totalPage = mPoiList.getTotalNum() / 50;
                }
                else {
                    totalPage = mPoiList.getTotalNum() / 50 + 1;
                }
                mPoiList.setTotalPage(totalPage);
                int count = mPoiList.getCurrSize();
                mStorageList = mPoiList.getStorageList();
                List<Map<String, Object>> lst = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", mStorageList.get(i).getName());
                    item.put("address", mStorageList.get(i).getAddress());
                    item.put("uid", mStorageList.get(i).getUid());
                    lst.add(item);
                }
                SimpleAdapter adpater = new SimpleAdapter(getApplicationContext(), lst, R.layout.list_item, new String[]{"name", "address", "uid"}, new int[]{R.id.tv_name, R.id.tv_address, R.id.tv_uid});
                lv_result.setAdapter(adpater);
                tv_totalNum.setText("共" + mPoiList.getTotalNum() + "条");
                tv_totalPage.setText("共" + totalPage + "页");
                tv_currPage.setText("第" + (currPage + 1) + "页");
                setPagingVisible(true);
                searchMode = 2;
            }
            else {
                Toast.makeText(SearchActivity.this, "未找到结果", Toast.LENGTH_SHORT).show();
                setPagingVisible(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        et_search = (EditText)findViewById(R.id.et_search);
        lv_result = (ListView)findViewById(R.id.lv_result);
        btn_pre = (Button)findViewById(R.id.btn_pre);
        btn_next = (Button)findViewById(R.id.btn_next);
        btn_first = (Button)findViewById(R.id.btn_first);
        btn_last = (Button)findViewById(R.id.btn_last);
        btn_return = (Button)findViewById(R.id.btn_return);
        btn_citySearch = (Button)findViewById(R.id.btn_citySearch);
        btn_nearbySearch = (Button)findViewById(R.id.btn_nearbySearch);
        tv_currPage = (TextView)findViewById(R.id.currPage);
        tv_totalPage = (TextView)findViewById(R.id.totoalPage);
        tv_totalNum = (TextView)findViewById(R.id.totalNum);
        btn_parkSearch = (Button)findViewById(R.id.btn_parkSearch);


        inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(poiResultListener);
        btn_pre.setOnClickListener(btn_preListener);
        btn_next.setOnClickListener(btn_nextListener);
        btn_first.setOnClickListener(btn_firstListener);
        btn_last.setOnClickListener(btn_lastListener);
        btn_return.setOnClickListener(btn_returnListener);
        btn_nearbySearch.setOnClickListener(btn_nearbySearchListener);
        btn_citySearch.setOnClickListener(btn_citySearchListener);
        lv_result.setOnItemClickListener(itemListener);
        btn_parkSearch.setOnClickListener(btn_parkSearchListener);

        mPoiList = new PoiList();
        mStorageList = new ArrayList<>();
    }

    @Override
    protected void onStop(){
        super.onStop();
        mPoiSearch.destroy();
    }

    @Override
    protected  void onPause(){
        super.onPause();
        mPoiSearch.destroy();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mPoiSearch.destroy();
    }

    private PoiNearbySearchOption getNearbySearchOption(int currPage, LatLng latLng){
        PoiNearbySearchOption option = new PoiNearbySearchOption();
        option.keyword(searchText);
        option.location(latLng);
        option.radius(2000);
        option.pageNum(currPage);
        option.pageCapacity(50);
        option.sortType(PoiSortType.distance_from_near_to_far);
        return option;
    }

    private PoiCitySearchOption getCitySearchOption(int currPage){
        PoiCitySearchOption option = new PoiCitySearchOption();
        option.keyword(searchText);
        option.city(getIntent().getStringExtra("city"));
        option.pageCapacity(50);
        option.pageNum(currPage);
        return option;
    }

    View.OnClickListener btn_preListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(currPage == 0){
                Toast.makeText(SearchActivity.this, "已经到头了", Toast.LENGTH_SHORT).show();
                return;
            }
            currPage -= 1;
            SearchNextByMode(currPage);
        }
    };

    View.OnClickListener btn_nextListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(currPage + 1 == totalPage){
                Toast.makeText(SearchActivity.this, "已经到尾了", Toast.LENGTH_SHORT).show();
                return;
            }
            currPage += 1;
            SearchNextByMode(currPage);
        }
    };

    View.OnClickListener btn_firstListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            currPage = 0;
            SearchNextByMode(currPage);
        }
    };

    private void SearchNextByMode(int currPage){
        if(searchMode == 0){
            mPoiSearch.searchNearby(getNearbySearchOption(currPage, latLng));
        }else {
            if(searchMode == 1){
                mPoiSearch.searchInCity(getCitySearchOption(currPage));
            }
            else {
                searchPark(currPage);
            }
        }
    }

    View.OnClickListener btn_lastListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            currPage = totalPage - 1;
            SearchNextByMode(currPage);
        }
    };

    ListView.OnItemClickListener itemListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(SearchActivity.this, MainActivity.class);
            intent.putExtra("mPoiList", mPoiList).putExtra("latLng", mStorageList.get(position).getLocation()).putExtra("index", position);
            startActivity(intent);
            finish();
        }
    };

    private void setPagingVisible(boolean visible){
        if(visible){
            lv_result.setVisibility(View.VISIBLE);
            tv_totalPage.setVisibility(View.VISIBLE);
            tv_currPage.setVisibility(View.VISIBLE);
            tv_totalNum.setVisibility(View.VISIBLE);
            btn_next.setVisibility(View.VISIBLE);
            btn_pre.setVisibility(View.VISIBLE);
            btn_first.setVisibility(View.VISIBLE);
            btn_last.setVisibility(View.VISIBLE);
        }
        else {
            lv_result.setVisibility(View.INVISIBLE);
            tv_totalPage.setVisibility(View.INVISIBLE);
            tv_currPage.setVisibility(View.INVISIBLE);
            tv_totalNum.setVisibility(View.INVISIBLE);
            btn_next.setVisibility(View.INVISIBLE);
            btn_pre.setVisibility(View.INVISIBLE);
            btn_first.setVisibility(View.INVISIBLE);
            btn_last.setVisibility(View.INVISIBLE);
        }
    }

    View.OnClickListener btn_nearbySearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(et_search.getText().toString().equals("") || et_search.getText() == null){
                setPagingVisible(false);
                Toast.makeText(SearchActivity.this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
                return;
            }
            totalPage = 0;
            currPage = 0;
            searchText = et_search.getText().toString();
            latLng = getIntent().getParcelableExtra("myLatLng");
            mPoiSearch.searchNearby(getNearbySearchOption(0, latLng));
            searchMode = 0;
            inputManager.hideSoftInputFromWindow(et_search.getWindowToken(), 0);
        }
    };

    View.OnClickListener btn_citySearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(et_search.getText().toString().equals("") || et_search.getText() == null){
                setPagingVisible(false);
                Toast.makeText(SearchActivity.this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
                return;
            }
            totalPage = 0;
            currPage = 0;
            searchText = et_search.getText().toString();
            mPoiSearch.searchInCity(getCitySearchOption(0));
            searchMode = 1;
            inputManager.hideSoftInputFromWindow(et_search.getWindowToken(), 0);
        }
    };

    OnGetPoiSearchResultListener poiResultListener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(final PoiResult poiResult) {
            if (poiResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(SearchActivity.this, "未找到结果", Toast.LENGTH_SHORT).show();
                setPagingVisible(false);
            } else {
                totalPage = poiResult.getTotalPageNum();
                List<PoiInfo> mList = poiResult.getAllPoi();
                int count = mList.size();
                List<Map<String, Object>> lst = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", mList.get(i).name);
                    item.put("address", mList.get(i).address);
                    lst.add(item);
                    Storage st = new Storage();
                    st.setName(mList.get(i).name);
                    st.setAddress(mList.get(i).address);
                    st.setLocation(mList.get(i).location);
                    st.setUid("0");
                    mStorageList.add(st);
                }
                mPoiList.setCurrSize(count);
                mPoiList.setTotalNum(poiResult.getTotalPoiNum());
                mPoiList.setTotalPage(poiResult.getTotalPageNum());
                mPoiList.setStorageList(mStorageList);
                SimpleAdapter adpater = new SimpleAdapter(getApplicationContext(), lst, R.layout.list_item, new String[]{"name", "address"}, new int[]{R.id.tv_name, R.id.tv_address});
                lv_result.setAdapter(adpater);
                tv_totalNum.setText("共" + poiResult.getTotalPoiNum() + "条");
                tv_totalPage.setText("共" + totalPage + "页");
                tv_currPage.setText("第" + (currPage + 1) + "页");
                setPagingVisible(true);
            }
        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }
    };

    View.OnClickListener btn_parkSearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            searchPark(currPage);
            inputManager.hideSoftInputFromWindow(et_search.getWindowToken(), 0);
        }
    };

    private void searchPark(final int currPage){
        SharedPreferences sharedPreferences = getSharedPreferences("setting", Context.MODE_PRIVATE);
        final LatLng l = new LatLng(Double.parseDouble(sharedPreferences.getString("Latitude", "")), Double.parseDouble(sharedPreferences.getString("Longitude", "")));
        Thread park = new Thread(new Runnable() {
            @Override
            public void run() {
                mPoiList = Park.getPark(l, currPage);
                if(mPoiList != null && mPoiList.getCurrSize() == 0){
                    Message message = new Message();
                    message.what = 0;
                    poiHandler.sendMessage(message);
                }
                else {
                    Message message = new Message();
                    message.what = 1;
                    message.obj = mPoiList;
                    poiHandler.sendMessage(message);
                }
            }
        });
        try{
            park.start();
        }
        catch (Exception e){
            e.printStackTrace();;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                long secondTime = System.currentTimeMillis();
                if(secondTime - firstTime > 2000){
                    Toast.makeText(SearchActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                    firstTime = secondTime;
                    return true;
                }else{
                    System.exit(0);
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    View.OnClickListener btn_returnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SearchActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    };
}
