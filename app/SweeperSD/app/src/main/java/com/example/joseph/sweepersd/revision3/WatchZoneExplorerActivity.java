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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.presentation.manualalarms.CreateAlarmLabelDialogFragment;
import com.example.joseph.sweepersd.presentation.manualalarms.LimitViewAdapter2;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneModelRepository;
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
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

public class WatchZoneExplorerActivity extends AppCompatActivity implements
        OnMapReadyCallback {
    private static final String TAG = WatchZoneExplorerActivity.class.getSimpleName();

    private PlaceAutocompleteFragment mPlaceFragment;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private FloatingActionButton mSaveButton;

    private SlidingUpPanelLayout mSlidingPanelLayout;

    private TextView mRadiusDisplay;
    private SeekBar mRadiusSeekbar;

    private TabLayout mTabLayout;
    private WrapContentTabViewPager mTabViewPager;
    private LinearLayout mDragLayout;

    private Long mCurrentWatchZoneUid;

    private RecyclerView mRecyclerView;
    private LimitViewAdapter2 mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private WatchZoneViewItemDecoration mLimitViewItemDecoration;



    private Circle mMarkerRadius;

    private LatLng mLatLng;

    private String mLabel;

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
        mRadiusDisplay = findViewById(R.id.radius_display);
        mRadiusSeekbar = findViewById(R.id.seekbar_radius);
        mDragLayout = findViewById(R.id.drag_view);
        //mRecyclerView = findViewById(R.id.limit_recycler_view);
        //mLayoutManager = new LinearLayoutManager(this, LinearLayout.VERTICAL, false);

        mapFragment.getMapAsync(this);
        mPlaceFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.e("Joey", "onPlaceSelected " + place.getAddress());
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

            }
        });
        TabAdapter tabAdapter = new TabAdapter(getSupportFragmentManager());
        LimitsTabFragment limitsTabFragment = new LimitsTabFragment();
        limitsTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_limits));
        CalendarTabFragment calendarTabFragment = new CalendarTabFragment();
        calendarTabFragment.setTabTitle(getResources().getString(R.string.explorer_tab_title_calendar));
        tabAdapter.addFragment(limitsTabFragment);
        tabAdapter.addFragment(calendarTabFragment);
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

        mRadiusDisplay.setText(String.format(getString(R.string.alarm_radius_string),
                getRadiusForProgress(mRadiusSeekbar.getProgress())));
        mRadiusSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mRadiusDisplay.setText(String.format(getString(R.string.alarm_radius_string),
                        getRadiusForProgress(progress)));
                mMarkerRadius.setRadius(getRadiusForProgress(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int radius = getRadiusForProgress(seekBar.getProgress());
                //WatchZoneRepository.getInstance(this).updateWatchZone(mCurrentWatchZoneUid, )
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

                }
            }
        });
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
        public LimitsTabFragment() {

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
            return inflater.inflate(R.layout.content_posted_limits, container, false);

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
            Log.e("Joey", "onCreateViewCalled1");
            return inflater.inflate(R.layout.content_posted_limits, container, false);

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

        if (mCurrentWatchZoneUid != 0L) {
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

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                setCurrentZone(null,
                        latLng,
                        false);
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

    private void setCurrentZone(String address, LatLng latLng, boolean animateCamera) {
        if (mCurrentWatchZoneUid != 0L) {
            WatchZoneRepository.getInstance(this).deleteWatchZone(mCurrentWatchZoneUid);
        }

        setAlarmLocation(latLng);
        if (animateCamera) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.5f));
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

        mCurrentWatchZoneUid = WatchZoneRepository.getInstance(this).createWatchZone(address,
                latLng.latitude, latLng.longitude,
                getRadiusForProgress(mRadiusSeekbar.getProgress()));
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
                    long uid = WatchZoneRepository.getInstance(WatchZoneExplorerActivity.this).createWatchZone(
                            mLabel, mLatLng.latitude, mLatLng.longitude,
                            getRadiusForProgress(mRadiusSeekbar.getProgress()));
                    if (uid != 0) {
                        Log.e("Joey", "created?");
                    }
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

        mLatLng = location;

        mMarkerRadius = mMap.addCircle(new CircleOptions()
                .center(mLatLng)
                .radius(getRadiusForProgress(mRadiusSeekbar.getProgress()))
                .strokeColor(getResources().getColor(R.color.app_primary))
                .fillColor(getResources().getColor(R.color.map_radius_fill)));

        mSlidingPanelLayout.getAnchorPoint();
        mSlidingPanelLayout.getPanelState();

        /*if (mScanTask != null) {
            mScanTask.cancel(false);
        }

        mAdapter.clearLimits();

        mScanTask = new ScanTask(mLatLng,  mRadiusSeekbar.getProgress());
        mScanTask.execute();*/
    }

    private int getRadiusForProgress(int progress) {
        double percentage = (double) progress / 100;
        return 30 + (int)(percentage * 270.0);
    }
}
