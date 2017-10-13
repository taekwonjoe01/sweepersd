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
import com.example.joseph.sweepersd.archived.model.watchzone.SweepingAddress;
import com.example.joseph.sweepersd.archived.model.watchzone.SweepingDate;
import com.example.joseph.sweepersd.archived.model.watchzone.WatchZone;
import com.example.joseph.sweepersd.archived.model.watchzone.WatchZoneUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by joseph on 9/7/16.
 */
public class LimitViewAdapter extends RecyclerView.Adapter<LimitViewAdapter.ViewHolder> {
    private static final String TAG = LimitViewAdapter.class.getSimpleName();

    private final Context mContext;
    private final WatchZone mWatchZone;
    private List<LimitPresenter> mLimitPresenters;

    private boolean mIsDetached = true;

    public LimitViewAdapter(Context context, WatchZone watchZone) {
        mContext = context;
        mWatchZone = watchZone;
        mLimitPresenters = new ArrayList<>();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mIsDetached = false;

        new LoadLimitViewTask(mWatchZone.getSweepingAddresses()).execute();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mIsDetached = true;
    }

    @Override
    public LimitViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_limit_list_item, parent, false);

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
        holder.mLimitStreet.setText(mLimitPresenters.get(position).getStreet());
        holder.mLimitDate.setText(mLimitPresenters.get(position).getDate());
        holder.mLimitTime.setText(mLimitPresenters.get(position).getTime());
        holder.mLimitTimer.setText(mLimitPresenters.get(position).getTimeUntilSweeping());

        holder.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Show watchZone details", Toast.LENGTH_SHORT).show();
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
        List<Limit> limits;
        SweepingDate date;

        LimitPresenter(int position, Limit limit, SweepingDate date) {
            this.position = position;
            this.limits = new ArrayList<>();
            this.limits.add(limit);
            this.date = date;
        }

        String getStreet() {
            return WordUtils.capitalize(this.limits.get(0).getStreet());
        }

        String getDate() {
            SimpleDateFormat format = new SimpleDateFormat("EEE, MMM dd");

            String dateString = format.format(date.getStartTime().getTime());
            return dateString;
        }

        String getTime() {
            return LimitParser.convertHourToTimeString(date.getLimitSchedule().getStartHour())
                    + "-" +
                    LimitParser.convertHourToTimeString(date.getLimitSchedule().getEndHour());
        }

        String getTimeUntilSweeping() {
            if (date.getStartTime() != null) {
                GregorianCalendar today = new GregorianCalendar();
                long timeUntilSweeping = date.getStartTime().getTime().getTime() -
                        today.getTime().getTime();

                String timerMessage = "";
                if (timeUntilSweeping > 0) {
                    long hoursUntilParking = timeUntilSweeping / 3600000;
                    long leftOverMinutes = (timeUntilSweeping % 3600000) / 60000;
                    long daysUntilSweeping = hoursUntilParking / 24;
                    long leftOverHours = hoursUntilParking % 24;
                    timerMessage = daysUntilSweeping + "d, "
                            + leftOverHours + "hrs, " + leftOverMinutes + "min";
                } else {
                    timerMessage = "Sweeping now";
                }
                return timerMessage;
            } else {
                return "";
            }
        }

        /**
         * Used during limit loading. Used to determine if a Limit belongs to this presenter.
         * Belonging is defined as having the same Street and start time for the next sweeping
         * date.
         * @return
         */
        boolean insertLimit(Limit l, SweepingDate nextSweepingDate) {
            boolean result = false;
            Limit thisLimit = this.limits.get(0);
            if (thisLimit.getStreet().equals(l.getStreet()) &&
                    this.date.getStartTime().getTime().getTime() ==
                    nextSweepingDate.getStartTime().getTime().getTime()) {
                this.limits.add(l);
                result = true;
            }
            return result;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, View.OnLongClickListener {
        public TextView mLimitStreet;
        public TextView mLimitDate;
        public TextView mLimitTime;
        public TextView mLimitTimer;
        private FrameLayout mViewLayout;
        public View.OnLongClickListener mLongClickListener;
        public View.OnClickListener mOnClickListener;

        public boolean mFinished = false;
        private boolean mFadingIn = false;

        public ViewHolder(View v) {
            super(v);
            mLimitStreet = (TextView) v.findViewById(R.id.textview_limit_range_and_street);
            mLimitDate = (TextView) v.findViewById(R.id.textview_limit_rules);
            mLimitTime = (TextView) v.findViewById(R.id.limit_time);
            mLimitTimer = (TextView) v.findViewById(R.id.limit_timer);
            mViewLayout = (FrameLayout) v.findViewById(R.id.list_item_layout);

            mViewLayout.setOnClickListener(this);
            mViewLayout.setOnLongClickListener(this);

            mLimitTimer.animate().alpha(0.1f).setDuration(750).withEndAction(new Runnable() {
                @Override
                public void run() {
                    if (!mFinished) {
                        if (mFadingIn) {
                            mFadingIn = false;
                            mLimitTimer.animate().alpha(0.1f).withEndAction(this).start();
                        } else {
                            mFadingIn = true;
                            mLimitTimer.animate().alpha(1.0f).withEndAction(this).start();
                        }
                    }
                }
            }).start();
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
                List<SweepingDate> sweepingDates =
                        WatchZoneUtils.getTimeOrderedSweepingDatesForLimit(l);
                if (sweepingDates.size() > 0) {
                    // This next bit essentially filters out duplicate LimitPresenters that will
                    // show the same data. If there are duplicates, they're added to existing
                    // presenter's list.
                    SweepingDate nextSweepingDate = sweepingDates.get(0);

                    boolean contains = false;
                    for (LimitPresenter presenter : results) {
                        contains = presenter.insertLimit(l, nextSweepingDate);
                        if (contains) {
                            break;
                        }
                    }
                    if (!contains) {
                        results.add(new LimitPresenter(results.size(), l, nextSweepingDate));
                    }
                }
            }

            Collections.sort(results, new Comparator<LimitPresenter>() {
                @Override
                public int compare(LimitPresenter presenter1, LimitPresenter presenter2) {
                    long val = presenter1.date.getStartTime().getTime().getTime() -
                            presenter2.date.getStartTime().getTime().getTime();
                    return (int)val;
                }
            });

            return results;
        }

        @Override
        protected void onPostExecute(List<LimitPresenter> results) {
            mLimitPresenters = results;
            notifyDataSetChanged();
            super.onPostExecute(results);
        }
    }
}
