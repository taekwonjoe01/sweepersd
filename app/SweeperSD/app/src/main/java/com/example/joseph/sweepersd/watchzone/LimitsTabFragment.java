package com.example.joseph.sweepersd.watchzone;

import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.example.joseph.sweepersd.limit.LimitModel;
import com.example.joseph.sweepersd.utils.ChangeSet;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneLimitsObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;

import java.util.HashMap;
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

    private void createPresenter(final Long watchZoneUid) {
        final WatchZonePresenter presenter = new WatchZonePresenter();
        presenter.limitsObserver = new WatchZoneLimitsObserver(watchZoneUid,
                new WatchZoneLimitsObserver.WatchZoneLimitsChangedCallback() {
            @Override
            public void onLimitsChanged(Map<Long, LimitModel> data,
                                        ChangeSet changeSet) {
                for (Long uid : changeSet.removedUids) {
                    mAdapter.removeLimitModel(uid);
                }
                for (Long uid : changeSet.changedUids) {
                    mAdapter.updateLimitModel(presenter.limitsObserver.getLimitModels().get(uid));
                }
                for (Long uid : changeSet.addedUids) {
                    mAdapter.addLimitModel(presenter.limitsObserver.getLimitModels().get(uid));
                }
            }

            @Override
            public void onDataLoaded(Map<Long, LimitModel> data) {
                for (Long uid : presenter.limitsObserver.getLimitModels().keySet()) {
                    mAdapter.addLimitModel(presenter.limitsObserver.getLimitModels().get(uid));
                }
            }

            @Override
            public void onDataInvalid() {
                mAdapter.removeAll();
            }
        });
        WatchZoneModelRepository.getInstance(getContext()).getZoneModelForUid(watchZoneUid)
                .observe(this, presenter.limitsObserver);
        mWatchZones.put(watchZoneUid, presenter);
    }

    private class WatchZonePresenter {
        WatchZoneLimitsObserver limitsObserver;
    }
}