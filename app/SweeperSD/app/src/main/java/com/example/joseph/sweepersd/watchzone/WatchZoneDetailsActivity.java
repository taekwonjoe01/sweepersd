package com.example.joseph.sweepersd.watchzone;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.TabAdapter;
import com.example.joseph.sweepersd.utils.WrapContentTabViewPager;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.apache.commons.lang3.text.WordUtils;

import java.util.Map;

public class WatchZoneDetailsActivity extends WatchZoneBaseActivity {
    private static final String TAG = WatchZoneDetailsActivity.class.getSimpleName();
    public static final String KEY_WATCHZONE_ID = "KEY_WATCHZONE_ID";

    private Long mWatchZoneId;

    private TabLayout mTabLayout;
    private WrapContentTabViewPager mTabViewPager;
    private ShortSummaryLayout mShortSummaryLayout;
    private SlidingUpPanelLayout mSlidingPanelLayout;
    private LinearLayout mDragLayout;

    private LimitsTabFragment mLimitsTabFragment;
    private CalendarTabFragment mCalendarTabFragment;
    private NotificationsTabFragment mNotificationsTabFragment;

    private MapFragment mMapFragment;

    private Map<Long, Integer> mUpdatingProgressMap;

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

        mTabLayout = findViewById(R.id.tab_layout);
        mTabViewPager = findViewById(R.id.tab_viewpager);
        mShortSummaryLayout = findViewById(R.id.short_summary_layout);
        mSlidingPanelLayout = findViewById(R.id.sliding_layout);
        mDragLayout = findViewById(R.id.drag_view);

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

        WatchZoneModelRepository.getInstance(this).getZoneModelForUid(mWatchZoneId).observe(this, new Observer<WatchZoneModel>() {
            @Override
            public void onChanged(@Nullable WatchZoneModel watchZoneModel) {
                int progress = -1;
                if (mUpdatingProgressMap != null) {
                    Integer p = mUpdatingProgressMap.get(watchZoneModel.watchZone.getUid());
                    if (p != null) {
                        progress = p.intValue();
                    }
                }

                ShortSummaryLayout.SummaryAction action =
                        mSlidingPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ?
                                ShortSummaryLayout.SummaryAction.None :
                                ShortSummaryLayout.SummaryAction.Customize;
                mShortSummaryLayout.set(watchZoneModel, action, progress);

                mSlidingPanelLayout.setVisibility(View.VISIBLE);

                setMap(watchZoneModel.watchZone);
            }
        });

        mShortSummaryLayout.setCallback(new ShortSummaryLayout.SummaryLayoutCallback() {
            @Override
            public void onSummaryActionClicked() {
                //TODO
            }

            @Override
            public void onLayoutClicked() {
                mSlidingPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }

            @Override
            public void onMoreInfoClicked() {
                mSlidingPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        });
        mSlidingPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel,
                                            SlidingUpPanelLayout.PanelState previousState,
                                            SlidingUpPanelLayout.PanelState newState) {
                ShortSummaryLayout.SummaryAction action =
                        newState == SlidingUpPanelLayout.PanelState.EXPANDED ?
                                ShortSummaryLayout.SummaryAction.None :
                                ShortSummaryLayout.SummaryAction.Customize;
                mShortSummaryLayout.setSummaryAction(action);
            }
        });
        mDragLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                mSlidingPanelLayout.setPanelHeight(mShortSummaryLayout.getHeight());
            }
        });

        mSlidingPanelLayout.setVisibility(View.GONE);
    }

    @Override
    public void onWatchZoneUpdateProgress(Map<Long, Integer> progressMap) {
        mUpdatingProgressMap = progressMap;
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
                WatchZoneModelRepository.getInstance(this).deleteWatchZone(mWatchZoneId);
                return true;
        }
        return false;
    }

    private void setMap(WatchZone watchZone) {
        setTitle(WordUtils.capitalize(watchZone.getLabel()));
        LatLng latLng = new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude());

        LatLng southWest = SphericalUtil.computeOffset(latLng,
                ((double)watchZone.getRadius()) * Math.sqrt(2), 225);
        LatLng northEast = SphericalUtil.computeOffset(latLng,
                ((double)watchZone.getRadius()) * Math.sqrt(2), 45);
        LatLngBounds bounds = new LatLngBounds(southWest, northEast);
        mMapFragment.animateCameraBounds(CameraUpdateFactory.newLatLngBounds(bounds, 0));
    }

    @Override
    public void onBackPressed() {
        if (mSlidingPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ||
                mSlidingPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED) {
            mSlidingPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }
}
