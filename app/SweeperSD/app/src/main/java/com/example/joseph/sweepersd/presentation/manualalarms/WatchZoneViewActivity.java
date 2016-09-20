package com.example.joseph.sweepersd.presentation.manualalarms;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.joseph.sweepersd.MapsActivity;
import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.model.AddressValidatorManager;
import com.google.android.gms.maps.model.LatLng;

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
        setContentView(R.layout.activity_alarm);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton fab = (ImageButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(WatchZoneViewActivity.this, "Add WatchZone", Toast.LENGTH_SHORT).show();
                startActivityForResult(
                        new Intent(WatchZoneViewActivity.this, MapsActivity.class), CREATE_ALARM_CODE);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.alarm_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        int itemMargin = getResources().getDimensionPixelSize(R.dimen.alarm_view_item_space);
        mWatchZoneViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

        mRecyclerView.addItemDecoration(mWatchZoneViewItemDecoration);

        mAdapter = new WatchZoneViewAdapter(this);

        setTitle("Watch Zones");

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
                Log.e("Joey", "Processing mapsactivity result");
                LatLng location = data.getParcelableExtra(MapsActivity.LOCATION_KEY);
                int radius = data.getIntExtra(MapsActivity.RADIUS_KEY, 0);

                mAdapter.createAlarm(location, radius);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setAdapter();

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
