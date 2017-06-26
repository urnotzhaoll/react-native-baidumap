package com.yiyang.reactnativebaidumap;

import android.util.Log;
import android.graphics.Point;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.map.Projection;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.model.LatLngBounds;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.WritableMap;
import com.baidu.mapapi.map.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


/**
 * Created by yiyang on 16/2/29.
 */
public class ReactMapView {

    private MapView mMapView;

    private LocationClient mLocationClient;

    private ReactMapMyLocationConfiguration mConfiguration;

    private boolean autoZoomToSpan;

    private final BaiduMapViewManager baiduMapViewManager;

    private final Map<Marker, ReactMapMarker> markerMap = new HashMap<>();

    public boolean isAutoZoomToSpan() {
        return autoZoomToSpan;
    }

    public void setAutoZoomToSpan(boolean autoZoomToSpan) {
        this.autoZoomToSpan = autoZoomToSpan;
    }

    private List<ReactMapMarker> mMarkers = new ArrayList<ReactMapMarker>();
    private List<String> mMarkerIds = new ArrayList<String>();

    private List<ReactMapOverlay> mOverlays = new ArrayList<ReactMapOverlay>();
    private List<String> mOverlayIds = new ArrayList<String>();

    public ReactMapView(MapView mapView,BaiduMapViewManager baiduMapViewManager) {
        this.mMapView = mapView;
        this.baiduMapViewManager=baiduMapViewManager;
    }

    public BaiduMap getMap() {
        return this.mMapView.getMap();
    }

    public void setOverlays(List<ReactMapOverlay> overlays) {
        List<String> newOverlayIds = new ArrayList<String>();
        List<ReactMapOverlay> overlaysToDelete = new ArrayList<ReactMapOverlay>();
        List<ReactMapOverlay> overlaysToAdd = new ArrayList<ReactMapOverlay>();

        for (ReactMapOverlay overlay :
                overlays) {
            if (overlay instanceof ReactMapOverlay == false) {
                continue;
            }

            newOverlayIds.add(overlay.getId());

            if (!mOverlayIds.contains(overlay.getId())) {
                overlaysToAdd.add(overlay);
            }
        }

        for (ReactMapOverlay overlay :
                this.mOverlays) {
            if (overlay instanceof ReactMapOverlay == false) {
                continue;
            }

            if (!newOverlayIds.contains(overlay.getId())) {
                overlaysToDelete.add(overlay);
            }
        }

        if (!overlaysToDelete.isEmpty()) {
            for (ReactMapOverlay overlay :
                    overlaysToDelete) {
                overlay.getPolyline().remove();
                this.mOverlays.remove(overlay);
            }
        }

        if (!overlaysToAdd.isEmpty()) {
            for (ReactMapOverlay overlay:
                    overlaysToAdd) {
                if (overlay.getOptions() != null) {
                    overlay.addToMap(this.getMap());
                    this.mOverlays.add(overlay);
                }
            }
        }

        this.mOverlayIds = newOverlayIds;

    }

    public void setMarker(List<ReactMapMarker> markers) {

        List<String> newMarkerIds = new ArrayList<String>();
        List<ReactMapMarker> markersToDelete = new ArrayList<ReactMapMarker>();
        List<ReactMapMarker> markersToAdd = new ArrayList<ReactMapMarker>();

        for (ReactMapMarker marker :
                markers) {
            if (marker instanceof ReactMapMarker == false) {
                continue;
            }

            newMarkerIds.add(marker.getMarkerId());

            if (!mMarkerIds.contains(marker.getId())) {
                markersToAdd.add(marker);
            }
        }

        for (ReactMapMarker marker :
                this.mMarkers) {
            if (marker instanceof ReactMapMarker == false) {
                continue;
            }

            if (!newMarkerIds.contains(marker.getId())) {
                markersToDelete.add(marker);
            }

            markerMap.put(marker.getMarker(),marker);
        }

        if (!markersToDelete.isEmpty()) {
            for (ReactMapMarker marker :
                    markersToDelete) {
                marker.getMarker().remove();
                this.mMarkers.remove(marker);
            }
        }

        if (!markersToAdd.isEmpty()) {
            for (ReactMapMarker marker :
                    markersToAdd) {
                if (marker.getOptions() != null) {
                    marker.addToMap(this.getMap());
                    this.mMarkers.add(marker);
                }
            }
        }

        this.mMarkerIds = newMarkerIds;
    }

    public WritableMap makeClickEventData(Marker marker) {

        LatLng point=marker.getPosition();
        WritableMap event = new WritableNativeMap();

        WritableMap coordinate = new WritableNativeMap();
        coordinate.putDouble("latitude", point.latitude);
        coordinate.putDouble("longitude", point.longitude);
        event.putMap("coordinate", coordinate);

        Projection projection = this.mMapView.getMap().getProjection();
        Point screenPoint = projection.toScreenLocation(point);

        WritableMap position = new WritableNativeMap();
        position.putDouble("x", screenPoint.x);
        position.putDouble("y", screenPoint.y);
        event.putMap("position", position);

        event.putInt("id",marker.getZIndex());

        return event;
    }


