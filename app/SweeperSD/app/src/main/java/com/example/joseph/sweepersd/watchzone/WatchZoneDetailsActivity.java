package com.example.joseph.sweepersd.watchzone;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.TabAdapter;
import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.utils.WrapContentTabViewPager;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneLimitModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZonePoint;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneDetailsActivity extends AppCompatActivity implements
        OnMapReadyCallback {
    private static final String TAG = WatchZoneDetailsActivity.class.getSimpleName();
    public static final String KEY_WATCHZONE_ID = "KEY_WATCHZONE_ID";

    private Long mWatchZoneId;

    private TabLayout mTabLayout;
    private WrapContentTabViewPager mTabViewPager;
    private LimitsTabFragment mLimitsTabFragment;
    private CalendarTabFragment mCalendarTabFragment;
    private WatchZoneNotificationsTabFragment mNotificationsTabFragment;

    private GoogleMap mMap;

    private List<LatLng> mCurrentFinishedWatchZonePoints = new ArrayList<>();

    private WatchZoneModel mWatchZoneModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_zone_details);

        Bundle b = getIntent().getExtras();
        if(b == null) {
            finish();
        }
        mWatchZoneId = b.getLong(KEY_WATCHZONE_ID);
        if (mWatchZoneId == 0L) {
            Log.e(TAG, "INVALID WATCH ZONE ID, FINISHING ACTIVITY");
            finish();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        mTabLayout = findViewById(R.id.tab_layout);
        mTabViewPager = findViewById(R.id.tab_viewpager);
        setSupportActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        TabAdapter tabAdapter = new TabAdapter(getSupportFragmentManager());
        mLimitsTabFragment = new LimitsTabFragment();
        mLimitsTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_limits));
        mCalendarTabFragment = new CalendarTabFragment();
        mCalendarTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_calendar));
        mNotificationsTabFragment = new WatchZoneNotificationsTabFragment();
        mNotificationsTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_notifications));
        tabAdapter.addFragment(mLimitsTabFragment);
        tabAdapter.addFragment(mCalendarTabFragment);
        tabAdapter.addFragment(mNotificationsTabFragment);
        mTabViewPager.setAdapter(tabAdapter);
        mTabLayout.setupWithViewPager(mTabViewPager);

        WatchZoneModelRepository.getInstance(this).observe(this, new Observer<WatchZoneModelRepository>() {
            @Override
            public void onChanged(@Nullable WatchZoneModelRepository repository) {
                if (!WatchZoneModelRepository.getInstance(WatchZoneDetailsActivity.this)
                        .watchZoneExists(mWatchZoneId)) {
                    finish();
                } else {
                    boolean showWatchZoneOnMap = mWatchZoneModel == null;
                    mWatchZoneModel = repository.getWatchZoneModel(mWatchZoneId);
                    if (showWatchZoneOnMap) {
                        showWatchZoneOnMap();
                    }
                    invalidateUi();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_watch_zone_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                WatchZoneRepository.getInstance(this).deleteWatchZone(mWatchZoneId);
                return true;
        }
        return false;
    }

    private void invalidateUi() {
        if (mWatchZoneModel != null) {
            WatchZone watchZone = mWatchZoneModel.getWatchZone();
            if (watchZone != null) {
                setTitle(WordUtils.capitalize(watchZone.getLabel()));
                List<WatchZonePoint> watchZonePoints = mWatchZoneModel.getWatchZonePointsModel().getWatchZonePointsList();
                invalidateWatchZonePoints(watchZonePoints);

                Map<Limit, List<LimitSchedule>> limitsAndSchedules = new HashMap<>();
                for (Long limitUid : mWatchZoneModel.getWatchZoneLimitModelUids()) {
                    WatchZoneLimitModel limitModel = mWatchZoneModel.getWatchZoneLimitModel(limitUid);
                    Limit limit = limitModel.getLimit();
                    if (limitModel != null && limit != null && limitModel
                            .getLimitSchedulesModel().getScheduleList() != null) {
                        limitsAndSchedules.put(limit, limitModel
                                .getLimitSchedulesModel().getScheduleList());
                    }
                }

                mLimitsTabFragment.setLimitsAndSchedules(limitsAndSchedules);
            }
        }
    }

    private void invalidateWatchZonePoints(final List<WatchZonePoint> watchZonePoints) {
        if (mMap == null) {
            return;
        }
        if (watchZonePoints != null) {
            List<WatchZonePoint> finishedPoints = new ArrayList<>();
            for (WatchZonePoint p : watchZonePoints) {
                if (p.getAddress() != null) {
                    finishedPoints.add(p);
                }
            }
            for (WatchZonePoint p : finishedPoints) {
                LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
                if (!mCurrentFinishedWatchZonePoints.contains(latLng)) {
                    mCurrentFinishedWatchZonePoints.add(latLng);
                    mMap.addCircle(new CircleOptions()
                            .center(latLng)
                            .radius(1.0)
                            .strokeColor(getResources().getColor(R.color.app_primary))
                            .fillColor(getResources().getColor(R.color.map_radius_fill)));
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        showWatchZoneOnMap();
    }

    private void showWatchZoneOnMap() {
        if (mWatchZoneModel != null && mMap != null) {
            WatchZone watchZone = mWatchZoneModel.getWatchZone();
            if (watchZone != null) {
                LatLng center = new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude());
                mMap.clear();

                mMap.addCircle(new CircleOptions()
                        .center(center)
                        .radius(watchZone.getRadius())
                        .strokeColor(getResources().getColor(R.color.app_primary))
                        .fillColor(getResources().getColor(R.color.map_radius_fill)));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 15f));
            }
        }
    }
}
