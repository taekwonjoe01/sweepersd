package com.example.joseph.sweepersd.alarms;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.joseph.sweepersd.MapsActivity;
import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.utils.AlarmHelper;

import java.util.List;

public class AlarmViewActivity extends AppCompatActivity {
    public static final String ALARM_LOCATION_EXTRA = "ALARM_LOCATION_EXTRA";
    private static final int CREATE_ALARM_CODE = 1;

    private RecyclerView mRecyclerView;
    private AlarmViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private AlarmViewItemDecoration mAlarmViewItemDecoration;



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
                Toast.makeText(AlarmViewActivity.this, "Add Alarm", Toast.LENGTH_SHORT).show();
                startActivityForResult(
                        new Intent(AlarmViewActivity.this, MapsActivity.class), CREATE_ALARM_CODE);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        int itemMargin = getResources().getDimensionPixelSize(R.dimen.alarm_view_item_space);
        mAlarmViewItemDecoration = new AlarmViewItemDecoration(itemMargin);

        mRecyclerView.addItemDecoration(mAlarmViewItemDecoration);

        List<Alarm> alarms = AlarmHelper.loadAlarms(this);
        mAdapter = new AlarmViewAdapter(this, new AlarmModel(alarms));

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
                Location location = data.getParcelableExtra(MapsActivity.LOCATION_KEY);
                int radius = data.getIntExtra(MapsActivity.RADIUS_KEY, 0);

                mAdapter.createAlarm(location, radius);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setAdapter();
    }

    private void setAdapter() {
        mRecyclerView.setAdapter(mAdapter);
    }
}
