package com.example.joseph.sweepersd.watchzone;

import android.arch.lifecycle.LiveData;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.archived.model.AddressValidatorManager;
import com.example.joseph.sweepersd.utils.ChangeSet;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WatchZoneListActivity extends WatchZoneBaseActivity implements
        AddressValidatorManager.ValidatorProgressListener{
    public static final String ALARM_LOCATION_EXTRA = "ALARM_LOCATION_EXTRA";
    private static final int CREATE_ALARM_CODE = 1;

    private RecyclerView mRecyclerView;
    private UserZonesViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private WatchZoneViewItemDecoration mWatchZoneViewItemDecoration;

    private LiveData<List<WatchZoneModel>> mWatchZoneModels;

    private Menu mOptionsMenu;

    private FrameLayout mOverlay;
    private TextView mOverlayText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_zone_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(WatchZoneListActivity.this, "Add", Toast.LENGTH_SHORT).show();
                startActivityForResult(
                        new Intent(WatchZoneListActivity.this, WatchZoneExplorerActivity.class),
                        CREATE_ALARM_CODE);
            }
        });

        mOverlay = (FrameLayout) findViewById(R.id.layout_overlay);
        mOverlayText = findViewById(R.id.textview_overlay);

        mRecyclerView = (RecyclerView) findViewById(R.id.alarm_recycler_view);
        //mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        int itemMargin = getResources().getDimensionPixelSize(R.dimen.alarm_view_item_space);
        mWatchZoneViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

        mRecyclerView.addItemDecoration(mWatchZoneViewItemDecoration);

        mAdapter = new UserZonesViewAdapter(this);

        setTitle(getResources().getString(R.string.title_user_zone_activity));

        RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
    }

    @Override
    public void onWatchZoneUpdateProgress(Map<Long, Integer> progressMap) {
        if (mAdapter != null) {
            mAdapter.setWatchZoneProgress(progressMap);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        AddressValidatorManager.getInstance(this).addListener(this);
        setProgress(AddressValidatorManager.getInstance(this).getValidationProgress());

        mWatchZoneModels = WatchZoneModelRepository.getInstance(this).getWatchZoneModelsLiveData();
        mWatchZoneModels.observe(this, new WatchZoneModelsObserver(true,
                new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
            @Override
            public void onModelsChanged(Map<Long, WatchZoneModel> data,
                                        ChangeSet changeSet) {
                // This is only capable of detecting insertions or deletions.
                // Changes must be detected directly.
                List<Long> modelUids = new ArrayList<>(data.keySet());
                Collections.sort(modelUids);
                final List<WatchZoneModel> sortedModels = new ArrayList<>();
                for (Long uid : modelUids) {
                    WatchZoneModel model = data.get(uid);
                    sortedModels.add(model);
                }
                mAdapter.setWatchZoneModels(sortedModels);

                if (sortedModels.isEmpty()) {
                    mOverlay.setVisibility(mRecyclerView.VISIBLE);
                    mOverlayText.setText(R.string.watch_zone_list_overlay_empty);
                } else {
                    mOverlay.setVisibility(mRecyclerView.GONE);
                }
            }

            @Override
            public void onDataLoaded(Map<Long, WatchZoneModel> data) {
                List<Long> modelUids = new ArrayList<>(data.keySet());
                Collections.sort(modelUids);
                List<WatchZoneModel> sortedModels = new ArrayList<>();
                for (Long uid : modelUids) {
                    WatchZoneModel model = data.get(uid);
                    sortedModels.add(model);
                }

                mAdapter.setWatchZoneModels(sortedModels);

                if (sortedModels.isEmpty()) {
                    mOverlay.setVisibility(mRecyclerView.VISIBLE);
                    mOverlayText.setText(R.string.watch_zone_list_overlay_empty);
                } else {
                    mOverlay.setVisibility(mRecyclerView.GONE);
                }
            }

            @Override
            public void onDataInvalid() {
                mAdapter.setWatchZoneModels(null);
            }
        }));

        mOverlay.setVisibility(mRecyclerView.VISIBLE);
        mOverlayText.setText(R.string.watch_zone_list_overlay_loading);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWatchZoneModels.removeObservers(this);

        AddressValidatorManager.getInstance(this).removeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity_alarm, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onValidatorProgress(int progress) {
        setValidatorProgress(progress);
    }

    @Override
    public void onValidatorComplete() {
        setValidatorProgress(AddressValidatorManager.INVALID_PROGRESS);
    }

    private void setValidatorProgress(int progress) {
        MenuItem progressItem = mOptionsMenu.findItem(R.id.validator_progress);
        if (progressItem != null) {
            String p = "";
            if (progress != AddressValidatorManager.INVALID_PROGRESS) {
                p = String.format("Updating DB: %d%%", progress);
            }
            progressItem.setTitle(p);
        }
    }

    private void setAdapter() {
        mRecyclerView.setAdapter(mAdapter);
    }
}
