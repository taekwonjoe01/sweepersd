package com.example.joseph.sweepersd.presentation.manualalarms;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.model.watchzone.WatchZone;
import com.example.joseph.sweepersd.model.watchzone.SweepingAddress;
import com.example.joseph.sweepersd.model.limits.LimitParser;
import com.example.joseph.sweepersd.model.limits.LimitSchedule;

import org.apache.commons.lang3.text.WordUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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


        Log.e("Joey", "LIMITPRESENTER size " + mLimitPresenters.size());
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

        Log.e("Joey", "onCreateViewHolder ");
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
        String street;
        LimitSchedule schedule;
        GregorianCalendar startCalendar;
        GregorianCalendar endCalendar;
        boolean isDeleted;

        LimitPresenter(int position, String street, LimitSchedule schedule,
                       GregorianCalendar start, GregorianCalendar end) {
            this.position = position;
            this.street = street;
            this.schedule = schedule;
            this.startCalendar = start;
            this.endCalendar = end;
            isDeleted = false;
        }

        String getStreet() {
            return WordUtils.capitalize(this.street);
        }

        String getDate() {
            SimpleDateFormat format = new SimpleDateFormat("EEE, MMM dd");

            String date = format.format(startCalendar.getTime());
            return date;
        }

        String getTime() {
            return LimitParser.convertHourToTimeString(schedule.getStartHour()) + "-" +
                    LimitParser.convertHourToTimeString(schedule.getEndHour());
        }

        String getTimeUntilSweeping() {
            if (startCalendar != null) {
                GregorianCalendar today = new GregorianCalendar();
                long timeUntilSweeping = startCalendar.getTime().getTime() -
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
            mLimitStreet = (TextView) v.findViewById(R.id.limit_street_name);
            mLimitDate = (TextView) v.findViewById(R.id.limit_date);
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

    public class LoadLimitViewTask extends AsyncTask<Void, Void, List<LimitPresenter>> {
        List<SweepingAddress> mAddresses;
        private String mDescriptionResult;

        public LoadLimitViewTask(List<SweepingAddress> addresses) {
            mAddresses = addresses;
        }

        @Override
        protected List<LimitPresenter> doInBackground(Void... params) {
            List<LimitPresenter> results = new ArrayList<>();

            HashMap<Integer, SweepingAddress> uniqueLimitSweepingAddresses = new HashMap<>();
            for (SweepingAddress address : mAddresses) {
                if (address.getLimit() != null) {
                    uniqueLimitSweepingAddresses.put(address.getLimit().getId(), address);
                }
            }
            for (Integer limitId : uniqueLimitSweepingAddresses.keySet()) {
                SweepingAddress address = uniqueLimitSweepingAddresses.get(limitId);
                Log.d(TAG, address.getAddress() + "\n" +
                        address.getLimit().getId() + " " + address.getLimit().getStreet());

                for (LimitSchedule schedule : address.getLimit().getSchedules()) {
                    GregorianCalendar startTime = null;
                    GregorianCalendar endTime = null;
                    Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

                    for (int j = 0; j < 31; j++) {
                        int dow = calendar.get(Calendar.DAY_OF_WEEK);

                        if (dow == schedule.getDay()) {
                            int dom = calendar.get(Calendar.DAY_OF_MONTH);
                            int result = ((dom - 1) / 7) + 1;
                            if (result == schedule.getWeekNumber()) {
                                GregorianCalendar potentialStartTime = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                                        schedule.getStartHour(), 0, 0);
                                GregorianCalendar potentialEndTime = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                                        schedule.getEndHour(), 0, 0);

                                GregorianCalendar today = new GregorianCalendar(Locale.getDefault());
                                if (today.getTime().getTime() - potentialEndTime.getTime().getTime() < 0) {
                                    Log.e("Joey", "Now: " + today.getTime().toString());
                                    Log.e("Joey", "endTime: " + potentialEndTime.getTime().toString());


                                    Log.e("Joey", "Now: " + today.getTime().getTime());
                                    Log.e("Joey", "endTime: " + potentialEndTime.getTime().getTime());
                                    startTime = potentialStartTime;
                                    endTime = potentialEndTime;
                                    break;
                                }
                            }
                        }

                        calendar.add(Calendar.DATE, 1);
                    }

                    if (startTime != null && endTime != null) {
                        LimitPresenter presenter = new LimitPresenter(results.size(),
                                address.getLimit().getStreet(), schedule, startTime, endTime);
                        results.add(presenter);
                    } else {
                        Log.e(TAG, "start time or end time null! " + schedule.getDay() + "\n" +
                                schedule.getWeekNumber());
                    }
                }
            }

            // Sorting
            Collections.sort(results, new Comparator<LimitPresenter>() {
                @Override
                public int compare(LimitPresenter limit1, LimitPresenter limit2) {
                    long val = limit1.startCalendar.getTime().getTime() -
                            limit2.startCalendar.getTime().getTime();
                    return (int)val;
                }
            });

            HashMap<String, LimitPresenter> finalPresenters = new HashMap<>();
            for (LimitPresenter p : results) {
                if (!finalPresenters.containsKey(p.getStreet())) {
                    finalPresenters.put(p.getStreet(), p);
                }
            }
            results = new ArrayList<>(finalPresenters.values());

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
