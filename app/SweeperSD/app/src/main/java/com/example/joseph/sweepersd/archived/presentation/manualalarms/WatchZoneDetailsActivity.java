package com.example.joseph.sweepersd.archived.presentation.manualalarms;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.archived.model.watchzone.WatchZone;
import com.example.joseph.sweepersd.archived.model.watchzone.WatchZoneManager;
import com.example.joseph.sweepersd.watchzone.WatchZoneViewItemDecoration;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;

import org.apache.commons.lang3.text.WordUtils;

public class WatchZoneDetailsActivity extends AppCompatActivity implements
        OnMapReadyCallback {
    private static final String TAG = WatchZoneDetailsActivity.class.getSimpleName();
    public static final String KEY_WATCHZONE_ID = "KEY_WATCHZONE_ID";

    private Long mWatchZoneId;
    private WatchZone mBriefWatchZone;
    private WatchZone mWatchZone;

    private WatchZoneManager mWatchZoneManager;
    private RecyclerView mRecyclerView;
    private LimitViewAdapter2 mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private WatchZoneViewItemDecoration mLimitViewItemDecoration;

    private LinearLayout mLoadingGroup;

    private GoogleMap mMap;

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
        mWatchZoneManager = new WatchZoneManager(this);
        mWatchZoneManager.addWatchZoneChangeListener(mWatchZoneChangeListener);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //mRecyclerView = (RecyclerView) findViewById(R.id.limit_recycler_view);
        //mLoadingGroup = (LinearLayout) findViewById(R.id.limit_loading_group);
        mLayoutManager = new LinearLayoutManager(this, LinearLayout.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        int itemMargin = getResources().getDimensionPixelSize(R.dimen.limit_view_item_space);
        mLimitViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

        mRecyclerView.addItemDecoration(mLimitViewItemDecoration);

        RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        mBriefWatchZone = mWatchZoneManager.getWatchZoneBrief(mWatchZoneId);

        setTitle(WordUtils.capitalize(mBriefWatchZone.getLabel()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        new LoadWatchZoneTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                mWatchZoneManager.deleteWatchZone(mWatchZoneId);
                finish();
                return true;
        }
        return false;
    }

    private class LoadWatchZoneTask extends AsyncTask<Void, Long, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            mWatchZone = mWatchZoneManager.getWatchZoneComplete(mWatchZoneId);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            setAdapter();

            mLoadingGroup.setVisibility(View.GONE);
        }
    }

    private void setAdapter() {
        mAdapter = new LimitViewAdapter2(this, mWatchZone.getSweepingAddresses());
        mRecyclerView.setAdapter(mAdapter);
    }

    private WatchZoneManager.WatchZoneChangeListener mWatchZoneChangeListener =
            new WatchZoneManager.WatchZoneChangeListener() {
        @Override
        public void onWatchZoneUpdated(long createdTimestamp) {
            Log.d(TAG, "onWatchZoneUpdated " + createdTimestamp);

            /*for (WatchZonePresenter p : mWatchZonePresenters) {
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
            }*/
        }

        @Override
        public void onWatchZoneCreated(long createdTimestamp) {
            Log.d(TAG, "onWatchZoneCreated " + createdTimestamp);
            /*UpdatingPresenter presenter = new UpdatingPresenter(
                    mWatchZonePresenters.size(), createdTimestamp);
            mWatchZonePresenters.add(presenter);

            notifyDataSetChanged();*/
        }

        @Override
        public void onWatchZoneDeleted(long createdTimestamp) {
            Log.d(TAG, "onWatchZoneDeleted " + createdTimestamp);
            /*int position = -1;
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
            }*/
        }
    };

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
        setMap();
    }

    private void setMap() {
        mMap.clear();

        mMap.addCircle(new CircleOptions()
                .center(mBriefWatchZone.getCenter())
                .radius(mBriefWatchZone.getRadius())
                .strokeColor(getResources().getColor(R.color.app_primary))
                .fillColor(getResources().getColor(R.color.map_radius_fill)));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mBriefWatchZone.getCenter(), 15f));
    }
}
