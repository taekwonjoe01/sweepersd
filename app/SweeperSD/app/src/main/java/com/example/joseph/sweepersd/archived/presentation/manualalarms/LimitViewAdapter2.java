package com.example.joseph.sweepersd.archived.presentation.manualalarms;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.archived.model.limits.Limit;
import com.example.joseph.sweepersd.archived.model.limits.LimitParser;
import com.example.joseph.sweepersd.archived.model.limits.LimitSchedule;
import com.example.joseph.sweepersd.archived.model.watchzone.SweepingAddress;
import com.example.joseph.sweepersd.archived.model.watchzone.WatchZoneUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 9/7/16.
 */
public class LimitViewAdapter2 extends RecyclerView.Adapter<LimitViewAdapter2.ViewHolder> {
    private static final String TAG = LimitViewAdapter2.class.getSimpleName();

    private final Context mContext;
    private final List<SweepingAddress> mSweepingAddresses;
    private List<LimitPresenter> mLimitPresenters;

    private AsyncTask<Void, Void, List<LimitPresenter>> mLoadLimitsTask;

    private boolean mIsDetached = true;

    public LimitViewAdapter2(Context context, List<SweepingAddress> sweepingAddresses) {
        mContext = context;
        mSweepingAddresses = sweepingAddresses;
        mLimitPresenters = new ArrayList<>();
    }

    public void addSweepingAddress(SweepingAddress address) {
        if (address.getLimit() == null) {
            return;
        }
        boolean isDuplicate = false;
        for (LimitPresenter p : mLimitPresenters) {
            if (p.limit.getId() == address.getLimit().getId()) {
                isDuplicate = true;
                break;
            }
        }

        if (!isDuplicate) {
            mLimitPresenters.add(new LimitPresenter(mLimitPresenters.size(), address.getLimit()));
            notifyItemInserted(mLimitPresenters.size()-1);
        }
    }

    public void clearLimits() {
        mLimitPresenters.clear();
        notifyDataSetChanged();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mIsDetached = false;

        mLoadLimitsTask = new LoadLimitViewTask(mSweepingAddresses).execute();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mIsDetached = true;

        mLoadLimitsTask.cancel(false);
    }

    @Override
    public LimitViewAdapter2.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_limit_list_item_2, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.mFinished = true;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mTextViewLimitRange.setText(mLimitPresenters.get(position).getRange());
        holder.mTextViewLimitStreet.setText(mLimitPresenters.get(position).getStreet());
        holder.mTextViewLimitRules.setText(mLimitPresenters.get(position).getRules());

        holder.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Show limit details", Toast.LENGTH_SHORT).show();
            }
        };
        holder.mLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        };
    }

    @Override
    public int getItemCount() {
        return mLimitPresenters.size();
    }

    class LimitPresenter {
        int position;
        Limit limit;

        LimitPresenter(int position, Limit limit) {
            this.position = position;
            this.limit = limit;
        }

        String getRange() {
            return WordUtils.capitalize(this.limit.getRange()[0] + " - " + this.limit.getRange()[1]);
        }

        String getStreet() {
            return WordUtils.capitalize(this.limit.getStreet());
        }

        String getRules() {
            String rule = "";
            int index = 0;
            for (LimitSchedule schedule : this.limit.getSchedules()) {
                if (index != 0) {
                    rule += ", ";
                }
                rule += LimitParser.getPrefix(schedule.getWeekNumber()) + " " +
                        WordUtils.capitalize(LimitParser.getDay(schedule.getDay())) + " (" +
                        LimitParser.convertHourToTimeString(schedule.getStartHour()) + "-" +
                        LimitParser.convertHourToTimeString(schedule.getEndHour()) + ")";
                index++;
            }
            return rule;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, View.OnLongClickListener {
        public TextView mTextViewLimitRange;
        public TextView mTextViewLimitStreet;
        public TextView mTextViewLimitRules;
        private FrameLayout mViewLayout;
        public View.OnLongClickListener mLongClickListener;
        public View.OnClickListener mOnClickListener;

        public boolean mFinished = false;

        public ViewHolder(View v) {
            super(v);
            mTextViewLimitRange = (TextView) v.findViewById(R.id.textview_limit_range);
            mTextViewLimitStreet = (TextView) v.findViewById(R.id.textview_limit_street);
            mTextViewLimitRules = (TextView) v.findViewById(R.id.textview_limit_rules);
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

    private class LoadLimitViewTask extends AsyncTask<Void, Void, List<LimitPresenter>> {
        List<SweepingAddress> mAddresses;
        private String mDescriptionResult;

        public LoadLimitViewTask(List<SweepingAddress> addresses) {
            mAddresses = addresses;
        }

        @Override
        protected List<LimitPresenter> doInBackground(Void... params) {
            List<LimitPresenter> results = new ArrayList<>();

            List<Limit> uniqueLimits = WatchZoneUtils.getUniqueIdLimits(mAddresses);
            for (Limit l : uniqueLimits) {
                if (isCancelled()) {
                    return null;
                }
                results.add(new LimitPresenter(results.size(), l));
            }

            return results;
        }

        @Override
        protected void onPostExecute(List<LimitPresenter> results) {
            super.onPostExecute(results);
            if (results == null) {
                return;
            }
            mLimitPresenters = results;
            notifyDataSetChanged();
        }
    }
}
