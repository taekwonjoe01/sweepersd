package com.example.joseph.sweepersd.watchzone;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.joseph.sweepersd.LimitViewAdapter;
import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.TabFragment;
import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitSchedule;

import java.util.List;
import java.util.Map;

public class LimitsTabFragment extends TabFragment {

    private String mTabTitle;

    private RecyclerView mRecyclerView;
    private LimitViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private WatchZoneViewItemDecoration mLimitViewItemDecoration;

    public LimitsTabFragment() {

    }

    public void setLimitsAndSchedules(Map<Limit, List<LimitSchedule>> limitsAndSchedules) {
        if (mAdapter == null) {
            return;
        }
        mAdapter.setPostedLimits(limitsAndSchedules);
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
        mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayout.HORIZONTAL,
                false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        int itemMargin = getResources().getDimensionPixelSize(R.dimen.alarm_view_item_space);
        mLimitViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

        mRecyclerView.addItemDecoration(mLimitViewItemDecoration);

        RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        mAdapter = new LimitViewAdapter();

        mRecyclerView.setAdapter(mAdapter);

        return view;
    }
}