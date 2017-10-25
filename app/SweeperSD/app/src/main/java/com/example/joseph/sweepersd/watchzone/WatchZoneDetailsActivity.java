package com.example.joseph.sweepersd.watchzone;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.TabAdapter;
import com.example.joseph.sweepersd.utils.WrapContentTabViewPager;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.List;

public class WatchZoneDetailsActivity extends WatchZoneBaseActivity {
    private static final String TAG = WatchZoneDetailsActivity.class.getSimpleName();
    public static final String KEY_WATCHZONE_ID = "KEY_WATCHZONE_ID";

    private Long mWatchZoneId;

    private TabLayout mTabLayout;
    private WrapContentTabViewPager mTabViewPager;
    private LimitsTabFragment mLimitsTabFragment;
    private CalendarTabFragment mCalendarTabFragment;
    private NotificationsTabFragment mNotificationsTabFragment;

    private MapFragment mMapFragment;

    private List<LatLng> mCurrentFinishedWatchZonePoints = new ArrayList<>();

    private WatchZoneModel mWatchZoneModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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

        mMapFragment= (MapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.watch_zone_map_fragment);
        mMapFragment.addWatchZone(mWatchZoneId);

        TabAdapter tabAdapter = new TabAdapter(getSupportFragmentManager());
        mLimitsTabFragment = new LimitsTabFragment();
        mLimitsTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_limits));
        mLimitsTabFragment.addWatchZone(mWatchZoneId);
        mCalendarTabFragment = new CalendarTabFragment();
        mCalendarTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_calendar));
        mCalendarTabFragment.addWatchZone(mWatchZoneId);
        mNotificationsTabFragment = new NotificationsTabFragment();
        mNotificationsTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_notifications));
        mNotificationsTabFragment.setWatchZoneUid(mWatchZoneId);
        tabAdapter.addFragment(mLimitsTabFragment);
        tabAdapter.addFragment(mCalendarTabFragment);
        tabAdapter.addFragment(mNotificationsTabFragment);
        mTabViewPager.setAdapter(tabAdapter);
        mTabLayout.setupWithViewPager(mTabViewPager);

        WatchZoneModelRepository.getInstance(this).observe(this, new WatchZoneObserver(mWatchZoneId,
                new WatchZoneObserver.WatchZoneChangedCallback() {
            @Override
            public void onWatchZoneChanged(WatchZone watchZone) {
                invalidateUi(watchZone);
            }

            @Override
            public void onDataLoaded(WatchZone watchZone) {
                invalidateUi(watchZone);
            }

            @Override
            public void onDataInvalid() {
                finish();
            }
        }));
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

    private void invalidateUi(WatchZone watchZone) {
        setTitle(WordUtils.capitalize(watchZone.getLabel()));
        LatLng latLng = new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude());
        Log.e("Joey", "radius " + watchZone.getRadius());

        LatLng southWest = SphericalUtil.computeOffset(latLng,
                ((double)watchZone.getRadius()) * Math.sqrt(2), 225);
        LatLng northEast = SphericalUtil.computeOffset(latLng,
                ((double)watchZone.getRadius()) * Math.sqrt(2), 45);
        LatLngBounds bounds = new LatLngBounds(southWest, northEast);
        mMapFragment.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10));
    }
}
