package com.example.joseph.sweepersd.revision3;

import android.app.DialogFragment;
import android.arch.lifecycle.Observer;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.presentation.manualalarms.CreateAlarmLabelDialogFragment;
import com.example.joseph.sweepersd.revision3.limit.Limit;
import com.example.joseph.sweepersd.revision3.limit.LimitSchedule;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneLimitModel;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneModel;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneModelRepository;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneModelUpdater;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZonePoint;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneRepository;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.roomorama.caldroid.CaldroidFragment;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneExplorerActivity extends AppCompatActivity implements
        OnMapReadyCallback {
    private static final String TAG = WatchZoneExplorerActivity.class.getSimpleName();

    private PlaceAutocompleteFragment mPlaceFragment;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
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

    private Circle mMarkerRadius;
    private List<LatLng> mCurrentFinishedWatchZonePoints = new ArrayList<>();

    private LatLng mLatLng;

    private String mLabel;

    private ProgressBar mProgressBar;

    private boolean mSaveOnDestroy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_zone_explorer);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
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

        mapFragment.getMapAsync(this);
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
                mMarkerRadius.setRadius(getRadiusForProgress(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mCurrentRadius = getRadiusForProgress(seekBar.getProgress());
                WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this)
                        .deleteWatchZone(mCurrentWatchZoneUid);
                mCurrentWatchZoneUid = WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this)
                        .createWatchZone(mCurrentLabel,
                        mCurrentLatitude, mCurrentLongitude, mCurrentRadius);
                setAlarmLocation(mLatLng);
                /*WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this).updateWatchZone(mCurrentWatchZoneUid,
                        mCurrentLabel, mCurrentLatitude, mCurrentLongitude, mCurrentRadius);*/
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

        WatchZoneModelRepository.getInstance(this).observe(this,
                new Observer<WatchZoneModelRepository>() {
            @Override
            public void onChanged(@Nullable WatchZoneModelRepository repository) {
                if (repository.watchZoneExists(mCurrentWatchZoneUid)) {
                    WatchZoneModel thisModel = repository.getWatchZoneModel(mCurrentWatchZoneUid);
                    if (thisModel != null) {
                        List<WatchZonePoint> watchZonePoints = thisModel.getWatchZonePointsModel().getWatchZonePointsList();
                        invalidateWatchZonePoints(watchZonePoints);

                        Map<Limit, List<LimitSchedule>> limitsAndSchedules = new HashMap<>();
                        for (Long limitUid : thisModel.getWatchZoneLimitModelUids()) {
                            WatchZoneLimitModel limitModel = thisModel.getWatchZoneLimitModel(limitUid);
                            Limit limit = limitModel.getLimit();
                            if (limitModel != null && limit != null && limitModel
                                    .getLimitSchedulesModel().getScheduleList() != null) {
                                limitsAndSchedules.put(limit, limitModel
                                        .getLimitSchedulesModel().getScheduleList());
                            }
                        }

                        mLimitsTabFragment.setLimitsAndSchedules(limitsAndSchedules);
                    }
                } else {

                }
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

    class TabAdapter extends FragmentPagerAdapter {
        private final List<TabFragment> mFragmentList = new ArrayList<>();

        public TabAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(TabFragment fragment) {
            mFragmentList.add(fragment);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentList.get(position).getTabTitle();
        }
    }

    public abstract static class TabFragment extends Fragment {
        abstract String getTabTitle();
        abstract void setTabTitle(String tabTitle);
    }

    public static class LimitsTabFragment extends TabFragment {

        private String mTabTitle;

        private RecyclerView mRecyclerView;
        private LimitViewAdapter mAdapter;
        private RecyclerView.LayoutManager mLayoutManager;
        private WatchZoneViewItemDecoration mLimitViewItemDecoration;

        public LimitsTabFragment() {

        }

        public void setLimitsAndSchedules(Map<Limit, List<LimitSchedule>> limitsAndSchedules) {
            mAdapter.setPostedLimits(limitsAndSchedules);
        }

        @Override
        public void setTabTitle(String tabTitle) {
            mTabTitle = tabTitle;
        }

        @Override
        String getTabTitle() {
            return mTabTitle;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.content_posted_limits_list, container, false);

            mRecyclerView = view.findViewById(R.id.posted_limits_recycler_view);
            //mRecyclerView.setHasFixedSize(true);
            mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayout.HORIZONTAL,
                    false);
            mRecyclerView.setLayoutManager(mLayoutManager);

            int itemMargin = getResources().getDimensionPixelSize(R.dimen.alarm_view_item_space);
            mLimitViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

            mRecyclerView.addItemDecoration(mLimitViewItemDecoration);

            RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
            if (animator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
            }

            mAdapter = new LimitViewAdapter();

            mRecyclerView.setAdapter(mAdapter);

            return view;
            //return inflater.inflate(R.layout.content_posted_limits, container, false);
        }
    }

    public static class CalendarTabFragment extends TabFragment {

        private String mTabTitle;
        public CalendarTabFragment() {

        }

        @Override
        public void setTabTitle(String tabTitle) {
            mTabTitle = tabTitle;
        }

        @Override
        String getTabTitle() {
            return mTabTitle;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.content_posted_limits_calendar, container, false);

            CaldroidFragment caldroidFragment = new CaldroidFragment();
            Bundle args = new Bundle();
            Calendar cal = Calendar.getInstance();
            args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1);
            args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR));
            args.putInt(CaldroidFragment.THEME_RESOURCE, R.style.CaldroidDefaultDark);
            caldroidFragment.setArguments(args);

            FragmentTransaction t = getActivity().getSupportFragmentManager().beginTransaction();
            t.replace(R.id.caldroid_fragment_layout, caldroidFragment);
            t.commit();
            return v;
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
        mMap.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.explorer_map_padding_top),
                0, getResources().getDimensionPixelOffset(R.dimen.explorer_map_padding_bottom));

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                setCurrentZone(null, latLng, true);
            }
        });
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ) {
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

    private void invalidateWatchZonePoints(final List<WatchZonePoint> watchZonePoints) {
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

    private void setCurrentZone(String address, LatLng latLng, boolean animateCamera) {
        if (mCurrentWatchZoneUid != 0L) {
            WatchZoneRepository.getInstance(this).deleteWatchZone(mCurrentWatchZoneUid);
        }

        setAlarmLocation(latLng);
        if (animateCamera) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.5f));
            mSlidingPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
        }

        if (address == null) {
            address = LocationUtils.getAddressForLatLnt(
                    WatchZoneExplorerActivity.this, latLng);
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
        mCurrentWatchZoneUid = WatchZoneRepository.getInstance(this).createWatchZone(mCurrentLabel,
                mCurrentLatitude, mCurrentLongitude, mCurrentRadius);
    }

    private void showCreateLabelDialog() {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(
                CreateAlarmLabelDialogFragment.class.getSimpleName());
        if (fragment == null) {
            fragment = new CreateAlarmLabelDialogFragment().newInstance(
                    new CreateAlarmLabelDialogFragment.CreateAlarmLabelDialogListener() {
                @Override
                public void onLabelCreated(String label) {
                    mLabel = label;
                    /*long uid = WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this).createWatchZone(
                            mLabel, mLatLng.latitude, mLatLng.longitude,
                            getRadiusForProgress(mRadiusSeekbar.getProgress()));*/

                    /*Intent returnIntent = new Intent();
                    returnIntent.putExtra(LABEL_KEY, mLabel);
                    returnIntent.putExtra(LOCATION_KEY, mLatLng);
                    returnIntent.putExtra(
                            RADIUS_KEY, getRadiusForProgress(mRadiusSeekbar.getProgress()));
                    returnIntent.putExtra(RADIUS_KEY,
                            getRadiusForProgress(mRadiusSeekbar.getProgress()));
                    setResult(Activity.RESULT_OK,returnIntent);*/
                    finish();
                }
            });
        }
        fragment.show(getFragmentManager(), CreateAlarmLabelDialogFragment.class.getSimpleName());
    }

    private void dismissCreateLabelDialog() {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(
                CreateAlarmLabelDialogFragment.class.getSimpleName());
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void setAlarmLocation(LatLng location) {
        mMap.clear();
        mCurrentFinishedWatchZonePoints.clear();

        mLatLng = location;

        mMarkerRadius = mMap.addCircle(new CircleOptions()
                .center(mLatLng)
                .radius(getRadiusForProgress(mRadiusSeekbar.getProgress()))
                .strokeColor(getResources().getColor(R.color.app_primary))
                .fillColor(getResources().getColor(R.color.map_radius_fill)));

        mSlidingPanelLayout.getAnchorPoint();
        mSlidingPanelLayout.getPanelState();
    }

    private int getRadiusForProgress(int progress) {
        double percentage = (double) progress / 100;
        return 30 + (int)(percentage * 270.0);
    }
}
