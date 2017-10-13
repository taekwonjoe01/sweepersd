package com.example.joseph.sweepersd;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitParser;
import com.example.joseph.sweepersd.limit.LimitSchedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LimitViewAdapter extends RecyclerView.Adapter<LimitViewAdapter.ViewHolder> {
    private static final String TAG = LimitViewAdapter.class.getSimpleName();

    private List<PostedSign> mPostedSigns;

    public LimitViewAdapter() {
        mPostedSigns = new ArrayList<>();
    }

    public void setPostedLimits(Map<Limit, List<LimitSchedule>> postedLimits) {
        invalidatePostedSigns(postedLimits);
    }

    @Override
    public LimitViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.content_posted_limits, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        PostedSign sign = mPostedSigns.get(position);

        holder.mStreet.setText(sign.street.toUpperCase());
        holder.mStartTime.setText(sign.startTime.toUpperCase());
        holder.mStartAMPM.setText(sign.startAMPM.toUpperCase());
        holder.mEndTime.setText(sign.endTime.toUpperCase());
        holder.mEndAMPM.setText(sign.endAMPM.toUpperCase());
        holder.mPrefix.setText(sign.prefix);
        holder.mDay.setText(sign.day.toUpperCase());
    }

    @Override
    public int getItemCount() {
        return mPostedSigns.size();
    }

    private void invalidatePostedSigns(Map<Limit, List<LimitSchedule>> postedLimits) {
        mPostedSigns.clear();

        Map<LimitTuple, List<LimitSchedule>> sortedSchedules = new HashMap<>();
        for (Limit l : postedLimits.keySet()) {
            List<LimitSchedule> schedules = postedLimits.get(l);

            for (LimitSchedule s : schedules) {
                LimitTuple tuple = new LimitTuple(l.getStreet(), s.getDayNumber(),
                        s.getStartHour(), s.getEndHour());
                List<LimitSchedule> schedulesForDay = sortedSchedules.get(tuple);
                if (schedulesForDay == null) {
                    schedulesForDay = new ArrayList<>();
                }
                schedulesForDay.add(s);

                sortedSchedules.put(tuple, schedulesForDay);
            }
        }
        for (LimitTuple tuple : sortedSchedules.keySet()) {
            PostedSign newSign = new PostedSign();
            newSign.street = tuple.street;
            newSign.day = LimitParser.getDay(tuple.day);
            newSign.startTime = tuple.startHour < 13 ? Integer.toString(tuple.startHour) :
                    Integer.toString(tuple.startHour - 12);
            newSign.startAMPM = tuple.startHour < 13 ? "AM" : "PM";
            newSign.endTime = tuple.endHour < 13 ? Integer.toString(tuple.endHour) :
                    Integer.toString(tuple.endHour - 12);
            newSign.endAMPM = tuple.startHour < 13 ? "AM" : "PM";

            List<LimitSchedule> uniques = sortedSchedules.get(tuple);
            List<Integer> weekNumbers = new ArrayList<>();
            for (LimitSchedule unique : uniques) {
                if (!weekNumbers.contains(unique.getWeekNumber())) {
                    weekNumbers.add(unique.getWeekNumber());
                }
            }
            Collections.sort(weekNumbers);

            if (weekNumbers.size() > 3) {
                newSign.prefix = "EVERY";
            } else {
                newSign.prefix = "";
                for (Integer number : weekNumbers) {
                    if (!newSign.prefix.equals("")) {
                        newSign.prefix += ", ";
                    }
                    newSign.prefix += LimitParser.getPrefix(number);
                }
            }

            mPostedSigns.add(newSign);

            Collections.sort(mPostedSigns);
        }

        notifyDataSetChanged();
    }

    private class LimitTuple {
        final String street;
        final int day;
        final int startHour;
        final int endHour;

        LimitTuple(String street, int day, int start, int end) {
            this.street = street;
            this.day = day;
            this.startHour = start;
            this.endHour = end;
        }

        @Override
        public boolean equals(Object obj) {
            LimitTuple other = (LimitTuple) obj;
            return this.day == other.day && this.startHour == other.startHour && this.endHour == other.endHour
                    && this.street.equals(other.street);
        }

        @Override
        public int hashCode() {
            return street.hashCode() * day * startHour * endHour;
        }
    }

    private class PostedSign implements Comparable<PostedSign> {
        String street;
        String startTime;
        String startAMPM;
        String endTime;
        String endAMPM;
        String prefix;
        String day;

        @Override
        public int compareTo(@NonNull PostedSign o) {
            return this.street.compareTo(o.street);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mStreet;
        public TextView mStartTime;
        public TextView mStartAMPM;
        public TextView mEndTime;
        public TextView mEndAMPM;
        public TextView mPrefix;
        public TextView mDay;

        public ViewHolder(View v) {
            super(v);
            mStreet = v.findViewById(R.id.textview_street);
            mStartTime = v.findViewById(R.id.textview_start_range);
            mStartAMPM = v.findViewById(R.id.textview_start_ampm);
            mEndTime = v.findViewById(R.id.textview_end_range);
            mEndAMPM = v.findViewById(R.id.textview_end_ampm);
            mPrefix = v.findViewById(R.id.textview_week_prefix);
            mDay = v.findViewById(R.id.textview_day);
        }
    }
}
