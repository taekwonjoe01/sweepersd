package com.example.joseph.sweepersd.archived.presentation.manualalarms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.example.joseph.sweepersd.archived.model.AddressValidatorManager;
import com.example.joseph.sweepersd.watchzone.WatchZoneViewItemDecoration;
import com.google.android.gms.maps.model.LatLng;
import com.hutchins.tbd.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

public class WatchZoneViewActivity extends AppCompatActivity implements
        AddressValidatorManager.ValidatorProgressListener{
    public static final String ALARM_LOCATION_EXTRA = "ALARM_LOCATION_EXTRA";
    private static final int CREATE_ALARM_CODE = 1;

    private RecyclerView mRecyclerView;
    private WatchZoneViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private WatchZoneViewItemDecoration mWatchZoneViewItemDecoration;

    private Menu mOptionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_zone_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton fab = (ImageButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(WatchZoneViewActivity.this, CreateWatchZoneActivity.class), CREATE_ALARM_CODE);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.alarm_recycler_view);
        //mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        int itemMargin = getResources().getDimensionPixelSize(R.dimen.alarm_view_item_space);
        mWatchZoneViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

        mRecyclerView.addItemDecoration(mWatchZoneViewItemDecoration);

        mAdapter = new WatchZoneViewAdapter(this);

        setTitle(getResources().getString(R.string.title_user_zone_activity));

        RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CREATE_ALARM_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    String label = data.getStringExtra(CreateWatchZoneActivity.LABEL_KEY);
                    LatLng location = data.getParcelableExtra(CreateWatchZoneActivity.LOCATION_KEY);
                    int radius = data.getIntExtra(CreateWatchZoneActivity.RADIUS_KEY, 0);

                    mAdapter.createAlarm(label, location, radius);
                }
                mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                break;
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
    }

    @Override
    protected void onPause() {
        super.onPause();

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
