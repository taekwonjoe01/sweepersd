package com.example.joseph.sweepersd.presentation.manualalarms;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.model.watchzone.WatchZone;
import com.example.joseph.sweepersd.model.watchzone.WatchZoneManager;

import org.apache.commons.lang3.text.WordUtils;

public class WatchZoneDetailsActivity extends AppCompatActivity {
    private static final String TAG = WatchZoneDetailsActivity.class.getSimpleName();
    public static final String KEY_WATCHZONE_ID = "KEY_WATCHZONE_ID";

    private Long mWatchZoneId;
    private WatchZone mWatchZone;

    private WatchZoneManager mWatchZoneManager;
    private RecyclerView mRecyclerView;
    private LimitViewAdapter2 mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private WatchZoneViewItemDecoration mLimitViewItemDecoration;

    private LinearLayout mLoadingGroup;

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

        setTitle("Loading...");
    }

    @Override
    protected void onResume() {
        super.onResume();

        new LoadWatchZoneTask().execute();
    }

    private class LoadWatchZoneTask extends AsyncTask<Void, Long, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            mWatchZone = mWatchZoneManager.getWatchZone(mWatchZoneId);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            setAdapter();

            setTitle(WordUtils.capitalize(mWatchZone.getLabel()));

            mLoadingGroup.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setAdapter() {
        mAdapter = new LimitViewAdapter2(this, mWatchZone);
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
}
