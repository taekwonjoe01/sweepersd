package com.example.joseph.sweepersd.presentation.manualalarms;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.model.alarms.Alarm;
import com.example.joseph.sweepersd.model.alarms.SweepingAddress;
import com.example.joseph.sweepersd.model.limits.LimitParser;
import com.example.joseph.sweepersd.model.limits.LimitSchedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by joseph on 9/7/16.
 */
public class LimitViewAdapter extends RecyclerView.Adapter<LimitViewAdapter.ViewHolder> {
    private final Context mContext;
    private final Alarm mAlarm;
    private final List<LimitPresenter> mLimitPresenters;

    public LimitViewAdapter(Context context, Alarm alarm) {
        mContext = context;
        mAlarm = alarm;
        mLimitPresenters = new ArrayList<>();

        HashMap<Integer, SweepingAddress> uniqueLimitSweepingAddresses = new HashMap<>();
        for (SweepingAddress address : mAlarm.getSweepingAddresses()) {
            Log.e("Joey", address.getAddress() + "\n" +
                    address.getLimit().getId() + " " + address.getLimit().getStreet());
            uniqueLimitSweepingAddresses.put(address.getLimit().getId(), address);
            /*LimitPresenter presenter = new LimitPresenter(mLimitPresenters.size(), address);
            mLimitPresenters.add(presenter);*/
        }
        for (Integer limitId : uniqueLimitSweepingAddresses.keySet()) {
            SweepingAddress address = uniqueLimitSweepingAddresses.get(limitId);
            Log.e("Joey", address.getAddress() + "\n" +
                    address.getLimit().getId() + " " + address.getLimit().getStreet());

            for (LimitSchedule schedule : address.getLimit().getSchedules()) {
                Log.e("Joey", schedule.getDay() + "\n" +
                        schedule.getWeekNumber());

                GregorianCalendar startTime = null;
                GregorianCalendar endTime = null;
                Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

                for (int j = 0; j < 31; j++) {
                    int dow = calendar.get(Calendar.DAY_OF_WEEK);

                    if (dow == schedule.getDay()) {
                        int dom = calendar.get(Calendar.DAY_OF_MONTH);
                        int result = ((dom - 1) / 7) + 1;
                        if (result == schedule.getWeekNumber()) {
                            startTime = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                                    schedule.getStartHour(), 0, 0);
                            endTime = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                                    schedule.getEndHour(), 0, 0);
                            break;
                        }
                    }

                    calendar.add(Calendar.DATE, 1);
                }

                LimitPresenter presenter = new LimitPresenter(mLimitPresenters.size(),
                        address.getLimit().getStreet(), schedule, startTime, endTime);
                mLimitPresenters.add(presenter);
            }
        }

        // Sorting
        Collections.sort(mLimitPresenters, new Comparator<LimitPresenter>() {
            @Override
            public int compare(LimitPresenter limit1, LimitPresenter limit2) {
                long val = limit1.startCalendar.getTime().getTime() -
                        limit2.startCalendar.getTime().getTime();
                return (int)val;
            }
        });

        Log.e("Joey", "LIMITPRESENTER size " + mLimitPresenters.size());
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
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mLimitStreet.setText(mLimitPresenters.get(position).getStreet());
        holder.mLimitDate.setText(mLimitPresenters.get(position).getDate());
        holder.mLimitTime.setText(mLimitPresenters.get(position).getTime());
        holder.mLimitTimer.setText(mLimitPresenters.get(position).getTimeUntilSweeping());

        holder.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Show alarm details", Toast.LENGTH_SHORT).show();
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
            return this.street;
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

                long hoursUntilParking = timeUntilSweeping / 3600000;
                long leftOverMinutes = (timeUntilSweeping % 3600000) / 60000;
                long daysUntilSweeping = hoursUntilParking / 24;
                long leftOverHours = hoursUntilParking % 24;
                String timerMessage = daysUntilSweeping + "d, "
                        + leftOverHours + "hrs, " + leftOverMinutes + "min.";
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

        public ViewHolder(View v) {
            super(v);
            mLimitStreet = (TextView) v.findViewById(R.id.limit_street_name);
            mLimitDate = (TextView) v.findViewById(R.id.limit_date);
            mLimitTime = (TextView) v.findViewById(R.id.limit_time);
            mLimitTimer = (TextView) v.findViewById(R.id.limit_timer);
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
