package com.example.arlocation.location.sensor;

import android.content.Context;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.DPoint;
import com.example.arlocation.location.utils.GCJ2WGSUtils;

public class LocationManager {
    public Context mContext;
    public AMapLocationClient mLocationClient;
    public AMapLocationListener mLocationListener;
    public DPoint currentLocation=null;
    public AMapLocation currentAmapLocation=null;
    public LocationManager(Context context){
        this.mContext = context;
        //声明AMapLocationClient类对象
        mLocationClient = null;
        //初始化定位
        mLocationClient = new AMapLocationClient(mContext);
        //设置定位回调操作
        mLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation amapLocation) {
                if (amapLocation != null) {
                    if (amapLocation.getErrorCode() == 0) {
                        //解析定位结果
                        currentAmapLocation=amapLocation;
                        double gcjLat=currentAmapLocation.getLatitude();
                        double gcjLon=currentAmapLocation.getLongitude();
                        double wgsLat=GCJ2WGSUtils.WGSLat(gcjLat,gcjLon);
                        double wgsLon=GCJ2WGSUtils.WGSLon(gcjLat,gcjLon);
                        currentLocation=new DPoint(wgsLat,wgsLon);
                    }
                    else{
                        //错误信息
                    }
                }
                else{
                    //返回为空
                }
            }
        };
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
        //启动定位
        mLocationClient.startLocation();
        //异步获取定位结果
    }
}


