package com.example.joseph.sweepersd.watchzone;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneObserver;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mGoogleMap;

    private Map<Long, WatchZonePresenter> mWatchZones = new HashMap<>();

    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;

    private GoogleMap.OnMapLongClickListener mLongClickListener;
    private CameraUpdate mCameraUpdate;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.content_watchzone_map_fragment, container, false);

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
        if (mLongClickListener != null) {
            mGoogleMap.setOnMapLongClickListener(mLongClickListener);
        }
        if (mCameraUpdate != null) {
            mGoogleMap.animateCamera(mCameraUpdate);
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
                if (presenter.watchZonePoints != null) {
                    for (Circle c : presenter.watchZonePoints) {
                        c.remove();
                    }
                }
                WatchZoneModelRepository.getInstance(getContext()).removeObserver(presenter.watchZoneObserver);
                WatchZoneModelRepository.getInstance(getContext()).removeObserver(presenter.watchZonePointsObserver);
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

    public void setOnLongClickListener (GoogleMap.OnMapLongClickListener listener) {
        mLongClickListener = listener;
        if (mGoogleMap != null) {
            mGoogleMap.setOnMapLongClickListener(mLongClickListener);
        }
    }

    public void animateCamera(CameraUpdate update) {
        mCameraUpdate = update;
        if (mGoogleMap != null) {
            mGoogleMap.animateCamera(mCameraUpdate);
        }
    }

    private void createPresenter(final Long watchZoneUid) {
        final WatchZonePresenter presenter = new WatchZonePresenter();
        presenter.watchZoneObserver = new WatchZoneObserver(watchZoneUid,
                new WatchZoneObserver.WatchZoneChangedCallback() {
            @Override
            public void onWatchZoneChanged(WatchZone watchZone) {
                Log.e("Joey", "WAtchzone changed!");
                presenter.watchZoneCenter.remove();
                presenter.watchZoneRadius.remove();

                if (presenter.watchZonePointsObserver != null) {
                    for (Circle point : presenter.watchZonePoints) {
                        point.remove();
                    }
                }
                WatchZoneModelRepository.getInstance(getContext()).removeObserver(
                        presenter.watchZonePointsObserver);
                createWatchZonePointsObserver(watchZoneUid, presenter);

                presenter.watchZoneRadius = mGoogleMap.addCircle(new CircleOptions()
                        .center(new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude()))
                        .radius(watchZone.getRadius())
                        .strokeColor(getResources().getColor(R.color.app_primary))
                        .fillColor(getResources().getColor(R.color.map_radius_fill)));
                presenter.watchZoneCenter = mGoogleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude())));
            }
            @Override
            public void onDataLoaded(WatchZone watchZone) {
                presenter.watchZoneRadius = mGoogleMap.addCircle(new CircleOptions()
                        .center(new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude()))
                        .radius(watchZone.getRadius())
                        .strokeColor(getResources().getColor(R.color.app_primary))
                        .fillColor(getResources().getColor(R.color.map_radius_fill)));
                presenter.watchZoneCenter = mGoogleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude())));
            }
            @Override
            public void onDataInvalid() {
                presenter.watchZoneCenter.remove();
                presenter.watchZoneRadius.remove();
            }
        });
        createWatchZonePointsObserver(watchZoneUid, presenter);
        WatchZoneModelRepository.getInstance(getContext()).observe(this, presenter.watchZoneObserver);
        mWatchZones.put(watchZoneUid, presenter);
    }

    private void createWatchZonePointsObserver(final Long watchZoneUid, final WatchZonePresenter presenter) {
        presenter.watchZonePointsObserver = new WatchZonePointsObserver(watchZoneUid,
                new WatchZonePointsObserver.WatchZonePointsChangedCallback() {
            @Override
            public void onWatchZonePointAdded(int index) {
                WatchZonePoint p = presenter.watchZonePointsObserver.getWatchZonePoints().get(index);
                CircleOptions circleOptions = new CircleOptions()
                        .center(new LatLng(p.getLatitude(), p.getLongitude()))
                        .radius(1.0)
                        .strokeColor(getResources().getColor(R.color.app_primary))
                        .fillColor(getResources().getColor(R.color.map_radius_fill));
                presenter.watchZonePoints.add(index, mGoogleMap.addCircle(circleOptions));
            }

            @Override
            public void onWatchZonePointRemoved(int index) {
                Circle circle = presenter.watchZonePoints.remove(index);
                circle.remove();
            }

            @Override
            public void onWatchZonePointUpdated(int index) {
                Circle circle = presenter.watchZonePoints.remove(index);
                circle.remove();

                WatchZonePoint p = presenter.watchZonePointsObserver.getWatchZonePoints().get(index);
                CircleOptions circleOptions = new CircleOptions()
                        .center(new LatLng(p.getLatitude(), p.getLongitude()))
                        .radius(1.0)
                        .strokeColor(getResources().getColor(R.color.app_primary))
                        .fillColor(getResources().getColor(R.color.map_radius_fill));
                presenter.watchZonePoints.add(index, mGoogleMap.addCircle(circleOptions));
            }

            @Override
            public void onDataLoaded(List<WatchZonePoint> watchZonePoints) {
                presenter.watchZonePoints = new ArrayList<>();
                for (WatchZonePoint p : watchZonePoints) {
                    CircleOptions circleOptions = new CircleOptions()
                            .center(new LatLng(p.getLatitude(), p.getLongitude()))
                            .radius(1.0)
                            .strokeColor(getResources().getColor(R.color.app_primary))
                            .fillColor(getResources().getColor(R.color.map_radius_fill));
                    presenter.watchZonePoints.add(mGoogleMap.addCircle(circleOptions));
                }
            }

            @Override
            public void onDataInvalid() {
                for (Circle point : presenter.watchZonePoints) {
                    point.remove();
                }
            }
        });
        WatchZoneModelRepository.getInstance(getContext()).observe(this,
                presenter.watchZonePointsObserver);
    }

    private class WatchZonePresenter {
        Marker watchZoneCenter;
        Circle watchZoneRadius;
        List<Circle> watchZonePoints;
        WatchZoneObserver watchZoneObserver;
        WatchZonePointsObserver watchZonePointsObserver;
    }
}
