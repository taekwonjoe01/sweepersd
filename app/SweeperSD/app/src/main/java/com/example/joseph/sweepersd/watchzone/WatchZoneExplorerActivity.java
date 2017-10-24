package com.example.joseph.sweepersd.watchzone;

import android.app.DialogFragment;
import android.arch.lifecycle.Observer;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.TabAdapter;
import com.example.joseph.sweepersd.archived.presentation.manualalarms.CreateAlarmLabelDialogFragment;
import com.example.joseph.sweepersd.archived.utils.LocationUtils;
import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.utils.WrapContentTabViewPager;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneLimitModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelUpdater;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneRepository;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneExplorerActivity extends AppCompatActivity {
    private static final String TAG = WatchZoneExplorerActivity.class.getSimpleName();

    private PlaceAutocompleteFragment mPlaceFragment;
    private MapFragment mMapFragment;
    private FloatingActionButton mSaveButton;

    private SlidingUpPanelLayout mSlidingPanelLayout;

    private SeekBar mRadiusSeekbar;

    private TabLayout mTabLayout;
    private WrapContentTabViewPager mTabViewPager;
    private LinearLayout mDragLayout;

    private LimitsTabFragment mLimitsTabFragment;
    private CalendarTabFragment mCalendarTabFragment;

    private Long mCurrentWatchZoneUid;
    private String mCurrentLabel;
    private double mCurrentLatitude;
    private double mCurrentLongitude;
    private int mCurrentRadius;

    private LatLng mLatLng;

    private ProgressBar mProgressBar;

    private boolean mSaveOnDestroy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_zone_explorer);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment= (MapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.watch_zone_map_fragment);
        mPlaceFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        mSaveButton = findViewById(R.id.button_save_zone);
        mSlidingPanelLayout = findViewById(R.id.sliding_layout);
        mTabLayout = findViewById(R.id.tab_layout);
        mTabViewPager = findViewById(R.id.tab_viewpager);
        mRadiusSeekbar = findViewById(R.id.seekbar_radius);
        mDragLayout = findViewById(R.id.drag_view);
        mProgressBar = findViewById(R.id.progress_updating);
        //mRecyclerView = findViewById(R.id.limit_recycler_view);
        //mLayoutManager = new LinearLayoutManager(this, LinearLayout.VERTICAL, false);

        mMapFragment.setMapPadding(0, getResources().getDimensionPixelOffset(R.dimen.explorer_map_padding_top),
                0, getResources().getDimensionPixelOffset(R.dimen.explorer_map_padding_bottom));
        mMapFragment.setOnLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                setCurrentZone(null, latLng, true);
            }
        });
        mPlaceFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                setCurrentZone(place.getAddress().toString(), place.getLatLng(),
                        true);
            }

            @Override
            public void onError(Status status) {

            }
        });
        ((EditText)findViewById(R.id.place_autocomplete_search_input)).setTextColor(
                getResources().getColor(android.R.color.white));
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSaveOnDestroy = true;
                finish();
            }
        });
        mSlidingPanelLayout.setAnchorPoint(0.4f);
        mSlidingPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel,
                                            SlidingUpPanelLayout.PanelState previousState,
                                            SlidingUpPanelLayout.PanelState newState) {

            }
        });
        TabAdapter tabAdapter = new TabAdapter(getSupportFragmentManager());
        mLimitsTabFragment = new LimitsTabFragment();
        mLimitsTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_limits));
        mCalendarTabFragment = new CalendarTabFragment();
        mCalendarTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_calendar));
        tabAdapter.addFragment(mLimitsTabFragment);
        tabAdapter.addFragment(mCalendarTabFragment);
        mTabViewPager.setAdapter(tabAdapter);
        mTabLayout.setupWithViewPager(mTabViewPager);
        //mRecyclerView.setLayoutManager(mLayoutManager);

        //int itemMargin = getResources().getDimensionPixelSize(R.dimen.limit_view_item_space);
        //mLimitViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

        //mRecyclerView.addItemDecoration(mLimitViewItemDecoration);

        /*RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }*/

        mRadiusSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mCurrentWatchZoneUid == 0L) {
                    return;
                }
                mCurrentRadius = getRadiusForProgress(seekBar.getProgress());
                /*WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this)
                        .deleteWatchZone(mCurrentWatchZoneUid);
                mCurrentWatchZoneUid = WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this)
                        .createWatchZone(mCurrentLabel,
                        mCurrentLatitude, mCurrentLongitude, mCurrentRadius);*/
                WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this)
                        .updateWatchZone(mCurrentWatchZoneUid, mCurrentLabel,
                                mCurrentLatitude, mCurrentLongitude, mCurrentRadius,
                                WatchZone.REMIND_RANGE_DEFAULT,
                                WatchZone.REMIND_POLICY_DEFAULT);
                /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WatchZoneExplorerActivity.this);
                preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_EXPLORER_UID, mCurrentWatchZoneUid).commit();*/
                setAlarmLocation(mLatLng);
            }
        });
        mDragLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                //mSlidingPanelLayout.setParallaxOffset();
                //mSlidingPanelLayout.setPanelHeight(findViewById(R.id.drag_view).getHeight());



                ViewGroup.MarginLayoutParams saveButtonLayoutParams =
                        (ViewGroup.MarginLayoutParams) mSaveButton.getLayoutParams();
                mSlidingPanelLayout.setPanelHeight(mSaveButton.getHeight()
                        //+ saveButtonLayoutParams.topMargin
                        + saveButtonLayoutParams.bottomMargin);



                //mSlidingPanelLayout.setParallaxOffset(mDragLayout.getHeight());
                //mSlidingPanelLayout.setAnchorPoint(0.5f);
                /*ViewGroup.LayoutParams lp = mTransparentSliderView.getLayoutParams();
                lp.height = mDragLayout.getHeight();
                mTransparentSliderView.setLayoutParams(lp);*/
            }
        });
        WatchZoneModelUpdater.getInstance(this).observe(this, new Observer<Map<Long, Integer>>() {
            @Override
            public void onChanged(@Nullable Map<Long, Integer> longIntegerMap) {
                if (longIntegerMap != null) {
                    if (longIntegerMap.containsKey(mCurrentWatchZoneUid)) {
                        mProgressBar.setVisibility(View.VISIBLE);
                        mProgressBar.setProgress(longIntegerMap.get(mCurrentWatchZoneUid));
                    } else {
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        mSaveOnDestroy = false;
        mCurrentWatchZoneUid = 0L;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);
        } else if (mCurrentWatchZoneUid == 0L) {
            LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        setCurrentZone(null,
                                new LatLng(location.getLatitude(), location.getLongitude()),
                                true);
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        dismissCreateLabelDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCurrentWatchZoneUid != 0L && !mSaveOnDestroy) {
            WatchZoneRepository.getInstance(this).deleteWatchZone(mCurrentWatchZoneUid);
        }

        /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                WatchZoneExplorerActivity.this);
        preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_EXPLORER_UID, 0L).commit();*/
    }

    private void setCurrentZone(String address, LatLng latLng, boolean animateCamera) {
        /*if (mCurrentWatchZoneUid != 0L) {
            WatchZoneRepository.getInstance(this).deleteWatchZone(mCurrentWatchZoneUid);
            mMapFragment.removeWatchZone(mCurrentWatchZoneUid);
            mLimitsTabFragment.removeWatchZone(mCurrentWatchZoneUid);
        }*/

        setAlarmLocation(latLng);
        if (animateCamera) {
            mMapFragment.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.5f));
            mSlidingPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
        }

        if (address == null) {
            address = LocationUtils.getAddressForLatLnt(
                    WatchZoneExplorerActivity.this, latLng);

            if (address.contains(",")) {
                address = address.split(",")[0];
            }
        }

        if (TextUtils.isEmpty(address)) {
            mPlaceFragment.setText(latLng.latitude + ", "
                    + latLng.longitude);
        } else {
            mPlaceFragment.setText(address);
        }

        mCurrentLabel = address;
        mCurrentLatitude = latLng.latitude;
        mCurrentLongitude = latLng.longitude;
        mCurrentRadius = getRadiusForProgress(mRadiusSeekbar.getProgress());
        if (mCurrentWatchZoneUid == 0L) {
            mCurrentWatchZoneUid = WatchZoneRepository.getInstance(this).createWatchZone(mCurrentLabel,
                    mCurrentLatitude, mCurrentLongitude, mCurrentRadius);
            mMapFragment.addWatchZone(mCurrentWatchZoneUid);
            mLimitsTabFragment.addWatchZone(mCurrentWatchZoneUid);
            mCalendarTabFragment.addWatchZone(mCurrentWatchZoneUid);
            /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                    WatchZoneExplorerActivity.this);
            preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_EXPLORER_UID,
                    mCurrentWatchZoneUid).commit();*/
        } else {
            WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this)
                    .updateWatchZone(mCurrentWatchZoneUid, mCurrentLabel,
                            mCurrentLatitude, mCurrentLongitude, mCurrentRadius,
                            WatchZone.REMIND_RANGE_DEFAULT,
                            WatchZone.REMIND_POLICY_DEFAULT);
        }
        /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WatchZoneExplorerActivity.this);
        preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_EXPLORER_UID, mCurrentWatchZoneUid).commit();*/
    }

    private void dismissCreateLabelDialog() {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(
                CreateAlarmLabelDialogFragment.class.getSimpleName());
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void setAlarmLocation(LatLng location) {
        mLatLng = location;

        mSlidingPanelLayout.getAnchorPoint();
        mSlidingPanelLayout.getPanelState();
    }

    private int getRadiusForProgress(int progress) {
        double percentage = (double) progress / 100;
        return 30 + (int)(percentage * 270.0);
    }
}
