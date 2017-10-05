package com.example.joseph.sweepersd.revision3;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.example.joseph.sweepersd.revision3.limit.LimitSchedule;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneModel;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneModelRepository;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneModelUpdater;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZonePoint;
import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneUtils;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class UserZonesViewAdapter extends RecyclerView.Adapter<UserZonesViewAdapter.ViewHolder> {
    private static final String TAG = UserZonesViewAdapter.class.getSimpleName();

    private final AppCompatActivity mActivity;

    private final WatchZoneModelUpdater mWatchZoneModelUpdater;

    private List<WatchZoneModel> mCurrentList;

    public UserZonesViewAdapter(AppCompatActivity activity) {
        mActivity = activity;

        WatchZoneModelRepository.getInstance(mActivity).observe(mActivity, new Observer<WatchZoneModelRepository>() {
            @Override
            public void onChanged(@Nullable final WatchZoneModelRepository repository) {
                if (repository != null) {
                    if (mCurrentList == null) {
                        mCurrentList = repository.getValue().getWatchZoneModels();
                        notifyItemRangeInserted(0, mCurrentList.size());
                    } else {
                        final List<WatchZoneModel> models = repository.getValue().getWatchZoneModels();
                        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                            @Override
                            public int getOldListSize() {
                                return mCurrentList.size();
                            }

                            @Override
                            public int getNewListSize() {
                                return models.size();
                            }

                            @Override
                            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                                return mCurrentList.get(oldItemPosition).getWatchZoneUid() ==
                                        models.get(newItemPosition).getWatchZoneUid();
                            }

                            @Override
                            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                                return !mCurrentList.get(oldItemPosition).isChanged(
                                        models.get(newItemPosition));
                            }
                        });
                        mCurrentList = repository.getValue().getWatchZoneModels();

                        result.dispatchUpdatesTo(UserZonesViewAdapter.this);
                    }

                    /*for (WatchZoneModel model : mCurrentList) {
                        for (WatchZonePoint p : model.getWatchZonePoints()) {
                            if (p.getLimitId() > 0L && !mCachedSchedules.containsKey(p.getLimitId())) {
                                Log.e("Joey", "Adding new cache for id " + p.getLimitId());
                                LiveData<List<LimitSchedule>> schedules =
                                        LimitRepository.getInstance(mActivity)
                                                .getLimitSchedulesLiveData(p.getLimitId());
                                schedules.observe(mActivity, new Observer<List<LimitSchedule>>() {
                                    @Override
                                    public void onChanged(@Nullable List<LimitSchedule> limitSchedules) {
                                        Log.e("Joey", "onChanged");
                                        if (limitSchedules != null && !limitSchedules.isEmpty()) {
                                            LimitSchedule first = limitSchedules.get(0);
                                            Long limitUid = first.getLimitId();

                                            List<Integer> indexesThatCare = new ArrayList<>();
                                            for (int i = 0; i < mCurrentList.size(); i++) {
                                                WatchZoneModel m = mCurrentList.get(i);

                                                boolean cares = false;
                                                for (WatchZonePoint p : m.getWatchZonePoints()) {
                                                    if (p.getLimitId() == limitUid) {
                                                        cares = true;
                                                    }
                                                }

                                                if (cares) {
                                                    indexesThatCare.add(i);
                                                }
                                            }

                                            for (Integer index : indexesThatCare) {
                                                notifyItemChanged(index);
                                            }
                                        }
                                    }
                                });
                                mCachedSchedules.put(p.getLimitId(), schedules);
                            }
                        }
                    }*/
                }
            }
        });

        mWatchZoneModelUpdater = WatchZoneModelUpdater.getInstance(mActivity);
        mWatchZoneModelUpdater.observe(mActivity, new Observer<Map<Long, Integer>>() {
            @Override
            public void onChanged(@Nullable Map<Long, Integer> longIntegerMap) {
                for (Long uid : longIntegerMap.keySet()) {
                    int index = mCurrentList.indexOf(uid);
                    if (index >= 0) {
                        notifyItemChanged(index);
                    }
                }
            }
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_watch_zone_list_item, parent, false);

        ViewHolder vh = new ViewHolder(mActivity, v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final WatchZoneModel model = mCurrentList.get(position);
        holder.mWatchZoneLabel.setText(model.getWatchZone().getLabel());
        holder.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
                Intent intent = new Intent(mActivity, UserZoneDetailsActivity.class);
                Bundle b = new Bundle();
                b.putLong(UserZoneDetailsActivity.KEY_WATCHZONE_ID, model.getWatchZone().getUid());
                intent.putExtras(b);
                mActivity.startActivity(intent);
            }
        };

        Map<Long, Integer> progressMap = mWatchZoneModelUpdater.getValue();
        if (progressMap != null) {
            Integer progress = progressMap.get(model.getWatchZone().getUid());
            if (progress != null) {
                holder.mDetailsGroup.setVisibility(View.GONE);
                holder.mLoadingGroup.setVisibility(View.VISIBLE);
                holder.mLoadingProgress.setProgress(progress);
                return;
            }
        }
        holder.mDetailsGroup.setVisibility(View.VISIBLE);
        holder.mLoadingGroup.setVisibility(View.INVISIBLE);
        List<WatchZonePoint> points = model.getWatchZonePoints();

        if (model.getStatus() == WatchZoneModel.WatchZoneStatus.VALID) {
            List<LimitSchedule> allLimitsSchedules = new ArrayList<>();
            boolean fullyLoaded = true;
            List<Long> uniqueIds = WatchZoneUtils.getUniqueLimitIds(model.getWatchZonePoints());
            for (Long uid : uniqueIds) {
                if (uid > 0L) {
                    List<LimitSchedule> schedulesForId = mCachedSchedules.get(uid).getValue();
                    if (schedulesForId == null) {
                        fullyLoaded = false;
                        break;
                    }
                    allLimitsSchedules.addAll(schedulesForId);
                }
            }
            if (fullyLoaded) {
                long nextSweepingTime = WatchZoneUtils.getNextSweepingTime(allLimitsSchedules);
                if (nextSweepingTime >= 0) {
                    String dateString = mActivity.getResources().getString(R.string.watch_zone_no_sweeping);
                    if (nextSweepingTime != 0) {
                        SimpleDateFormat format = new SimpleDateFormat("EEE, MMM dd");
                        dateString = format.format(nextSweepingTime);
                    }
                    holder.mAlarmNextSweeping.setText(dateString);
                } else {
                    holder.mAlarmNextSweeping.setText("Unknown");
                }
            } else {
                holder.mAlarmNextSweeping.setText("Checking...");
            }
        }
    }

    @Override
    public int getItemCount() {
        return mCurrentList == null ? 0 : mCurrentList.size();
    }

    public void createAlarm(String label, LatLng location, int radius) {
        // TODO
        //mWatchZoneManager.createWatchZone(label, location, radius);
    }



    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, View.OnLongClickListener {
        private final Context mContext;

        public TextView mWatchZoneLabel;
        public Switch mAlarmEnabled;
        public TextView mAlarmNextSweeping;
        public LinearLayout mLoadingGroup;
        public LinearLayout mDetailsGroup;
        public ProgressBar mLoadingProgress;
        private FrameLayout mViewLayout;
        public View.OnLongClickListener mLongClickListener;
        public View.OnClickListener mOnClickListener;

        //public RecyclerView mLimitRecyclerView;
        //private RecyclerView.LayoutManager mLayoutManager;
        //private WatchZoneViewItemDecoration mLimitViewItemDecoration;

        public ViewHolder(Context context, View v) {
            super(v);
            mContext = context;
            mWatchZoneLabel = (TextView) v.findViewById(R.id.textview_watchzone_label);
            mAlarmEnabled = (Switch) v.findViewById(R.id.switch_enable_alarm);
            mAlarmNextSweeping = (TextView) v.findViewById(R.id.textview_next_sweeping);
            mLoadingGroup = (LinearLayout) v.findViewById(R.id.watchzone_loading_group);
            mDetailsGroup = (LinearLayout) v.findViewById(R.id.watchzone_details_group);
            mLoadingProgress = (ProgressBar) v.findViewById(R.id.progress_loading);
            mViewLayout = (FrameLayout) v.findViewById(R.id.list_item_layout);

            mViewLayout.setOnClickListener(this);
            mViewLayout.setOnLongClickListener(this);

            //mLayoutManager = new LinearLayoutManager(mActivity, LinearLayout.VERTICAL, false);
            //mLimitRecyclerView.setLayoutManager(mLayoutManager);

            //int itemMargin =
                   // mActivity.getResources().getDimensionPixelSize(R.dimen.limit_view_item_space);
            //mLimitViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

            //mLimitRecyclerView.addItemDecoration(mLimitViewItemDecoration);

            /*RecyclerView.ItemAnimator animator = mLimitRecyclerView.getItemAnimator();
            if (animator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
            }*/
        }

        @Override
        public void onClick(View v) {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(v);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mLongClickListener != null) {
                return mLongClickListener.onLongClick(v);
            }
            return false;
        }
    }
}
