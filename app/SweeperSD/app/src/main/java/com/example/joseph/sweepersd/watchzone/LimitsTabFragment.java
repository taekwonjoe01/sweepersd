package com.example.joseph.sweepersd.watchzone;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.joseph.sweepersd.LimitViewAdapter;
import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.TabFragment;
import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneLimitModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneLimitsObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZonePointsObserver;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LimitsTabFragment extends TabFragment {

    private String mTabTitle;

    private RecyclerView mRecyclerView;
    private LimitViewAdapter mAdapter;
    private StaggeredGridLayoutManager mLayoutManager;
    private LimitItemDecoration mLimitViewItemDecoration;

    private Map<Long, WatchZonePresenter> mWatchZones = new HashMap<>();

    public LimitsTabFragment() {

    }

    @Override
    public void setTabTitle(String tabTitle) {
        mTabTitle = tabTitle;
    }

    @Override
    public String getTabTitle() {
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
        mLayoutManager = new StaggeredGridLayoutManager(2, LinearLayout.HORIZONTAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        int itemMargin = getResources().getDimensionPixelSize(R.dimen.alarm_view_item_space);
        mLimitViewItemDecoration = new LimitItemDecoration(itemMargin);

        mRecyclerView.addItemDecoration(mLimitViewItemDecoration);

        RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        mAdapter = new LimitViewAdapter();

        mRecyclerView.setAdapter(mAdapter);

        for (Long watchZoneUid : mWatchZones.keySet()) {
            createPresenter(watchZoneUid);
        }

        return view;
    }



    public void addWatchZone(Long watchZoneUid) {
        if (mAdapter == null) {
            mWatchZones.put(watchZoneUid, null);
        } else if (!mWatchZones.containsKey(watchZoneUid)) {
            createPresenter(watchZoneUid);
        }
    }

    public void removeWatchZone(Long watchZoneUid) {
        if (mWatchZones.containsKey(watchZoneUid)) {
            WatchZonePresenter presenter = mWatchZones.get(watchZoneUid);
            if (presenter != null) {
                for (WatchZoneLimitModel limitModel : presenter.limitsObserver.getLimitModels()) {
                    mAdapter.removeLimitModel(limitModel);
                }
                WatchZoneModelRepository.getInstance(getContext()).removeObserver(presenter.limitsObserver);
            }

            mWatchZones.remove(watchZoneUid);
        }
    }

    private void createPresenter(final Long watchZoneUid) {
        final WatchZonePresenter presenter = new WatchZonePresenter();
        presenter.limitsObserver = new WatchZoneLimitsObserver(watchZoneUid,
                new WatchZoneLimitsObserver.WatchZoneLimitsChangedCallback() {
            @Override
            public void onLimitModelAdded(int index) {
                mAdapter.addLimitModel(presenter.limitsObserver.getLimitModels().get(index));
            }

            @Override
            public void onLimitModelRemoved(int index) {
                mAdapter.removeLimitModel(presenter.limitsObserver.getLimitModels().get(index));
            }

            @Override
            public void onLimitModelUpdated(int index) {
                mAdapter.updateLimitModel(presenter.limitsObserver.getLimitModels().get(index));
            }

            @Override
            public void onDataLoaded(List<WatchZoneLimitModel> data) {
                for (WatchZoneLimitModel limitModel : presenter.limitsObserver.getLimitModels()) {
                    mAdapter.addLimitModel(limitModel);
                }
            }

            @Override
            public void onDataInvalid() {
                for (WatchZoneLimitModel limitModel : presenter.limitsObserver.getLimitModels()) {
                    mAdapter.removeLimitModel(limitModel);
                }
            }
        });
        WatchZoneModelRepository.getInstance(getContext()).observe(this, presenter.limitsObserver);
        mWatchZones.put(watchZoneUid, presenter);
    }

    private class WatchZonePresenter {
        WatchZoneLimitsObserver limitsObserver;
    }
}