package com.example.joseph.sweepersd.watchzone;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.utils.ChangeSet;
import com.example.joseph.sweepersd.utils.LongPreferenceLiveData;
import com.example.joseph.sweepersd.utils.Preferences;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class UserZonesViewAdapter extends RecyclerView.Adapter<UserZonesViewAdapter.ViewHolder> {
    private static final String TAG = UserZonesViewAdapter.class.getSimpleName();

    private final AppCompatActivity mActivity;

    private List<WatchZoneModel> mCurrentList;
    private Map<Long, Integer> mUpdatingProgressMap;

    public UserZonesViewAdapter(AppCompatActivity activity) {
        mActivity = activity;

        WatchZoneModelRepository.getInstance(mActivity).getCachedWatchZoneModelsLiveData().observe(mActivity, new WatchZoneModelsObserver(true,
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
                DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return mCurrentList.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return sortedModels.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return mCurrentList.get(oldItemPosition).watchZone.getUid() ==
                                sortedModels.get(newItemPosition).watchZone.getUid();
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return !mCurrentList.get(oldItemPosition).isChanged(sortedModels.get(newItemPosition));
                    }
                }, false);
                mCurrentList = sortedModels;

                result.dispatchUpdatesTo(UserZonesViewAdapter.this);
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

                mCurrentList = sortedModels;
                notifyDataSetChanged();
            }

            @Override
            public void onDataInvalid() {
                mCurrentList.clear();
                notifyDataSetChanged();
            }
        }));

        LongPreferenceLiveData explorerPreference = new LongPreferenceLiveData(mActivity, Preferences.PREFERENCE_WATCH_ZONE_EXPLORER_UID);
        explorerPreference.observe(mActivity, new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long explorerUid) {
                if (explorerUid != null) {
                    //mExplorerUid = explorerUid;
                }
            }
        });
    }

    public void setWatchZoneProgress(Map<Long, Integer> watchZoneProgress) {
        if (mCurrentList != null && mUpdatingProgressMap != null) {
            List<Long> removedWatchZones = new ArrayList<>(mUpdatingProgressMap.keySet());
            for (Long uid : watchZoneProgress.keySet()) {
                removedWatchZones.remove(uid);
            }
            for (WatchZoneModel model : mCurrentList) {
                if (removedWatchZones.contains(model.watchZone.getUid())) {
                    int index = mCurrentList.indexOf(model);
                    notifyItemChanged(index);
                }
            }
        }
        mUpdatingProgressMap = watchZoneProgress;
        if (mCurrentList == null) {
            return;
        }
        for (WatchZoneModel model : mCurrentList) {
            if (mUpdatingProgressMap.containsKey(model.watchZone.getUid())) {
                int index = mCurrentList.indexOf(model);
                notifyItemChanged(index);
            }
        }
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
        holder.mViewLayout.setBackground(
                mActivity.getResources().getDrawable(R.drawable.apptheme_background));
        final WatchZoneModel model = mCurrentList.get(position);
        final WatchZone watchZone = model.watchZone;
        String label = watchZone.getLabel();

        holder.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, WatchZoneDetailsActivity.class);
                Bundle b = new Bundle();
                b.putLong(WatchZoneDetailsActivity.KEY_WATCHZONE_ID, watchZone.getUid());
                intent.putExtras(b);
                mActivity.startActivity(intent);
            }
        };
        // Any status could be being updated by the WatchZoneModelUpdater...
        Integer progress = null;
        if (mUpdatingProgressMap != null) {
            progress = mUpdatingProgressMap.get(watchZone.getUid());
        }

        if (progress != null) {
            holder.mDetailsGroup.setVisibility(View.GONE);
            holder.mLoadingGroup.setVisibility(View.VISIBLE);
            holder.mUpdatingProgress.setVisibility(View.VISIBLE);
            holder.mUpdatingProgress.setProgress(progress);
        } else {
            holder.mUpdatingProgress.setProgress(0);
            holder.mLoadingGroup.setVisibility(View.GONE);

            holder.mDetailsGroup.setVisibility(View.VISIBLE);

            // Get all LimitSchedules to determine sweeping dates
            List<LimitSchedule> allLimitSchedules = new ArrayList<>();
            for (Long uniqueLimitUid : model.getUniqueLimitModels().keySet()) {
                allLimitSchedules.addAll(new ArrayList<>(
                        model.getUniqueLimitModels().get(uniqueLimitUid).schedules));
            }

            long nextSweepingTime = WatchZoneUtils.getNextSweepingStartTime(allLimitSchedules);
            String dateString = mActivity.getResources().getString(R.string.watch_zone_no_sweeping);
            if (nextSweepingTime != 0) {
                if (nextSweepingTime < System.currentTimeMillis()) {
                    holder.mViewLayout.setBackground(
                            mActivity.getResources().getDrawable(R.drawable.background_userzones_now));
                    dateString = "Street sweeping is happening now.";
                } else {
                    long timeLeft = nextSweepingTime - System.currentTimeMillis();
                    if (timeLeft < 1000L * 60L * 60L * 24L) {
                        holder.mViewLayout.setBackground(
                                mActivity.getResources().getDrawable(R.drawable.background_userzones_upcoming));
                        Calendar sweeping = Calendar.getInstance();
                        sweeping.setTime(new Date(nextSweepingTime));
                        Calendar now = Calendar.getInstance();
                        if (sweeping.get(Calendar.DATE) != now.get(Calendar.DATE)) {
                            dateString = "Next sweeping will occur tomorrow at " +
                                    new SimpleDateFormat("K:mma").format(new Date(nextSweepingTime));
                        } else {
                            dateString = "Next sweeping will occur today at " +
                                    new SimpleDateFormat("K:mma").format(new Date(nextSweepingTime));
                        }
                    } else {
                        dateString = "Next sweeping will occur on " +
                                new SimpleDateFormat("EEE, MMM dd 'at' K:mma").format(new Date(nextSweepingTime));
                    }
                }
            }
            holder.mAlarmNextSweeping.setText(dateString);
        }
        holder.mWatchZoneLabel.setText(WordUtils.capitalize(label));

    }

    @Override
    public int getItemCount() {
        return mCurrentList == null ? 0 : mCurrentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, View.OnLongClickListener {
        private final Context mContext;

        public TextView mWatchZoneLabel;
        public TextView mAlarmNextSweeping;
        public LinearLayout mLoadingGroup;
        public LinearLayout mDetailsGroup;
        public ProgressBar mUpdatingProgress;
        public FrameLayout mViewLayout;
        public View.OnLongClickListener mLongClickListener;
        public View.OnClickListener mOnClickListener;

        public ViewHolder(Context context, View v) {
            super(v);
            mContext = context;
            mWatchZoneLabel = (TextView) v.findViewById(R.id.textview_watchzone_label);
            mAlarmNextSweeping = (TextView) v.findViewById(R.id.textview_next_sweeping);
            mLoadingGroup = (LinearLayout) v.findViewById(R.id.watchzone_loading_group);
            mDetailsGroup = (LinearLayout) v.findViewById(R.id.watchzone_details_group);
            mUpdatingProgress = (ProgressBar) v.findViewById(R.id.progress_updating);
            mViewLayout = (FrameLayout) v.findViewById(R.id.list_item_layout);

            mViewLayout.setOnClickListener(this);
            mViewLayout.setOnLongClickListener(this);
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