    public void onMapLoaded() {
        if (this.autoZoomToSpan) {
            this.zoomToSpan();
        }
        final MapView view=this.mMapView;
        //对Marker的点击监听  
        this.mMapView.getMap().setOnMarkerClickListener(new OnMarkerClickListener()  
        {  
            @Override  
            public boolean onMarkerClick(final Marker marker)  
            {  
                WritableMap event;
                event = makeClickEventData(marker);
                event.putString("action", "annotation-click");
                // baiduMapViewManager.pushEvent(markerMap.get(marker),"onPress", event);
                baiduMapViewManager.pushEvent(view,"onPress", event);
                return true;  
            }  
        });  
        //对Markerd的拖拽监听
        this.mMapView.getMap().setOnMarkerDragListener(new BaiduMap.OnMarkerDragListener() {
            public void onMarkerDrag(Marker marker) {
                
            }
            public void onMarkerDragEnd(Marker marker) {  
                WritableMap event;
                event = makeClickEventData(marker);
                event.putString("action", "annotation-drag");
                event.putDouble("latitude", marker.getPosition().latitude);
                event.putDouble("longitude", marker.getPosition().longitude);
                baiduMapViewManager.pushEvent(view,"onPress", event);
            }
            public void onMarkerDragStart(Marker marker) {
              
            }
        });
        //对地图状态改变监听
        this.mMapView.getMap().setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            /** 
            * 手势操作地图，设置地图状态等操作导致地图状态开始改变。 
            * @param status 地图状态改变开始时的地图状态 
            */  
            public void onMapStatusChangeStart(MapStatus status){  
            }  
            /** 
            * 地图状态变化中 
            * @param status 当前地图状态 
            */  
            public void onMapStatusChange(MapStatus status){  
            }  
            /** 
            * 地图状态改变结束 
            * @param status 地图状态改变结束后的地图状态 
            */  
            public void onMapStatusChangeFinish(MapStatus status){
                WritableMap event = new WritableNativeMap();
                event.putDouble("latitude", status.target.latitude);
                event.putDouble("longitude",status.target.longitude);
                event.putString("action", "mapStatus-change");
                baiduMapViewManager.pushEvent(view,"onPress", event);
            }  
        });
    }

    public void zoomToSpan(List<ReactMapMarker> markers, List<ReactMapOverlay> overlays) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasBuilded = false;
        if (markers != null && markers.size() > 0) {
            for (ReactMapMarker marker:
                 markers) {
                if (marker != null && marker.getOptions() != null) {
                    LatLng location = marker.getOptions().getPosition();
                    builder.include(location);
                    hasBuilded = true;
                }
            }
        }
        if (overlays != null && overlays.size() > 0) {
            for (ReactMapOverlay overlay :
                    overlays) {
                if (overlay != null && overlay.getOptions() != null) {
                    for (LatLng location :
                            overlay.getOptions().getPoints()) {
                        builder.include(location);
                        hasBuilded = true;
                    }
                }
            }
        }
        if (hasBuilded) {
            this.getMap().animateMapStatus(MapStatusUpdateFactory.newLatLngBounds(builder.build()));
        }
    }

    public void zoomToSpan() {
        this.zoomToSpan(this.mMarkers, this.mOverlays);
    }

    public void setShowsUserLocation(boolean showsUserLocation) {
        if (getMap() == null) {
            return;
        }
        if (showsUserLocation != getMap().isMyLocationEnabled()) {
            getMap().setMyLocationEnabled(showsUserLocation);
            if (showsUserLocation && mLocationClient == null) {
                mLocationClient = new LocationClient(mMapView.getContext());
                BaiduLocationListener listener = new BaiduLocationListener(mLocationClient, new BaiduLocationListener.ReactLocationCallback() {
                    @Override
                    public void onSuccess(BDLocation bdLocation) {

                        float radius = 0;
                        if (mConfiguration != null && mConfiguration.isShowAccuracyCircle()) {
                            radius = bdLocation.getRadius();
                        }
                        MyLocationData locData = new MyLocationData.Builder()
                                .accuracy(radius)
                                .latitude(bdLocation.getLatitude())
                                .longitude(bdLocation.getLongitude())
                                .build();
                        if (getMap().isMyLocationEnabled()) {

                            getMap().setMyLocationData(locData);
                        }
                    }

                    @Override
                    public void onFailure(BDLocation bdLocation) {
                        Log.e("RNBaidumap", "error: " + bdLocation.getLocType());
                    }
                });
                mLocationClient.setLocOption(getLocationOption());
                mLocationClient.registerLocationListener(listener);
                mLocationClient.start();
            } else if (showsUserLocation) {
                if (mLocationClient.isStarted()) {
                    mLocationClient.requestLocation();
                } else {
                    mLocationClient.start();
                }
            } else if (mLocationClient != null) {
                if (mLocationClient.isStarted()) {
                    mLocationClient.stop();
                }
            }
        }
    }

    public void setConfiguration(ReactMapMyLocationConfiguration configuration) {
        this.mConfiguration = configuration;
        this.mConfiguration.setConfigurationUpdateListener(new ReactMapMyLocationConfiguration.ConfigurationUpdateListener() {
            @Override
            public void onConfigurationUpdate(ReactMapMyLocationConfiguration aConfiguration) {
                if (getMap() != null) {
                    getMap().setMyLocationConfigeration(aConfiguration.getConfiguration());
                }
            }
        });
        if (getMap() != null) {
            getMap().setMyLocationConfigeration(configuration.getConfiguration());
        }
    }

    private LocationClientOption getLocationOption() {
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(1000);
        option.setCoorType("bd09ll");
        return option;
    }

}
