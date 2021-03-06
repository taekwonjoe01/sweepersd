package com.example.joseph.sweepersd.watchzone;

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joseph.sweepersd.TabAdapter;
import com.example.joseph.sweepersd.archived.presentation.manualalarms.CreateAlarmLabelDialogFragment;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.example.joseph.sweepersd.utils.LongPreferenceLiveData;
import com.example.joseph.sweepersd.utils.Preferences;
import com.example.joseph.sweepersd.utils.WrapContentTabViewPager;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
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
import com.google.android.material.tabs.TabLayout;
import com.google.maps.android.SphericalUtil;
import com.hutchins.tbd.R;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class WatchZoneExplorerActivity extends WatchZoneBaseActivity {
    private static final String TAG = WatchZoneExplorerActivity.class.getSimpleName();
    private static final LatLng SAN_DIEGO_CENTER = new LatLng(32.720330, -117.157383);
    private static final double SAN_DIEGO_RADIUS_METERS = 120701.0; // 75 miles
    private static final LatLngBounds SAN_DIEGO_BOUNDS = new LatLngBounds(
            SphericalUtil.computeOffset(SAN_DIEGO_CENTER,
            SAN_DIEGO_RADIUS_METERS * Math.sqrt(2), 225),
            SphericalUtil.computeOffset(SAN_DIEGO_CENTER,
            SAN_DIEGO_RADIUS_METERS * Math.sqrt(2), 45));

    private static final long SHOW_TUTORIAL_AFTER_MS = 1000L * 60L * 60L * 48L;

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 0;

    private PlaceAutocompleteFragment mPlaceFragment;
    private MapFragment mMapFragment;

    private SlidingUpPanelLayout mSlidingPanelLayout;

    private SeekBar mRadiusSeekbar;

    private TabLayout mTabLayout;
    private WrapContentTabViewPager mTabViewPager;
    private LinearLayout mDragLayout;
    private ShortSummaryLayout mShortSummaryLayout;
    private FrameLayout mBufferLayout;

    private RelativeLayout mOverlay;
    private TextView mOverlayText;
    private Button mOverlayButton;

    private LimitsTabFragment mLimitsTabFragment;
    private CalendarTabFragment mCalendarTabFragment;
    private NotificationsTabFragment mNotificationsTabFragment;

    private Long mCurrentWatchZoneUid;
    private String mCurrentLabel;
    private double mCurrentLatitude;
    private double mCurrentLongitude;
    private int mCurrentRadius;

    private LongPreferenceLiveData mLastTutorialLiveData;

    private LatLng mLatLng;

    private boolean mSaveOnDestroy;

    private boolean mPermissionRequested;

    private Observer<WatchZoneModel> mModelObserver = new Observer<WatchZoneModel>() {
        @Override
        public void onChanged(@Nullable WatchZoneModel watchZoneModel) {
            int progress = -1;
            if (mUpdatingProgressMap != null) {
                Integer p = mUpdatingProgressMap.get(watchZoneModel.watchZone.getUid());
                if (p != null) {
                    progress = p.intValue();
                }
            }
            if (mSlidingPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.DRAGGING) {
                mShortSummaryLayout.setWatchZoneModel(watchZoneModel);
                mShortSummaryLayout.setUpdatingProgress(progress);
            } else {
                ShortSummaryLayout.SummaryDisplayMode displayMode =
                        mSlidingPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ?
                                ShortSummaryLayout.SummaryDisplayMode.EXPLORER_TITLE :
                                ShortSummaryLayout.SummaryDisplayMode.EXPLORER;
                mShortSummaryLayout.set(watchZoneModel, displayMode, progress);
            }

            mRadiusSeekbar.setVisibility(View.VISIBLE);
            mShortSummaryLayout.setVisibility(View.VISIBLE);
            mSlidingPanelLayout.setTouchEnabled(true);
        }
    };

    private LiveData<WatchZoneModel> mModelLiveData;
    private Map<Long, Integer> mUpdatingProgressMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_zone_explorer);

        mMapFragment= (MapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.watch_zone_map_fragment);
        mPlaceFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        mSlidingPanelLayout = findViewById(R.id.sliding_layout);
        mTabLayout = findViewById(R.id.tab_layout);
        mTabViewPager = findViewById(R.id.tab_viewpager);
        mRadiusSeekbar = findViewById(R.id.seekbar_radius);
        mDragLayout = findViewById(R.id.drag_view);
        mShortSummaryLayout = findViewById(R.id.short_summary_layout);
        mBufferLayout = findViewById(R.id.buffer_layout);

        mOverlay = findViewById(R.id.layout_overlay);
        mOverlayText = findViewById(R.id.textview_overlay);
        mOverlayButton = findViewById(R.id.overlay_button);

        mMapFragment.setMapPadding(0, getResources().getDimensionPixelOffset(R.dimen.explorer_map_padding_top),
                0, getResources().getDimensionPixelOffset(R.dimen.explorer_map_padding_bottom));
        mMapFragment.setMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                setCurrentZone(null, latLng, true);
            }
        });
        ImageButton searchButton = findViewById(R.id.place_autocomplete_search_button);
        searchButton.setImageResource(R.drawable.ic_search_secondary_24dp);
        ImageButton clearButton = findViewById(R.id.place_autocomplete_clear_button);
        clearButton.setImageResource(R.drawable.ic_clear_secondary_24dp);
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
        mShortSummaryLayout.setCallback(new ShortSummaryLayout.SummaryLayoutCallback() {
            @Override
            public void onSummaryActionClicked() {
                // This summary is a Save button.
                mSaveOnDestroy = true;
                finish();
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
                if (slideOffset > 0.2f) {
                    mShortSummaryLayout.setDisplayMode(ShortSummaryLayout.SummaryDisplayMode.EXPLORER_TITLE);
                } else {
                    mShortSummaryLayout.setDisplayMode(ShortSummaryLayout.SummaryDisplayMode.EXPLORER);
                }
            }

            @Override
            public void onPanelStateChanged(View panel,
                                            SlidingUpPanelLayout.PanelState previousState,
                                            SlidingUpPanelLayout.PanelState newState) {
                ShortSummaryLayout.SummaryDisplayMode displayMode =
                        mSlidingPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ?
                                ShortSummaryLayout.SummaryDisplayMode.EXPLORER_TITLE :
                                ShortSummaryLayout.SummaryDisplayMode.EXPLORER;
                mShortSummaryLayout.setDisplayMode(displayMode);
            }
        });
        TabAdapter tabAdapter = new TabAdapter(getSupportFragmentManager());
        mLimitsTabFragment = new LimitsTabFragment();
        mLimitsTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_limits));
        mCalendarTabFragment = new CalendarTabFragment();
        mCalendarTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_calendar));
        mNotificationsTabFragment = new NotificationsTabFragment();
        mNotificationsTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_notifications));
        //tabAdapter.addFragment(mLimitsTabFragment);
        tabAdapter.addFragment(mCalendarTabFragment);
        tabAdapter.addFragment(mNotificationsTabFragment);
        mTabViewPager.setAdapter(tabAdapter);
        mTabLayout.setupWithViewPager(mTabViewPager);

        mRadiusSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int radius = getRadiusForProgress(progress);
                mMapFragment.setDraggingRadius(mCurrentWatchZoneUid, radius);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mCurrentWatchZoneUid == 0L) {
                    return;
                }
                mMapFragment.setDraggingRadius(mCurrentWatchZoneUid, -1);
                mCurrentRadius = getRadiusForProgress(seekBar.getProgress());
                WatchZoneModelRepository.getInstance(WatchZoneExplorerActivity.this)
                        .updateWatchZone(mCurrentWatchZoneUid, mCurrentLabel,
                                mCurrentLatitude, mCurrentLongitude, mCurrentRadius,
                                WatchZone.REMIND_RANGE_DEFAULT,
                                WatchZone.REMIND_POLICY_DEFAULT);
            }
        });
        mDragLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                mSlidingPanelLayout.setPanelHeight(mShortSummaryLayout.getHeight());
                mBufferLayout.getLayoutParams().height = mShortSummaryLayout.getHeight();
                mBufferLayout.requestLayout();
                mDragLayout.removeOnLayoutChangeListener(this);
            }
        });
        mSaveOnDestroy = false;
        mPermissionRequested = false;
        mCurrentWatchZoneUid = 0L;

        mLastTutorialLiveData = new LongPreferenceLiveData(this, Preferences.PREFERENCE_LAST_EXPLORER_TUTORIAL_TIMESTAMP);
        mLastTutorialLiveData.observe(this, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long aLong) {
                long now = System.currentTimeMillis();
                long elapsedTime = now - aLong;
                if (elapsedTime > SHOW_TUTORIAL_AFTER_MS) {
                    mOverlay.setVisibility(View.VISIBLE);
                    mOverlayButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mOverlay.setVisibility(View.GONE);
                            SharedPreferences preferences =
                                    PreferenceManager.getDefaultSharedPreferences(WatchZoneExplorerActivity.this);
                            preferences.edit().putLong(Preferences.PREFERENCE_LAST_EXPLORER_TUTORIAL_TIMESTAMP,
                                    System.currentTimeMillis()).apply();
                        }
                    });
                } else {
                    mOverlay.setVisibility(View.GONE);
                }
            }
        });

        mShortSummaryLayout.setVisibility(View.INVISIBLE);
        mRadiusSeekbar.setVisibility(View.GONE);
        mSlidingPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        mSlidingPanelLayout.setTouchEnabled(false);
    }

    @Override
    public void onWatchZoneUpdateProgress(Map<Long, Integer> progressMap) {
        mUpdatingProgressMap = progressMap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        int permission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED && !mPermissionRequested) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION);
        } else if (permission == PackageManager.PERMISSION_GRANTED && mCurrentWatchZoneUid == 0L) {
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
            Log.e("Joey", "animating bounds");
            mMapFragment.animateCameraBounds(CameraUpdateFactory.newLatLngBounds(SAN_DIEGO_BOUNDS, 0));
        } else {
            LatLng center = new LatLng(mCurrentLatitude, mCurrentLongitude);
            LatLng southWest = SphericalUtil.computeOffset(center,
                    mCurrentRadius * Math.sqrt(2), 225);
            LatLng northEast = SphericalUtil.computeOffset(center,
                    mCurrentRadius * Math.sqrt(2), 45);
            LatLngBounds bounds = new LatLngBounds(southWest, northEast);
            mMapFragment.animateCameraBounds(CameraUpdateFactory.newLatLngBounds(bounds, 10));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            mPermissionRequested = true;
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
            WatchZoneModelRepository.getInstance(this).deleteWatchZone(mCurrentWatchZoneUid);
        }
    }

    private void setCurrentZone(String address, LatLng latLng, boolean animateCamera) {
        mShortSummaryLayout.setVisibility(View.INVISIBLE);
        mRadiusSeekbar.setVisibility(View.GONE);
        mSlidingPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        mSlidingPanelLayout.setTouchEnabled(false);

        if (!SAN_DIEGO_BOUNDS.contains(latLng)) {
            Toast.makeText(this, "You can only set zones near San Diego!", Toast.LENGTH_SHORT).show();
            return;
        }
        mLatLng = latLng;

        if (address == null) {
            address = LocationUtils.getAddressForLatLnt(
                    WatchZoneExplorerActivity.this, latLng);
            if (address == null) {
                address = "";
            } else if (address.contains(",")) {
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
        }

        if (mCurrentWatchZoneUid == 0L) {
            mCurrentWatchZoneUid = WatchZoneModelRepository.getInstance(this).createWatchZone(mCurrentLabel,
                    mCurrentLatitude, mCurrentLongitude, mCurrentRadius);
            mMapFragment.addWatchZone(mCurrentWatchZoneUid);
            mLimitsTabFragment.addWatchZone(mCurrentWatchZoneUid);
            mCalendarTabFragment.addWatchZone(mCurrentWatchZoneUid);
            mNotificationsTabFragment.setWatchZoneUid(mCurrentWatchZoneUid);
        } else {
            WatchZoneModelRepository.getInstance(WatchZoneExplorerActivity.this)
                    .updateWatchZone(mCurrentWatchZoneUid, mCurrentLabel,
                            mCurrentLatitude, mCurrentLongitude, mCurrentRadius,
                            WatchZone.REMIND_RANGE_DEFAULT,
                            WatchZone.REMIND_POLICY_DEFAULT);
        }

        if (mModelObserver != null && mModelLiveData != null) {
            mModelLiveData.removeObserver(mModelObserver);
        }

        mModelLiveData = WatchZoneModelRepository.getInstance(this).getZoneModelForUid(mCurrentWatchZoneUid);
        mModelLiveData.observe(this, mModelObserver);
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
