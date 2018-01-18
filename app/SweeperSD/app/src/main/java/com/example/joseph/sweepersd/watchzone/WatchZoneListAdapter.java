package com.example.joseph.sweepersd.watchzone;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.example.joseph.sweepersd.utils.LongPreferenceLiveData;
import com.example.joseph.sweepersd.utils.Preferences;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class WatchZoneListAdapter extends RecyclerView.Adapter<WatchZoneListAdapter.ViewHolder> {
    private static final String TAG = WatchZoneListAdapter.class.getSimpleName();

    private final AppCompatActivity mActivity;

    private List<WatchZoneModel> mCurrentList;
    private Map<Long, Integer> mUpdatingProgressMap;

    public WatchZoneListAdapter(AppCompatActivity activity) {
        mActivity = activity;

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

    public void setWatchZoneModels(final List<WatchZoneModel> sortedModels) {
        if (sortedModels == null ) {
            mCurrentList = null;
            notifyDataSetChanged();
        } else if (mCurrentList == null) {
            mCurrentList = sortedModels;
            notifyDataSetChanged();
        } else {
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

            result.dispatchUpdatesTo(WatchZoneListAdapter.this);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        ShortSummaryLayout shortSummaryLayout = new ShortSummaryLayout(parent.getContext());
        ViewHolder vh = new ViewHolder(mActivity, shortSummaryLayout);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final WatchZoneModel model = mCurrentList.get(position);
        int progress = -1;
        if (mUpdatingProgressMap != null) {
            Integer p = mUpdatingProgressMap.get(model.watchZone.getUid());
            if (p != null) {
                progress = p.intValue();
            }
        }

        holder.mShortSummaryLayout.set(model, ShortSummaryLayout.SummaryDisplayMode.LIST, progress);
        holder.mShortSummaryLayout.setCallback(new ShortSummaryLayout.SummaryLayoutCallback() {
            @Override
            public void onSummaryActionClicked() {

            }

            @Override
            public void onLayoutClicked() {
                Intent intent = new Intent(mActivity, WatchZoneDetailsActivity.class);
                Bundle b = new Bundle();
                b.putLong(WatchZoneDetailsActivity.KEY_WATCHZONE_ID, model.watchZone.getUid());
                intent.putExtras(b);
                mActivity.startActivity(intent);
            }

            @Override
            public void onMoreInfoClicked() {
                Intent intent = new Intent(mActivity, WatchZoneDetailsActivity.class);
                Bundle b = new Bundle();
                b.putLong(WatchZoneDetailsActivity.KEY_WATCHZONE_ID, model.watchZone.getUid());
                intent.putExtras(b);
                mActivity.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mCurrentList == null ? 0 : mCurrentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Context mContext;

        public ShortSummaryLayout mShortSummaryLayout;

        public ViewHolder(Context context, ShortSummaryLayout shortSummaryLayout) {
            super(shortSummaryLayout);
            mContext = context;
            mShortSummaryLayout = shortSummaryLayout;
        }
    }
}
