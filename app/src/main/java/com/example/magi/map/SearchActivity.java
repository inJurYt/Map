package com.example.magi.map;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    private Button btn_citySearch;
    private Button btn_nearbySearch;
    private TextView tv_totalPage;
    private TextView tv_currPage;
    private TextView tv_totalNum;
    private PoiSearch mPoiSearch;
    private int currPage;
    private int totalPage;
    private String searchText;
    private LatLng latLng;
    private PoiResult mPoiResult;
    private int searchMode;
    List<PoiInfo> mList;
    private InputMethodManager inputManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        et_search = (EditText)findViewById(R.id.et_search);
        lv_result = (ListView)findViewById(R.id.lv_result);
        btn_pre = (Button)findViewById(R.id.btn_pre);
        btn_next = (Button)findViewById(R.id.btn_next);
        btn_citySearch = (Button)findViewById(R.id.btn_citySearch);
        btn_nearbySearch = (Button)findViewById(R.id.btn_nearbySearch);
        tv_currPage = (TextView)findViewById(R.id.currPage);
        tv_totalPage = (TextView)findViewById(R.id.totoalPage);
        tv_totalNum = (TextView)findViewById(R.id.totalNum);

        inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        String s = getIntent().getStringExtra("searchText");
        if(!s.equals("")){
            et_search.setText(s);
            et_search.selectAll();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(poiResultListener);
        btn_pre.setOnClickListener(btn_preListener);
        btn_next.setOnClickListener(btn_nextListener);
        btn_nearbySearch.setOnClickListener(btn_nearbySearchListener);
        btn_citySearch.setOnClickListener(btn_citySearchListener);
        lv_result.setOnItemClickListener(itemListener);
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
        option.radius(1000);
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
            if(searchMode == 0){
                mPoiSearch.searchNearby(getNearbySearchOption(currPage, latLng));
            }else {
                mPoiSearch.searchInCity(getCitySearchOption(currPage));
            }

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
            if(searchMode == 0){
                mPoiSearch.searchNearby(getNearbySearchOption(currPage, latLng));
            }else {
                mPoiSearch.searchInCity(getCitySearchOption(currPage));
            }
        }
    };

    ListView.OnItemClickListener itemListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(SearchActivity.this, MainActivity.class);
            intent.putExtra("mPoiResult", mPoiResult).putExtra("latLng", mList.get(position).location).putExtra("searchText", searchText).putExtra("index", position);
            startActivity(intent);
            finish();
        }
    };

    View.OnClickListener btn_nearbySearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(et_search.getText().toString().equals("") || et_search.getText() == null){
                lv_result.setVisibility(View.INVISIBLE);
                tv_totalPage.setVisibility(View.INVISIBLE);
                tv_currPage.setVisibility(View.INVISIBLE);
                tv_totalNum.setVisibility(View.INVISIBLE);
                btn_next.setVisibility(View.INVISIBLE);
                btn_pre.setVisibility(View.INVISIBLE);
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
                lv_result.setVisibility(View.INVISIBLE);
                tv_totalPage.setVisibility(View.INVISIBLE);
                tv_currPage.setVisibility(View.INVISIBLE);
                tv_totalNum.setVisibility(View.INVISIBLE);
                btn_next.setVisibility(View.INVISIBLE);
                btn_pre.setVisibility(View.INVISIBLE);
                Toast.makeText(SearchActivity.this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
                return;
            }
            totalPage = 0;
            currPage = 0;
            searchText = et_search.getText().toString();
            latLng = getIntent().getParcelableExtra("myLatLng");
            mPoiSearch.searchInCity(getCitySearchOption(0));
            searchMode = 1;
            inputManager.hideSoftInputFromWindow(et_search.getWindowToken(), 0);
        }
    };

    OnGetPoiSearchResultListener poiResultListener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            if (poiResult.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(SearchActivity.this, "未找到结果", Toast.LENGTH_SHORT).show();
                lv_result.setVisibility(View.INVISIBLE);
                tv_totalPage.setVisibility(View.INVISIBLE);
                tv_currPage.setVisibility(View.INVISIBLE);
                tv_totalNum.setVisibility(View.INVISIBLE);
                btn_next.setVisibility(View.INVISIBLE);
                btn_pre.setVisibility(View.INVISIBLE);
            } else {
                mPoiResult = poiResult;
                totalPage = poiResult.getTotalPageNum();
                mList = poiResult.getAllPoi();
                int count = mList.size();
                List<Map<String, Object>> lst = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", mList.get(i).name);
                    item.put("address", mList.get(i).address);
                    item.put("uid", mList.get(i).uid);
                    lst.add(item);
                }
                SimpleAdapter adpater = new SimpleAdapter(getApplicationContext(), lst, R.layout.list_item, new String[]{"name", "address", "uid"}, new int[]{R.id.tv_name, R.id.tv_address, R.id.tv_uid});
                lv_result.setAdapter(adpater);
                tv_totalNum.setText("共" + poiResult.getTotalPoiNum() + "条");
                tv_totalPage.setText("共" + totalPage + "页");
                tv_currPage.setText("第" + (currPage + 1) + "页");
                lv_result.setVisibility(View.VISIBLE);
                tv_totalPage.setVisibility(View.VISIBLE);
                tv_currPage.setVisibility(View.VISIBLE);
                tv_totalNum.setVisibility(View.VISIBLE);
                btn_next.setVisibility(View.VISIBLE);
                btn_pre.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }
    };
}
