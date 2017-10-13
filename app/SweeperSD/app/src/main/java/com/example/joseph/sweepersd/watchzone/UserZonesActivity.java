package com.example.joseph.sweepersd.watchzone;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.archived.model.AddressValidatorManager;
import com.example.joseph.sweepersd.archived.presentation.manualalarms.CreateWatchZoneActivity;
import com.google.android.gms.maps.model.LatLng;

public class UserZonesActivity extends AppCompatActivity implements
        AddressValidatorManager.ValidatorProgressListener{
    public static final String ALARM_LOCATION_EXTRA = "ALARM_LOCATION_EXTRA";
    private static final int CREATE_ALARM_CODE = 1;

    private RecyclerView mRecyclerView;
    private UserZonesViewAdapter mAdapter;
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
                Toast.makeText(UserZonesActivity.this, "Add", Toast.LENGTH_SHORT).show();
                startActivityForResult(
                        new Intent(UserZonesActivity.this, WatchZoneExplorerActivity.class),
                        CREATE_ALARM_CODE);
            }
        });

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

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
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
