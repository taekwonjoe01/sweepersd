package com.example.joseph.sweepersd.watchzone;

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
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.utils.LongPreferenceLiveData;
import com.example.joseph.sweepersd.utils.Preferences;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneBaseObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelUpdater;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;
import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.lang3.text.WordUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class UserZonesViewAdapter extends RecyclerView.Adapter<UserZonesViewAdapter.ViewHolder> {
    private static final String TAG = UserZonesViewAdapter.class.getSimpleName();

    private final AppCompatActivity mActivity;

    private final WatchZoneModelUpdater mWatchZoneModelUpdater;

    private List<WatchZoneModel> mCurrentList;
    private List<Long> mCurrentUpdatingList;

    private Long mExplorerUid = 0L;

    public UserZonesViewAdapter(AppCompatActivity activity) {
        mActivity = activity;

        WatchZoneModelRepository.getInstance(mActivity).observe(mActivity, new WatchZoneModelsObserver(
                new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
            @Override
            public void onModelsChanged(Map<Long, WatchZoneModel> data,
                                        WatchZoneBaseObserver.ChangeSet changeSet) {
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
                        return mCurrentList.get(oldItemPosition).getWatchZoneUid() ==
                                sortedModels.get(newItemPosition).getWatchZoneUid();
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return !mCurrentList.get(oldItemPosition).isChanged(
                                sortedModels.get(newItemPosition));
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
                    mExplorerUid = explorerUid;
                }
            }
        });

        mWatchZoneModelUpdater = WatchZoneModelUpdater.getInstance(mActivity);
        mWatchZoneModelUpdater.observe(mActivity, new Observer<Map<Long, Integer>>() {
            @Override
            public void onChanged(@Nullable Map<Long, Integer> longIntegerMap) {
                if (mCurrentList == null) {
                    return;
                }
                if (mCurrentUpdatingList == null) {
                    mCurrentUpdatingList = new ArrayList<>();

                }
                final List<Long> updatingList = new ArrayList<>(longIntegerMap.keySet());
                DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return mCurrentUpdatingList.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return updatingList.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return mCurrentUpdatingList.get(oldItemPosition) ==
                                updatingList.get(newItemPosition);
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return mCurrentUpdatingList.get(oldItemPosition) ==
                                updatingList.get(newItemPosition);
                    }
                });
                result.dispatchUpdatesTo(new ListUpdateCallback() {
                    @Override
                    public void onInserted(int position, int count) {
                        Long uid = updatingList.get(position);
                        int foundIndex = -1;
                        int index = 0;
                        for (WatchZoneModel model : mCurrentList) {
                            if (model.getWatchZoneUid() == uid) {
                                foundIndex = index;
                                break;
                            }
                            index++;
                        }
                        if (foundIndex >= 0) {
                            notifyItemChanged(index);
                        }
                    }

                    @Override
                    public void onRemoved(int position, int count) {
                        Long uid = mCurrentUpdatingList.get(position);
                        int foundIndex = -1;
                        int index = 0;
                        for (WatchZoneModel model : mCurrentList) {
                            if (model.getWatchZoneUid() == uid) {
                                foundIndex = index;
                                break;
                            }
                            index++;
                        }
                        if (foundIndex >= 0) {
                            notifyItemChanged(index);
                        }
                    }

                    @Override
                    public void onMoved(int fromPosition, int toPosition) {
                        // Doesn't matter
                    }

                    @Override
                    public void onChanged(int position, int count, Object payload) {
                        // Doesn't matter
                    }
                });
                mCurrentUpdatingList = updatingList;
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
        WatchZoneModel.Status modelStatus = WatchZoneModel.Status.LOADING;
        if (model != null) {
            modelStatus = model.getStatus();
        }
        String label = modelStatus.toString();
        if (modelStatus == WatchZoneModel.Status.INVALID_NO_WATCH_ZONE) {
            // TODO - This watch Zone doesn't exist and this should not happen!
        } else if (model != null) {
            WatchZone watchZone = model.getWatchZone();
            if (watchZone != null) {
                label = watchZone.getLabel() + " - " + label;

                holder.mOnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mActivity, WatchZoneDetailsActivity.class);
                        Bundle b = new Bundle();
                        b.putLong(WatchZoneDetailsActivity.KEY_WATCHZONE_ID, model.getWatchZoneUid());
                        intent.putExtras(b);
                        mActivity.startActivity(intent);
                    }
                };

                if (modelStatus == WatchZoneModel.Status.LOADING) {
                    holder.mDetailsGroup.setVisibility(View.GONE);
                    holder.mLoadingGroup.setVisibility(View.VISIBLE);
                    holder.mUpdatingProgress.setVisibility(View.GONE);
                } else {
                    // Any status could be being updated by the WatchZoneModelUpdater...
                    Map<Long, Integer> progressMap = mWatchZoneModelUpdater.getValue();
                    Integer progress = null;
                    if (progressMap != null) {
                        progress = progressMap.get(model.getWatchZone().getUid());
                    }

                    if (progress != null) {
                        holder.mDetailsGroup.setVisibility(View.GONE);
                        holder.mLoadingGroup.setVisibility(View.VISIBLE);
                        holder.mUpdatingProgress.setVisibility(View.VISIBLE);
                        holder.mUpdatingProgress.setProgress(progress);
                    } else {
                        holder.mUpdatingProgress.setProgress(0);
                        holder.mLoadingGroup.setVisibility(View.INVISIBLE);

                        if (model.getStatus() == WatchZoneModel.Status.VALID) {
                            holder.mDetailsGroup.setVisibility(View.VISIBLE);

                            // Get all LimitSchedules to determine sweeping dates
                            List<LimitSchedule> allLimitSchedules = new ArrayList<>();
                            for (Long uniqueLimitUid : model.getWatchZoneLimitModelUids()) {
                                allLimitSchedules.addAll(new ArrayList<>(
                                        model.getWatchZoneLimitModel(uniqueLimitUid)
                                                .getLimitSchedulesModel().getScheduleMap().values()));
                            }

                            long nextSweepingTime = WatchZoneUtils.getNextSweepingStartTime(allLimitSchedules);
                            String dateString = mActivity.getResources().getString(R.string.watch_zone_no_sweeping);
                            if (nextSweepingTime != 0) {
                                SimpleDateFormat format = new SimpleDateFormat("EEE, MMM dd");
                                dateString = format.format(nextSweepingTime);
                            }
                            holder.mAlarmNextSweeping.setText(dateString);
                        }
                    }
                }
            }
        }
        holder.mWatchZoneLabel.setText(WordUtils.capitalize(label));

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
        public ProgressBar mUpdatingProgress;
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
            mUpdatingProgress = (ProgressBar) v.findViewById(R.id.progress_updating);
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
