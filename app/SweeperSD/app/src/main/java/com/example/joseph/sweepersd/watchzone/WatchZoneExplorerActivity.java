package com.example.joseph.sweepersd.watchzone;

import android.app.DialogFragment;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.TabAdapter;
import com.example.joseph.sweepersd.archived.presentation.manualalarms.CreateAlarmLabelDialogFragment;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.example.joseph.sweepersd.utils.WrapContentTabViewPager;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneRepository;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.SphericalUtil;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Map;

public class WatchZoneExplorerActivity extends WatchZoneBaseActivity {
    private static final String TAG = WatchZoneExplorerActivity.class.getSimpleName();
    private static final LatLng SAN_DIEGO_CENTER = new LatLng(32.720330, -117.157383);
    private static final double SAN_DIEGO_RADIUS_METERS = 120701.0; // 75 miles
    private static final LatLngBounds SAN_DIEGO_BOUNDS = new LatLngBounds(
            SphericalUtil.computeOffset(SAN_DIEGO_CENTER,
            SAN_DIEGO_RADIUS_METERS * Math.sqrt(2), 225),
            SphericalUtil.computeOffset(SAN_DIEGO_CENTER,
            SAN_DIEGO_RADIUS_METERS * Math.sqrt(2), 45));

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_zone_explorer);

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

        mMapFragment.setMapPadding(0, getResources().getDimensionPixelOffset(R.dimen.explorer_map_padding_top),
                0, getResources().getDimensionPixelOffset(R.dimen.explorer_map_padding_bottom));
        mMapFragment.setOnLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                setCurrentZone(null, latLng, true);
            }
        });
        mPlaceFragment.setBoundsBias(SAN_DIEGO_BOUNDS);
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
                WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this)
                        .updateWatchZone(mCurrentWatchZoneUid, mCurrentLabel,
                                mCurrentLatitude, mCurrentLongitude, mCurrentRadius,
                                WatchZone.REMIND_RANGE_DEFAULT,
                                WatchZone.REMIND_POLICY_DEFAULT);
            }
        });
        mDragLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                ViewGroup.MarginLayoutParams saveButtonLayoutParams =
                        (ViewGroup.MarginLayoutParams) mSaveButton.getLayoutParams();
                mSlidingPanelLayout.setPanelHeight(mSaveButton.getHeight()
                        + saveButtonLayoutParams.bottomMargin);
            }
        });
        mSaveOnDestroy = false;
        mCurrentWatchZoneUid = 0L;
    }

    @Override
    public void onWatchZoneUpdateProgress(Map<Long, Integer> progressMap) {
        if (progressMap.containsKey(mCurrentWatchZoneUid)) {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(progressMap.get(mCurrentWatchZoneUid));
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
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
        } else if (mCurrentWatchZoneUid == 0L) {
            mMapFragment.animateCameraBounds(CameraUpdateFactory.newLatLngBounds(SAN_DIEGO_BOUNDS, 0));
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
    }

    private void setCurrentZone(String address, LatLng latLng, boolean animateCamera) {
        if (!SAN_DIEGO_BOUNDS.contains(latLng)) {
            Toast.makeText(this, "You can only set zones near San Diego!", Toast.LENGTH_SHORT).show();
            return;
        }
        mLatLng = latLng;

        if (address == null) {
            address = LocationUtils.getAddressForLatLnt(
                    WatchZoneExplorerActivity.this, latLng);

            if (address.contains(",")) {
                address = address.split(",")[0];
            }
        } else {
            if (address.contains(",")) {
                address = address.split(",")[0];
            }
        }

        if (TextUtils.isEmpty(address)) {
            address = latLng.latitude + ", "
                    + latLng.longitude;
        }
        mPlaceFragment.setText(address);

        mCurrentLabel = address;
        mCurrentLatitude = mLatLng.latitude;
        mCurrentLongitude = mLatLng.longitude;
        mCurrentRadius = getRadiusForProgress(mRadiusSeekbar.getProgress());

        if (animateCamera) {
            LatLng center = new LatLng(mCurrentLatitude, mCurrentLongitude);
            LatLng southWest = SphericalUtil.computeOffset(center,
                    mCurrentRadius * Math.sqrt(2), 225);
            LatLng northEast = SphericalUtil.computeOffset(center,
                    mCurrentRadius * Math.sqrt(2), 45);
            LatLngBounds bounds = new LatLngBounds(southWest, northEast);
            mMapFragment.animateCameraBounds(CameraUpdateFactory.newLatLngBounds(bounds, 10));
            mSlidingPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
        }

        if (mCurrentWatchZoneUid == 0L) {
            mCurrentWatchZoneUid = WatchZoneModelRepository.getInstance(this).createWatchZone(mCurrentLabel,
                    mCurrentLatitude, mCurrentLongitude, mCurrentRadius);
            mMapFragment.addWatchZone(mCurrentWatchZoneUid);
            mLimitsTabFragment.addWatchZone(mCurrentWatchZoneUid);
            mCalendarTabFragment.addWatchZone(mCurrentWatchZoneUid);
        } else {
            WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this)
                    .updateWatchZone(mCurrentWatchZoneUid, mCurrentLabel,
                            mCurrentLatitude, mCurrentLongitude, mCurrentRadius,
                            WatchZone.REMIND_RANGE_DEFAULT,
                            WatchZone.REMIND_POLICY_DEFAULT);
        }
    }

    private void dismissCreateLabelDialog() {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(
                CreateAlarmLabelDialogFragment.class.getSimpleName());
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private int getRadiusForProgress(int progress) {
        double percentage = (double) progress / 100;
        return 30 + (int)(percentage * 270.0);
    }
}
