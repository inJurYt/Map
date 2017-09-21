package com.example.magi.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.overlayutil.OverlayManager;
import com.baidu.mapapi.search.poi.PoiResult;

import java.util.ArrayList;
import java.util.List;

public class MyPoiOverlay extends OverlayManager {
    private PoiList mPoiList = null;
    private BitmapDrawable mBitmapDrawable;

    public MyPoiOverlay(BaiduMap baiduMap, BitmapDrawable mBitmapDrawable) {
        super(baiduMap);
        this.mBitmapDrawable = mBitmapDrawable;
    }

    public void setData(PoiList mPoiList) {
        this.mPoiList = mPoiList;
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
        if ((this.mPoiList == null) || (this.mPoiList.getCurrSize() == 0))
            return null;
        ArrayList<OverlayOptions> arrayList = new ArrayList<>();
        List<Storage> lst = mPoiList.getStorageList();
        for (int i = 0; i < lst.size(); i++) {
            if (lst.get(i).getLocation() == null)
                continue;
            Bundle bundle = new Bundle();
            bundle.putInt("index", i);
            bundle.putParcelable("info", lst.get(i));

            arrayList.add(new MarkerOptions()
                    .icon(BitmapDescriptorFactory
                            .fromBitmap(setNumToIcon(i + 1))).extraInfo(bundle)
                    .position(lst.get(i).getLocation()).extraInfo(bundle));
        }
        return arrayList;
    }

    private Bitmap setNumToIcon(int num) {
        BitmapDrawable bd = mBitmapDrawable;
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
