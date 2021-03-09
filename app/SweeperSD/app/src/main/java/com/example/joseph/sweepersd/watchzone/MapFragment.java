package com.example.joseph.sweepersd.watchzone;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.utils.ChangeSet;
import com.example.joseph.sweepersd.watchzone.model.WatchZonePointModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZonePoint;
import com.example.joseph.sweepersd.watchzone.model.WatchZonePointsObserver;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mGoogleMap;

    private Map<Long, WatchZonePresenter> mWatchZones = new HashMap<>();

    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;

    private GoogleMap.OnMapClickListener mMapClickListener;
    private CameraUpdate mCameraUpdate;

    private int mDraggingRadius = -1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.content_map_fragment, container, false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mPaddingLeft = 0;
        mPaddingTop = 0;
        mPaddingRight = 0;
        mPaddingBottom = 0;
        return v;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        for (Long uid : mWatchZones.keySet()) {
            createPresenter(uid);
        }
        mGoogleMap.setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);
        if (mMapClickListener != null) {
            mGoogleMap.setOnMapClickListener(mMapClickListener);
        }
        if (mCameraUpdate != null) {
            mGoogleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    mGoogleMap.animateCamera(mCameraUpdate);
                }
            });
        }
    }

    public void addWatchZone(Long watchZoneUid) {
        if (mGoogleMap == null) {
            mWatchZones.put(watchZoneUid, null);
        } else if (!mWatchZones.containsKey(watchZoneUid)) {
            createPresenter(watchZoneUid);
        }
    }

    public void removeWatchZone(Long watchZoneUid) {
        if (mWatchZones.containsKey(watchZoneUid)) {
            WatchZonePresenter presenter = mWatchZones.get(watchZoneUid);
            if (presenter != null) {
                if (presenter.watchZoneCenter != null) {
                    presenter.watchZoneCenter.remove();
                }
                if (presenter.watchZoneRadius != null) {
                    presenter.watchZoneRadius.remove();
                }
                if (presenter.pointsToCircleMap != null) {
                    for (Long uid : presenter.pointsToCircleMap.keySet()) {
                        Circle point = presenter.pointsToCircleMap.get(uid);
                        point.remove();
                    }
                }
                WatchZoneModelRepository.getInstance(getContext()).getZoneModelForUid(watchZoneUid).removeObserver(presenter.watchZoneObserver);
                WatchZoneModelRepository.getInstance(getContext()).getZoneModelForUid(watchZoneUid).removeObserver(presenter.watchZonePointsObserver);
            }

            mWatchZones.remove(watchZoneUid);
        }
    }

    public void setMapPadding(int left, int top, int right, int bottom) {
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
        if (mGoogleMap != null) {
            mGoogleMap.setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);
        }
    }

    public void setMapClickListener(GoogleMap.OnMapClickListener listener) {
        mMapClickListener = listener;
        if (mGoogleMap != null) {
            mGoogleMap.setOnMapClickListener(mMapClickListener);
        }
    }

    public void animateCameraBounds(CameraUpdate update) {
        mCameraUpdate = update;
        if (mGoogleMap != null) {
            mGoogleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    mGoogleMap.animateCamera(mCameraUpdate);
                }
            });
        }
    }

    public void setDraggingRadius(Long watchZoneUid, int radius) {
        mDraggingRadius = radius;

        if (mDraggingRadius > -1) {
            WatchZonePresenter presenter = mWatchZones.get(watchZoneUid);
            if (presenter != null) {
                presenter.watchZoneRadius.setRadius(mDraggingRadius);
                presenter.watchZoneRadius.setStrokeColor(getResources().getColor(R.color.secondaryColor));
            }
        }
    }

    private void createPresenter(final Long watchZoneUid) {
        final WatchZonePresenter presenter = new WatchZonePresenter();
        presenter.watchZoneObserver = new WatchZoneModelObserver(watchZoneUid,
                new WatchZoneModelObserver.WatchZoneModelChangedCallback() {
            @Override
            public void onWatchZoneModelChanged(WatchZone watchZone) {
                boolean refreshMap = false;
                if (presenter.oldWatchZone.getRadius() != watchZone.getRadius() ||
                        presenter.oldWatchZone.getCenterLatitude() != watchZone.getCenterLatitude() ||
                        presenter.oldWatchZone.getCenterLongitude() != watchZone.getCenterLongitude()) {
                    refreshMap = true;
                }
                if (refreshMap) {
                    presenter.watchZoneCenter.remove();
                    presenter.watchZoneRadius.remove();

                    /*if (presenter.watchZonePointsObserver != null) {
                        for (Long uid : presenter.pointsToCircleMap.keySet()) {
                            Circle point = presenter.pointsToCircleMap.get(uid);
                            point.remove();
                        }
                    }
                    WatchZoneModelRepository.getInstance(getContext()).getZoneModelForUid(watchZoneUid).removeObserver(
                            presenter.watchZonePointsObserver);
                    createWatchZonePointsObserver(watchZoneUid, presenter);*/

                    presenter.watchZoneRadius = mGoogleMap.addCircle(new CircleOptions()
                            .center(new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude()))
                            .radius(mDraggingRadius > -1 ? mDraggingRadius : watchZone.getRadius())
                            .strokeColor(mDraggingRadius > -1 ? getResources().getColor(R.color.secondaryColor)
                                    : getResources().getColor(R.color.primaryColor))
                            .fillColor(getResources().getColor(R.color.primaryColorTranslucent)));
                    presenter.watchZoneCenter = mGoogleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude())));
                    mGoogleMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
                        @Override
                        public void onCircleClick(Circle circle) {

                        }
                    });
                }
            }
            @Override
            public void onDataLoaded(WatchZone watchZone) {
                presenter.oldWatchZone = watchZone;
                presenter.watchZoneRadius = mGoogleMap.addCircle(new CircleOptions()
                        .center(new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude()))
                        .radius(mDraggingRadius > -1 ? mDraggingRadius : watchZone.getRadius())
                        .strokeColor(getResources().getColor(R.color.primaryColor))
                        .fillColor(getResources().getColor(R.color.primaryColorTranslucent)));
                presenter.watchZoneCenter = mGoogleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude())));
            }
            @Override
            public void onDataInvalid() {
                if (presenter.watchZoneCenter != null) {
                    presenter.watchZoneCenter.remove();
                }
                if (presenter.watchZoneRadius != null) {
                    presenter.watchZoneRadius.remove();
                }
            }
        });
        //createWatchZonePointsObserver(watchZoneUid, presenter);
        WatchZoneModelRepository.getInstance(getContext()).getZoneModelForUid(watchZoneUid)
                .observe(this, presenter.watchZoneObserver);
        mWatchZones.put(watchZoneUid, presenter);
    }

    private void createWatchZonePointsObserver(final Long watchZoneUid, final WatchZonePresenter presenter) {
        presenter.watchZonePointsObserver = new WatchZonePointsObserver(watchZoneUid,
                new WatchZonePointsObserver.WatchZonePointsChangedCallback() {
            @Override
            public void onWatchZonePointsChanged(Map<Long, WatchZonePointModel> watchZonePointMap,
                                                 ChangeSet changeSet) {
                for (Long uid : changeSet.removedUids) {
                    Circle circle = presenter.pointsToCircleMap.remove(uid);
                    circle.remove();
                }
                for (Long uid : changeSet.changedUids) {
                    Circle circle = presenter.pointsToCircleMap.remove(uid);
                    circle.remove();

                    WatchZonePoint p = presenter.watchZonePointsObserver.getWatchZonePoints().get(uid).point;
                    CircleOptions circleOptions = new CircleOptions()
                            .center(new LatLng(p.getLatitude(), p.getLongitude()))
                            .radius(1.0)
                            .strokeColor(getResources().getColor(R.color.primaryColor))
                            .fillColor(getResources().getColor(R.color.primaryColor));
                    presenter.pointsToCircleMap.put(uid, mGoogleMap.addCircle(circleOptions));
                }
                for (Long uid : changeSet.addedUids) {
                    WatchZonePoint p = presenter.watchZonePointsObserver.getWatchZonePoints().get(uid).point;
                    CircleOptions circleOptions = new CircleOptions()
                            .center(new LatLng(p.getLatitude(), p.getLongitude()))
                            .radius(1.0)
                            .strokeColor(getResources().getColor(R.color.primaryColor))
                            .fillColor(getResources().getColor(R.color.primaryColor));
                    presenter.pointsToCircleMap.put(uid, mGoogleMap.addCircle(circleOptions));
                }
            }

            @Override
            public void onDataLoaded(Map<Long, WatchZonePointModel> watchZonePoints) {
                presenter.pointsToCircleMap = new HashMap<>();
                for (Long uid : watchZonePoints.keySet()) {
                    WatchZonePoint p = watchZonePoints.get(uid).point;
                    CircleOptions circleOptions = new CircleOptions()
                            .center(new LatLng(p.getLatitude(), p.getLongitude()))
                            .radius(1.0)
                            .strokeColor(getResources().getColor(R.color.primaryColor))
                            .fillColor(getResources().getColor(R.color.primaryColor));
                    presenter.pointsToCircleMap.put(uid, mGoogleMap.addCircle(circleOptions));
                }
            }

            @Override
            public void onDataInvalid() {
                if (presenter.pointsToCircleMap != null) {
                    for (Long uid : presenter.pointsToCircleMap.keySet()) {
                        Circle point = presenter.pointsToCircleMap.get(uid);
                        point.remove();
                    }
                }
            }
        });
        WatchZoneModelRepository.getInstance(getContext()).getZoneModelForUid(watchZoneUid).observe(this,
                presenter.watchZonePointsObserver);
    }

    private class WatchZonePresenter {
        Marker watchZoneCenter;
        Circle watchZoneRadius;
        WatchZone oldWatchZone;
        Map<Long, Circle> pointsToCircleMap;
        WatchZoneModelObserver watchZoneObserver;
        WatchZonePointsObserver watchZonePointsObserver;
    }
}
