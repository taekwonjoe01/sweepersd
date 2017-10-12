package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZoneRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.lang3.text.WordUtils;

public class WatchZoneDetailsActivity extends AppCompatActivity implements
        OnMapReadyCallback {
    private static final String TAG = WatchZoneDetailsActivity.class.getSimpleName();
    public static final String KEY_WATCHZONE_ID = "KEY_WATCHZONE_ID";

    private Long mWatchZoneId;
    private WatchZone mBriefWatchZone;
    private WatchZone mWatchZone;

    private RecyclerView mRecyclerView;
    //private LimitViewAdapter2 mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private WatchZoneViewItemDecoration mLimitViewItemDecoration;

    private LinearLayout mLoadingGroup;

    private GoogleMap mMap;

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.limit_recycler_view);
        mLoadingGroup = (LinearLayout) findViewById(R.id.limit_loading_group);
        mLayoutManager = new LinearLayoutManager(this, LinearLayout.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        int itemMargin = getResources().getDimensionPixelSize(R.dimen.limit_view_item_space);
        mLimitViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

        mRecyclerView.addItemDecoration(mLimitViewItemDecoration);

        RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        setTitle(WordUtils.capitalize("Loading Zone"));
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

    private void setAdapter() {
        //mAdapter = new LimitViewAdapter2(this, mWatchZone.getSweepingAddresses());
        //mRecyclerView.setAdapter(mAdapter);
    }

    private void invalidateUi() {
        if (mWatchZoneModel != null) {
            WatchZone watchZone = mWatchZoneModel.getWatchZone();
            if (watchZone != null) {
                setTitle(WordUtils.capitalize(watchZone.getLabel()));
            }
        }
    }

    /*private WatchZoneManager.WatchZoneChangeListener mWatchZoneChangeListener =
            new WatchZoneManager.WatchZoneChangeListener() {
        @Override
        public void onWatchZoneUpdated(long createdTimestamp) {
            Log.d(TAG, "onWatchZoneUpdated " + createdTimestamp);

            for (WatchZonePresenter p : mWatchZonePresenters) {
                if (p.watchZoneTimestamp == createdTimestamp) {
                    if (mWatchZoneManager.getUpdatingWatchZones().contains(createdTimestamp)) {
                        UpdatingPresenter presenter = new UpdatingPresenter(
                                p.position, createdTimestamp);
                        mWatchZonePresenters.remove(p.position);
                        mWatchZonePresenters.add(p.position, presenter);
                    } else {
                        NonUpdatingWatchZonePresenter presenter =
                                new NonUpdatingWatchZonePresenter(p.position, createdTimestamp);
                        mWatchZonePresenters.remove(p.position);
                        mWatchZonePresenters.add(p.position, presenter);
                    }
                    notifyItemChanged(p.position);
                }
            }
        }

        @Override
        public void onWatchZoneCreated(long createdTimestamp) {
            Log.d(TAG, "onWatchZoneCreated " + createdTimestamp);
            UpdatingPresenter presenter = new UpdatingPresenter(
                    mWatchZonePresenters.size(), createdTimestamp);
            mWatchZonePresenters.add(presenter);

            notifyDataSetChanged();
        }

        @Override
        public void onWatchZoneDeleted(long createdTimestamp) {
            Log.d(TAG, "onWatchZoneDeleted " + createdTimestamp);
            int position = -1;
            for (int i = 0; i < mWatchZonePresenters.size(); i++) {
                WatchZonePresenter p = mWatchZonePresenters.get(i);
                Log.d(TAG, "timestamp: " + p.watchZoneTimestamp);
                if (p.watchZoneTimestamp == createdTimestamp) {
                    position = i;
                    Log.d(TAG, "position being set to: " + i);
                }
            }
            if (position > -1) {
                Log.d(TAG, "removing item at position: " + position);
                mWatchZonePresenters.remove(position);
                for (int i = position; i < mWatchZonePresenters.size(); i++) {
                    mWatchZonePresenters.get(i).position--;
                }
                notifyItemRemoved(position);
            }
        }
    };*/

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
